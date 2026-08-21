"""
The offline-first sync engine. See docs/API_CONTRACT.md ("Sync") for the
protocol this implements, and docs/DISCOVERY.md section 6 for why it's
shaped this way (client-generated UUIDs, explicit last-write-wins
conflicts, server-assigned document numbers).
"""

from django.utils import timezone
from django.utils.dateparse import parse_datetime

from common.money import compute_line_total

from . import registry

LINE_ITEM_PARENTS = {
    "quote_line_item": "quote",
    "invoice_line_item": "invoice",
}

NUMBERED_MODELS = {"quote", "job", "invoice"}

# A push batch can contain a whole offline session's worth of records in any
# order the client happens to have them in (e.g. a line item next to its
# not-yet-applied parent quote). Rather than push that ordering burden onto
# the Android client, the server applies changes within one batch in this
# fixed dependency order — parents before the children that reference them
# — so "which record did the client list first" never matters. The one
# case this doesn't cover is a lead <-> customer conversion recorded in the
# very same batch (Lead.converted_customer_id needs the Customer to exist,
# but Customer.source_lead_id needs the Lead to exist) — that's expected to
# land across two sync cycles in practice, since a lead is captured well
# before it's converted.
MODEL_APPLY_ORDER = {
    "lead": 0,
    "customer": 1,
    "quote": 2,
    "quote_line_item": 3,
    "job": 4,
    "invoice": 5,
    "invoice_line_item": 6,
    "payment": 7,
    "expense": 8,  # after "job" — an expense may reference one
}


def _iso(dt):
    """
    ISO-8601 using a 'Z' suffix for UTC instead of '+00:00'. `server_time`
    is meant to be echoed straight back as a `?since=` query parameter, and
    an un-encoded '+' in a URL query string is decoded as a space by
    standard form-encoding rules — silently truncating the timezone offset.
    'Z' has no such trap, so every timestamp this API emits uses it.
    """
    if dt is None:
        return None
    value = dt.isoformat()
    return value[:-6] + "Z" if value.endswith("+00:00") else value


def _assign_number(model_key: str, instance):
    if model_key == "quote":
        from sales.services import assign_quote_number_if_needed

        return assign_quote_number_if_needed(instance)
    if model_key == "job":
        from work.services import assign_job_number_if_needed

        return assign_job_number_if_needed(instance)
    if model_key == "invoice":
        from finance.services import assign_invoice_number_if_needed

        return assign_invoice_number_if_needed(instance)
    return instance


def _parse_required_dt(value, field_name, errors):
    if not value:
        errors.setdefault(field_name, []).append("This field is required.")
        return None
    dt = parse_datetime(value)
    if dt is None:
        errors.setdefault(field_name, []).append("Not a valid ISO-8601 datetime.")
        return None
    if timezone.is_naive(dt):
        dt = timezone.make_aware(dt, timezone.utc)
    return dt


def _parse_optional_dt(value, field_name, errors):
    if value in (None, ""):
        return None
    return _parse_required_dt(value, field_name, errors)


def apply_change(business, change: dict) -> dict:
    """
    Apply one record's worth of change to the database. Does NOT recompute
    parent-document totals — that happens once per push, after every change
    in the batch has been applied (see apply_push below), so ordering
    within a batch never matters.
    """
    model_key = change.get("model")
    record_id = change.get("id")

    try:
        model_cls, serializer_cls = registry.get_registered(model_key)
    except ValueError as exc:
        return {"model": model_key, "id": record_id, "status": "error", "errors": {"model": [str(exc)]}}

    errors: dict = {}
    incoming_updated_at = _parse_required_dt(change.get("updated_at"), "updated_at", errors)
    incoming_deleted_at = _parse_optional_dt(change.get("deleted_at"), "deleted_at", errors)
    if errors:
        return {"model": model_key, "id": record_id, "status": "error", "errors": errors}

    if not record_id:
        return {"model": model_key, "id": record_id, "status": "error", "errors": {"id": ["This field is required."]}}

    if model_cls.objects.filter(id=record_id).exclude(business=business).exists():
        return {
            "model": model_key,
            "id": record_id,
            "status": "error",
            "errors": {"id": ["This id is already in use by another business."]},
        }

    existing = model_cls.objects.filter(business=business, id=record_id).first()
    if existing and existing.updated_at >= incoming_updated_at:
        return {
            "model": model_key,
            "id": record_id,
            "status": "conflict",
            "server_record": serializer_cls(existing, context={"business": business}).data,
        }

    fields = dict(change.get("fields") or {})
    fields["id"] = record_id
    serializer = serializer_cls(instance=existing, data=fields, context={"business": business})
    if not serializer.is_valid():
        return {"model": model_key, "id": record_id, "status": "error", "errors": serializer.errors}

    instance = serializer.save(
        business=business, updated_at=incoming_updated_at, deleted_at=incoming_deleted_at
    )

    if model_key in LINE_ITEM_PARENTS:
        instance.line_total = compute_line_total(instance.quantity, instance.unit_price)
        instance.save(update_fields=["line_total"])

    if model_key == "expense":
        # Self-contained, same-record derived field — same pattern as
        # line_total above, not the separate-event pattern
        # _recompute_touched_parents uses for a *different* record's totals.
        from finance.services import recompute_expense_vat

        instance = recompute_expense_vat(instance, bump_updated_at=False)

    if model_key in NUMBERED_MODELS and instance.deleted_at is None:
        instance = _assign_number(model_key, instance)

    return {
        "model": model_key,
        "id": record_id,
        "status": "accepted",
        "server_record": serializer_cls(instance, context={"business": business}).data,
        "_instance": instance,
    }


def _recompute_touched_parents(business, results: list[dict]) -> None:
    from finance.services import recompute_invoice_payment_state, recompute_invoice_totals
    from sales.services import recompute_quote_totals

    touched_quote_ids = set()
    touched_invoice_ids_for_totals = set()
    touched_invoice_ids_for_payment_state = set()

    for result in results:
        if result["status"] != "accepted":
            continue
        model_key = result["model"]
        instance = result["_instance"]
        if model_key == "quote":
            touched_quote_ids.add(instance.id)
        elif model_key == "quote_line_item":
            touched_quote_ids.add(instance.quote_id)
        elif model_key == "invoice":
            touched_invoice_ids_for_totals.add(instance.id)
        elif model_key == "invoice_line_item":
            touched_invoice_ids_for_totals.add(instance.invoice_id)
        elif model_key == "payment" and instance.invoice_id:
            touched_invoice_ids_for_payment_state.add(instance.invoice_id)

    from sales.models import Quote

    for quote in Quote.objects.filter(business=business, id__in=touched_quote_ids):
        recompute_quote_totals(quote)

    from finance.models import Invoice

    for invoice in Invoice.objects.filter(business=business, id__in=touched_invoice_ids_for_totals):
        recompute_invoice_totals(invoice)

    remaining = touched_invoice_ids_for_payment_state - touched_invoice_ids_for_totals
    for invoice in Invoice.objects.filter(business=business, id__in=remaining):
        recompute_invoice_payment_state(invoice)


def apply_push(business, changes: list[dict]) -> list[dict]:
    ordered = sorted(
        enumerate(changes),
        key=lambda pair: (MODEL_APPLY_ORDER.get(pair[1].get("model"), 99), pair[0]),
    )
    results_by_original_index = {}
    for original_index, change in ordered:
        results_by_original_index[original_index] = apply_change(business, change)
    results = [results_by_original_index[i] for i in range(len(changes))]

    _recompute_touched_parents(business, results)

    # Re-serialize accepted records once more: totals recomputed above may
    # have changed the very record (e.g. a quote whose own discount_amount
    # was in this push) after its result was first built.
    for result in results:
        if result["status"] != "accepted":
            continue
        model_key = result["model"]
        _, serializer_cls = registry.get_registered(model_key)
        instance = result.pop("_instance")
        instance.refresh_from_db()
        result["server_record"] = serializer_cls(instance, context={"business": business}).data

    for result in results:
        result.pop("_instance", None)

    return results


def build_pull(business, since: str | None) -> dict:
    server_time = timezone.now()
    since_dt = None
    if since:
        since_dt = parse_datetime(since)
        if since_dt and timezone.is_naive(since_dt):
            since_dt = timezone.make_aware(since_dt, timezone.utc)

    changes = []
    for model_key in registry.all_model_keys():
        model_cls, serializer_cls = registry.get_registered(model_key)
        qs = model_cls.objects.filter(business=business)
        if since_dt:
            qs = qs.filter(updated_at__gt=since_dt)
        for instance in qs:
            changes.append(
                {
                    "model": model_key,
                    "id": str(instance.id),
                    "updated_at": _iso(instance.updated_at),
                    "deleted_at": _iso(instance.deleted_at),
                    "fields": serializer_cls(instance, context={"business": business}).data,
                }
            )

    return {"server_time": _iso(server_time), "changes": changes}

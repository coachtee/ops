from django.utils import timezone

from common.money import compute_document_totals, compute_line_total
from common.models import next_document_number

from .models import Quote


def recompute_quote_totals(quote: Quote, bump_updated_at: bool = True) -> Quote:
    """
    Line items are the source of truth for a quote's money. Whenever a line
    item is created/updated/deleted (directly, or via a sync push), the
    parent quote's subtotal/vat_amount/total are recomputed from scratch —
    never hand-edited — so the numbers can't drift from what's itemised.
    """
    line_totals = quote.line_items.filter(deleted_at__isnull=True).values_list(
        "line_total", flat=True
    )
    subtotal, vat_amount, total = compute_document_totals(
        line_totals, quote.discount_amount, quote.is_vat_applicable
    )
    quote.subtotal, quote.vat_amount, quote.total = subtotal, vat_amount, total
    update_fields = ["subtotal", "vat_amount", "total"]
    if bump_updated_at:
        quote.updated_at = timezone.now()
        update_fields.append("updated_at")
    quote.save(update_fields=update_fields)
    return quote


def assign_quote_number_if_needed(quote: Quote) -> Quote:
    if not quote.number:
        quote.number = next_document_number(quote.business, "quote", "Q")
        quote.save(update_fields=["number"])
    return quote


def recompute_quote_line_item_total(line_item):
    line_item.line_total = compute_line_total(line_item.quantity, line_item.unit_price)
    return line_item

from decimal import Decimal

from django.db.models import Sum
from django.utils import timezone

from common.models import next_document_number
from common.money import compute_document_totals, compute_line_total, extract_vat_from_inclusive, quantize

from .models import Expense, Invoice


def recompute_invoice_line_item_total(line_item):
    line_item.line_total = compute_line_total(line_item.quantity, line_item.unit_price)
    return line_item


def recompute_invoice_totals(invoice: Invoice, bump_updated_at: bool = True) -> Invoice:
    line_totals = invoice.line_items.filter(deleted_at__isnull=True).values_list(
        "line_total", flat=True
    )
    subtotal, vat_amount, total = compute_document_totals(
        line_totals, invoice.discount_amount, invoice.is_vat_applicable
    )
    invoice.subtotal, invoice.vat_amount, invoice.total = subtotal, vat_amount, total
    update_fields = ["subtotal", "vat_amount", "total"]
    if bump_updated_at:
        invoice.updated_at = timezone.now()
        update_fields.append("updated_at")
    invoice.save(update_fields=update_fields)
    recompute_invoice_payment_state(invoice, bump_updated_at=False)
    return invoice


def recompute_invoice_payment_state(invoice: Invoice, bump_updated_at: bool = True) -> Invoice:
    """
    "Who owes me money" depends entirely on this being right. amount_paid is
    always derived from actual payment records, never entered by hand.
    PAID/PARTIALLY_PAID are fully derived from amount_paid vs total in both
    directions — including a payment being deleted/corrected, which must
    pull the invoice back out of "Paid" rather than leave a false positive
    sitting in the owner's outstanding-money picture. Other workflow
    statuses (draft/cancelled) are never touched by this.
    """
    total_paid = invoice.payments.filter(deleted_at__isnull=True).aggregate(
        total=Sum("amount")
    )["total"] or Decimal("0.00")
    invoice.amount_paid = quantize(total_paid)

    update_fields = ["amount_paid"]
    if invoice.status != Invoice.STATUS_CANCELLED:
        if invoice.amount_paid > 0 and invoice.total > 0 and invoice.amount_paid >= invoice.total:
            new_status = Invoice.STATUS_PAID
        elif invoice.amount_paid > 0:
            new_status = Invoice.STATUS_PARTIALLY_PAID
        elif invoice.status in (Invoice.STATUS_PAID, Invoice.STATUS_PARTIALLY_PAID):
            # Payment(s) reversed/deleted — don't leave a false "Paid".
            new_status = Invoice.STATUS_SENT
        else:
            new_status = invoice.status
        if new_status != invoice.status:
            invoice.status = new_status
            update_fields.append("status")

    if bump_updated_at:
        invoice.updated_at = timezone.now()
        update_fields.append("updated_at")
    invoice.save(update_fields=update_fields)
    return invoice


def assign_invoice_number_if_needed(invoice: Invoice) -> Invoice:
    if not invoice.number:
        invoice.number = next_document_number(invoice.business, "invoice", "INV")
        invoice.save(update_fields=["number"])
    return invoice


def recompute_expense_vat(expense: Expense, bump_updated_at: bool = True) -> Expense:
    """`amount` is the VAT-inclusive total; `vat_amount` is always derived
    from it, never entered by hand — see the note on Expense.amount."""
    expense.vat_amount = extract_vat_from_inclusive(expense.amount, expense.is_vat_applicable)
    update_fields = ["vat_amount"]
    if bump_updated_at:
        expense.updated_at = timezone.now()
        update_fields.append("updated_at")
    expense.save(update_fields=update_fields)
    return expense

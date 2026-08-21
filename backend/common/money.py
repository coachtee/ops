"""
Money math shared by quotes and invoices. Everything is Decimal, never
float, and always quantized to cents. VAT is the flat South African rate
(currently 15%) applied to (subtotal - discount) only when the document is
marked VAT-applicable.
"""

from decimal import ROUND_HALF_UP, Decimal
from typing import Iterable

CENTS = Decimal("0.01")
VAT_RATE = Decimal("0.15")


def quantize(value) -> Decimal:
    return Decimal(value).quantize(CENTS, rounding=ROUND_HALF_UP)


def compute_line_total(quantity, unit_price) -> Decimal:
    return quantize(Decimal(quantity) * Decimal(unit_price))


def compute_document_totals(
    line_totals: Iterable[Decimal], discount_amount, is_vat_applicable: bool
) -> tuple[Decimal, Decimal, Decimal]:
    """Returns (subtotal, vat_amount, total)."""
    subtotal = quantize(sum((Decimal(t) for t in line_totals), Decimal("0")))
    discount = quantize(Decimal(discount_amount or 0))
    taxable = subtotal - discount
    if taxable < 0:
        taxable = Decimal("0.00")
    vat_amount = quantize(taxable * VAT_RATE) if is_vat_applicable else Decimal("0.00")
    total = quantize(taxable + vat_amount)
    return subtotal, vat_amount, total

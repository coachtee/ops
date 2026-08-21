from decimal import Decimal

from django.test import SimpleTestCase

from common.money import compute_document_totals, compute_line_total


class MoneyMathTests(SimpleTestCase):
    def test_line_total_multiplies_and_rounds_to_cents(self):
        self.assertEqual(compute_line_total("2", "950.005"), Decimal("1900.01"))

    def test_document_totals_with_vat(self):
        subtotal, vat, total = compute_document_totals(
            [Decimal("100.00"), Decimal("50.00")], discount_amount="0", is_vat_applicable=True
        )
        self.assertEqual(subtotal, Decimal("150.00"))
        self.assertEqual(vat, Decimal("22.50"))  # 15% of 150
        self.assertEqual(total, Decimal("172.50"))

    def test_document_totals_without_vat(self):
        subtotal, vat, total = compute_document_totals(
            [Decimal("100.00")], discount_amount="0", is_vat_applicable=False
        )
        self.assertEqual(vat, Decimal("0.00"))
        self.assertEqual(total, Decimal("100.00"))

    def test_discount_applied_before_vat(self):
        subtotal, vat, total = compute_document_totals(
            [Decimal("1000.00")], discount_amount="100.00", is_vat_applicable=True
        )
        self.assertEqual(subtotal, Decimal("1000.00"))
        # taxable = 1000 - 100 = 900; vat = 135.00
        self.assertEqual(vat, Decimal("135.00"))
        self.assertEqual(total, Decimal("1035.00"))

    def test_discount_larger_than_subtotal_never_goes_negative(self):
        subtotal, vat, total = compute_document_totals(
            [Decimal("50.00")], discount_amount="500.00", is_vat_applicable=True
        )
        self.assertEqual(vat, Decimal("0.00"))
        self.assertEqual(total, Decimal("0.00"))

    def test_empty_line_items_totals_zero(self):
        subtotal, vat, total = compute_document_totals([], discount_amount="0", is_vat_applicable=True)
        self.assertEqual((subtotal, vat, total), (Decimal("0.00"), Decimal("0.00"), Decimal("0.00")))

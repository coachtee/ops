from decimal import Decimal

from django.test import SimpleTestCase

from common.money import compute_document_totals, compute_line_total, extract_vat_from_inclusive


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


class VatInclusiveExtractionTests(SimpleTestCase):
    """
    Expenses run VAT the opposite direction from quotes/invoices: the owner
    already knows the total paid, and we extract the VAT portion already
    inside it, rather than adding VAT on top of a subtotal.
    """

    def test_clean_multiple_of_115_extracts_exactly(self):
        # R115 inclusive = R100 exclusive + R15 VAT, the textbook case.
        self.assertEqual(extract_vat_from_inclusive("115.00", True), Decimal("15.00"))

    def test_another_clean_case(self):
        self.assertEqual(extract_vat_from_inclusive("230.00", True), Decimal("30.00"))

    def test_rounds_half_up_on_an_unclean_division(self):
        # 100 * 15/115 = 13.0434... -> 13.04
        self.assertEqual(extract_vat_from_inclusive("100.00", True), Decimal("13.04"))
        # 50 * 15/115 = 6.5217... -> 6.52
        self.assertEqual(extract_vat_from_inclusive("50.00", True), Decimal("6.52"))

    def test_not_vat_applicable_is_always_zero(self):
        self.assertEqual(extract_vat_from_inclusive("1000.00", False), Decimal("0.00"))

    def test_zero_amount_extracts_zero(self):
        self.assertEqual(extract_vat_from_inclusive("0.00", True), Decimal("0.00"))

from decimal import Decimal

from rest_framework import status

from sales.models import Quote

from .helpers import AuthenticatedAPITestCase


class QuoteWorkflowTests(AuthenticatedAPITestCase):
    def _create_quote(self, **overrides):
        payload = {
            "customer_id": str(self.customer.id),
            "status": "draft",
            "issue_date": "2026-08-01",
            "is_vat_applicable": True,
        }
        payload.update(overrides)
        return self.client.post("/api/quotes/", payload, format="json")

    def test_create_quote_has_no_number_until_line_items_and_totals_are_zero(self):
        response = self._create_quote()
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertIsNotNone(response.data["number"])  # assigned on create, see docs
        self.assertEqual(response.data["total"], "0.00")

    def test_adding_line_items_recomputes_quote_totals_with_vat(self):
        quote_id = self._create_quote().data["id"]

        self.client.post(
            "/api/quote-line-items/",
            {"quote_id": quote_id, "description": "Toilet install", "quantity": "1", "unit_price": "2200.00"},
            format="json",
        )
        response = self.client.post(
            "/api/quote-line-items/",
            {"quote_id": quote_id, "description": "Labour", "quantity": "2", "unit_price": "950.00"},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertEqual(response.data["line_total"], "1900.00")

        quote = self.client.get(f"/api/quotes/{quote_id}/").data
        self.assertEqual(quote["subtotal"], "4100.00")
        self.assertEqual(quote["vat_amount"], "615.00")
        self.assertEqual(quote["total"], "4715.00")

    def test_deleting_a_line_item_recomputes_totals_down(self):
        quote_id = self._create_quote(is_vat_applicable=False).data["id"]
        item = self.client.post(
            "/api/quote-line-items/",
            {"quote_id": quote_id, "description": "Basin", "quantity": "1", "unit_price": "1850.00"},
            format="json",
        ).data
        self.client.post(
            "/api/quote-line-items/",
            {"quote_id": quote_id, "description": "Shower", "quantity": "1", "unit_price": "3400.00"},
            format="json",
        )
        self.assertEqual(self.client.get(f"/api/quotes/{quote_id}/").data["total"], "5250.00")

        self.client.delete(f"/api/quote-line-items/{item['id']}/")
        self.assertEqual(self.client.get(f"/api/quotes/{quote_id}/").data["total"], "3400.00")

    def test_number_is_assigned_once_and_does_not_change_on_update(self):
        quote_id = self._create_quote().data["id"]
        number = self.client.get(f"/api/quotes/{quote_id}/").data["number"]
        self.assertRegex(number, r"^Q-\d{4}$")

        response = self.client.patch(f"/api/quotes/{quote_id}/", {"notes": "updated"}, format="json")
        self.assertEqual(response.data["number"], number)

    def test_sequential_numbers_per_business(self):
        first = self._create_quote().data["number"]
        second = self._create_quote().data["number"]
        first_n = int(first.split("-")[1])
        second_n = int(second.split("-")[1])
        self.assertEqual(second_n, first_n + 1)

    def test_cannot_quote_a_customer_belonging_to_another_business(self):
        _, other_business = self.make_other_business_client()
        from crm.models import Customer

        other_customer = Customer.objects.create(
            business=other_business, name="Someone Else", phone="+27000000000"
        )
        response = self._create_quote(customer_id=str(other_customer.id))
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_quote_totals_never_hand_editable(self):
        response = self._create_quote(total="999999.00", subtotal="999999.00")
        self.assertEqual(response.data["total"], "0.00")

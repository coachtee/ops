from rest_framework import status

from .helpers import AuthenticatedAPITestCase


class InvoicePaymentWorkflowTests(AuthenticatedAPITestCase):
    def _create_invoice_with_total(self, amount, is_vat_applicable=False):
        invoice = self.client.post(
            "/api/invoices/",
            {
                "customer_id": str(self.customer.id),
                "status": "sent",
                "issue_date": "2026-08-01",
                "is_vat_applicable": is_vat_applicable,
            },
            format="json",
        ).data
        self.client.post(
            "/api/invoice-line-items/",
            {"invoice_id": invoice["id"], "description": "Work done", "quantity": "1", "unit_price": amount},
            format="json",
        )
        return self.client.get(f"/api/invoices/{invoice['id']}/").data

    def test_invoice_number_assigned_and_starts_unpaid(self):
        invoice = self._create_invoice_with_total("1000.00")
        self.assertRegex(invoice["number"], r"^INV-\d{4}$")
        self.assertEqual(invoice["amount_paid"], "0.00")
        self.assertEqual(invoice["status"], "sent")

    def test_partial_payment_moves_status_to_partially_paid(self):
        invoice = self._create_invoice_with_total("1000.00")
        self.client.post(
            "/api/payments/",
            {
                "customer_id": str(self.customer.id),
                "invoice_id": invoice["id"],
                "amount": "400.00",
                "method": "eft",
                "paid_date": "2026-08-02",
            },
            format="json",
        )
        updated = self.client.get(f"/api/invoices/{invoice['id']}/").data
        self.assertEqual(updated["amount_paid"], "400.00")
        self.assertEqual(updated["status"], "partially_paid")

    def test_full_payment_marks_invoice_paid(self):
        invoice = self._create_invoice_with_total("1000.00")
        self.client.post(
            "/api/payments/",
            {
                "customer_id": str(self.customer.id),
                "invoice_id": invoice["id"],
                "amount": "1000.00",
                "method": "cash",
                "paid_date": "2026-08-02",
            },
            format="json",
        )
        updated = self.client.get(f"/api/invoices/{invoice['id']}/").data
        self.assertEqual(updated["status"], "paid")

    def test_two_partial_payments_sum_to_paid(self):
        invoice = self._create_invoice_with_total("1000.00")
        for amount in ("400.00", "600.00"):
            self.client.post(
                "/api/payments/",
                {
                    "customer_id": str(self.customer.id),
                    "invoice_id": invoice["id"],
                    "amount": amount,
                    "method": "eft",
                    "paid_date": "2026-08-02",
                },
                format="json",
            )
        updated = self.client.get(f"/api/invoices/{invoice['id']}/").data
        self.assertEqual(updated["amount_paid"], "1000.00")
        self.assertEqual(updated["status"], "paid")

    def test_deleting_a_payment_recomputes_amount_paid_down(self):
        invoice = self._create_invoice_with_total("1000.00")
        payment = self.client.post(
            "/api/payments/",
            {
                "customer_id": str(self.customer.id),
                "invoice_id": invoice["id"],
                "amount": "1000.00",
                "method": "cash",
                "paid_date": "2026-08-02",
            },
            format="json",
        ).data
        self.assertEqual(self.client.get(f"/api/invoices/{invoice['id']}/").data["status"], "paid")

        self.client.delete(f"/api/payments/{payment['id']}/")
        updated = self.client.get(f"/api/invoices/{invoice['id']}/").data
        self.assertEqual(updated["amount_paid"], "0.00")
        # Reversing the payment must pull the invoice back out of "Paid" —
        # a false "Paid" would corrupt "who owes me money".
        self.assertEqual(updated["status"], "sent")

    def test_payment_on_account_without_invoice_is_allowed(self):
        response = self.client.post(
            "/api/payments/",
            {
                "customer_id": str(self.customer.id),
                "amount": "500.00",
                "method": "cash",
                "paid_date": "2026-08-02",
                "notes": "Payment on account, no invoice yet.",
            },
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertIsNone(response.data["invoice_id"])

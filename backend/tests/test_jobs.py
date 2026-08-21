from rest_framework import status

from .helpers import AuthenticatedAPITestCase


class JobWorkflowTests(AuthenticatedAPITestCase):
    def _accepted_quote_id(self):
        quote = self.client.post(
            "/api/quotes/",
            {
                "customer_id": str(self.customer.id),
                "status": "accepted",
                "issue_date": "2026-08-01",
                "is_vat_applicable": False,
            },
            format="json",
        ).data
        self.client.post(
            "/api/quote-line-items/",
            {"quote_id": quote["id"], "description": "Bathroom install", "quantity": "1", "unit_price": "9500.00"},
            format="json",
        )
        return quote["id"]

    def test_job_created_from_accepted_quote_gets_a_number(self):
        quote_id = self._accepted_quote_id()
        response = self.client.post(
            "/api/jobs/",
            {
                "customer_id": str(self.customer.id),
                "quote_id": quote_id,
                "title": "Bathroom installation — Khumalo residence",
                "status": "not_started",
            },
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertRegex(response.data["number"], r"^J-\d{4}$")
        self.assertEqual(str(response.data["quote_id"]), quote_id)

    def test_job_status_can_progress_to_completed(self):
        job_id = self.client.post(
            "/api/jobs/",
            {"customer_id": str(self.customer.id), "title": "Blocked drain", "status": "not_started"},
            format="json",
        ).data["id"]

        response = self.client.patch(
            f"/api/jobs/{job_id}/", {"status": "in_progress"}, format="json"
        )
        self.assertEqual(response.data["status"], "in_progress")

        response = self.client.patch(
            f"/api/jobs/{job_id}/", {"status": "completed", "completed_date": "2026-08-05"}, format="json"
        )
        self.assertEqual(response.data["status"], "completed")

    def test_invoice_can_be_created_from_a_completed_job(self):
        job_id = self.client.post(
            "/api/jobs/",
            {"customer_id": str(self.customer.id), "title": "Blocked drain", "status": "completed"},
            format="json",
        ).data["id"]

        invoice = self.client.post(
            "/api/invoices/",
            {
                "customer_id": str(self.customer.id),
                "job_id": job_id,
                "status": "draft",
                "issue_date": "2026-08-06",
                "is_vat_applicable": False,
            },
            format="json",
        )
        self.assertEqual(invoice.status_code, status.HTTP_201_CREATED, invoice.data)
        self.assertEqual(str(invoice.data["job_id"]), job_id)

    def test_cannot_create_job_for_another_businesss_quote(self):
        _, other_business = self.make_other_business_client()
        from crm.models import Customer as CustomerModel
        from sales.models import Quote

        other_customer = CustomerModel.objects.create(
            business=other_business, name="Other Co", phone="+27000000000"
        )
        other_quote = Quote.objects.create(
            business=other_business,
            customer=other_customer,
            status="draft",
            issue_date="2026-08-01",
        )
        response = self.client.post(
            "/api/jobs/",
            {"customer_id": str(self.customer.id), "quote_id": str(other_quote.id), "title": "X"},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

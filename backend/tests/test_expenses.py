import io
from datetime import timedelta

from django.core.files.uploadedfile import SimpleUploadedFile
from django.utils import timezone
from PIL import Image
from rest_framework import status

from finance.models import Expense
from work.models import Job

from .helpers import AuthenticatedAPITestCase


def _png_bytes():
    buf = io.BytesIO()
    Image.new("RGB", (32, 32), color=(200, 50, 50)).save(buf, format="PNG")
    return buf.getvalue()


class ExpenseWorkflowTests(AuthenticatedAPITestCase):
    def _create_expense(self, **overrides):
        payload = {
            "category": "materials_stock",
            "description": "20x bags of cement",
            "amount": "1150.00",
            "is_vat_applicable": True,
            "date": timezone.localdate().isoformat(),
        }
        payload.update(overrides)
        return self.client.post("/api/expenses/", payload, format="json")

    def test_create_expense_computes_vat_from_inclusive_amount(self):
        response = self._create_expense(amount="115.00")
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertEqual(response.data["vat_amount"], "15.00")

    def test_vat_not_applicable_expense_has_zero_vat(self):
        response = self._create_expense(amount="500.00", is_vat_applicable=False)
        self.assertEqual(response.data["vat_amount"], "0.00")

    def test_vat_amount_is_never_hand_editable(self):
        response = self._create_expense(amount="115.00", vat_amount="999.00")
        self.assertEqual(response.data["vat_amount"], "15.00")

    def test_updating_amount_recomputes_vat(self):
        expense_id = self._create_expense(amount="115.00").data["id"]
        response = self.client.patch(f"/api/expenses/{expense_id}/", {"amount": "230.00"}, format="json")
        self.assertEqual(response.data["vat_amount"], "30.00")

    def test_negative_or_zero_amount_is_rejected(self):
        for bad_amount in ("0.00", "-50.00"):
            response = self._create_expense(amount=bad_amount)
            self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST, bad_amount)
            self.assertIn("amount", response.data)

    def test_future_dated_expense_is_rejected(self):
        future = (timezone.localdate() + timedelta(days=5)).isoformat()
        response = self._create_expense(date=future)
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("date", response.data)

    def test_category_is_required_and_validated(self):
        response = self._create_expense(category="not-a-real-category")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("category", response.data)

    def test_expense_can_link_to_a_job(self):
        job = Job.objects.create(business=self.business, customer=self.customer, title="Kitchen reno")
        response = self._create_expense(job_id=str(job.id))
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertEqual(str(response.data["job_id"]), str(job.id))

    def test_cannot_link_expense_to_another_businesss_job(self):
        _, other_business = self.make_other_business_client()
        from crm.models import Customer as CustomerModel

        other_customer = CustomerModel.objects.create(business=other_business, name="Other", phone="+27000000000")
        other_job = Job.objects.create(business=other_business, customer=other_customer, title="X")
        response = self._create_expense(job_id=str(other_job.id))
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_list_only_returns_own_business_expenses(self):
        other_client, _ = self.make_other_business_client()
        self._create_expense()
        response = self.client.get("/api/expenses/")
        self.assertEqual(response.data["count"], 1)
        other_response = other_client.get("/api/expenses/")
        self.assertEqual(other_response.data["count"], 0)


class ExpenseReceiptUploadTests(AuthenticatedAPITestCase):
    def _expense_id(self):
        return self.client.post(
            "/api/expenses/",
            {
                "category": "fuel_travel",
                "amount": "500.00",
                "is_vat_applicable": True,
                "date": timezone.localdate().isoformat(),
            },
            format="json",
        ).data["id"]

    def test_upload_receipt_attaches_it_to_the_expense(self):
        expense_id = self._expense_id()
        photo = SimpleUploadedFile("receipt.png", _png_bytes(), content_type="image/png")
        response = self.client.post(f"/api/expenses/{expense_id}/receipt/", {"receipt": photo}, format="multipart")
        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)
        self.assertTrue(response.data["receipt_image"])

        fetched = self.client.get(f"/api/expenses/{expense_id}/")
        self.assertTrue(fetched.data["receipt_image"].endswith(".png"))

    def test_upload_receipt_bumps_updated_at_so_it_flows_through_pull(self):
        expense_id = self._expense_id()
        before = Expense.objects.get(id=expense_id).updated_at
        photo = SimpleUploadedFile("receipt.png", _png_bytes(), content_type="image/png")
        self.client.post(f"/api/expenses/{expense_id}/receipt/", {"receipt": photo}, format="multipart")
        after = Expense.objects.get(id=expense_id).updated_at
        self.assertGreater(after, before)

    def test_upload_receipt_for_missing_expense_404s(self):
        photo = SimpleUploadedFile("receipt.png", _png_bytes(), content_type="image/png")
        response = self.client.post(
            "/api/expenses/00000000-0000-0000-0000-000000000000/receipt/", {"receipt": photo}, format="multipart"
        )
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_upload_rejects_a_non_image_file(self):
        expense_id = self._expense_id()
        not_an_image = SimpleUploadedFile("receipt.txt", b"not a real image", content_type="text/plain")
        response = self.client.post(
            f"/api/expenses/{expense_id}/receipt/", {"receipt": not_an_image}, format="multipart"
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_cannot_upload_receipt_to_another_businesss_expense(self):
        expense_id = self._expense_id()
        other_client, _ = self.make_other_business_client()
        photo = SimpleUploadedFile("receipt.png", _png_bytes(), content_type="image/png")
        response = other_client.post(f"/api/expenses/{expense_id}/receipt/", {"receipt": photo}, format="multipart")
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

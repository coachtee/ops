import io

from django.core.files.uploadedfile import SimpleUploadedFile
from PIL import Image
from rest_framework import status

from .helpers import AuthenticatedAPITestCase


def _png_bytes():
    buf = io.BytesIO()
    Image.new("RGB", (32, 32), color=(200, 50, 50)).save(buf, format="PNG")
    return buf.getvalue()


class VisitWorkflowTests(AuthenticatedAPITestCase):
    def _job_id(self):
        return self.client.post(
            "/api/jobs/",
            {"customer_id": str(self.customer.id), "title": "Geyser replacement", "status": "not_started"},
            format="json",
        ).data["id"]

    def _employee_id(self):
        response = self.client.post(
            "/api/employees/",
            {"name": "Bongani Sithole", "role": "Plumber's assistant", "pay_rate_type": "hourly", "pay_rate": "85.00"},
            format="json",
        )
        return response.data["id"]

    def test_visit_can_be_scheduled_against_a_job(self):
        job_id = self._job_id()
        response = self.client.post(
            "/api/visits/",
            {"job_id": job_id, "scheduled_date": "2026-08-25", "start_time": "09:00:00", "status": "scheduled"},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertEqual(str(response.data["job_id"]), job_id)
        self.assertEqual(response.data["status"], "scheduled")
        self.assertIsNone(response.data["photo"])

    def test_visit_can_be_assigned_to_an_employee(self):
        job_id = self._job_id()
        employee_id = self._employee_id()
        response = self.client.post(
            "/api/visits/",
            {"job_id": job_id, "employee_id": employee_id, "scheduled_date": "2026-08-25"},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertEqual(str(response.data["employee_id"]), employee_id)

    def test_visit_workflow_start_to_complete(self):
        job_id = self._job_id()
        visit_id = self.client.post(
            "/api/visits/", {"job_id": job_id, "scheduled_date": "2026-08-25"}, format="json"
        ).data["id"]

        started = self.client.patch(
            f"/api/visits/{visit_id}/",
            {"status": "in_progress", "started_at": "2026-08-25T09:05:00Z"},
            format="json",
        )
        self.assertEqual(started.data["status"], "in_progress")

        completed = self.client.patch(
            f"/api/visits/{visit_id}/",
            {"status": "completed", "completed_at": "2026-08-25T11:30:00Z", "notes": "Geyser replaced, tested, no leaks."},
            format="json",
        )
        self.assertEqual(completed.data["status"], "completed")
        self.assertEqual(completed.data["notes"], "Geyser replaced, tested, no leaks.")

    def test_visit_photo_upload(self):
        job_id = self._job_id()
        visit_id = self.client.post(
            "/api/visits/", {"job_id": job_id, "scheduled_date": "2026-08-25"}, format="json"
        ).data["id"]

        photo = SimpleUploadedFile("visit.png", _png_bytes(), content_type="image/png")
        response = self.client.post(
            f"/api/visits/{visit_id}/photo/",
            {"photo": photo},
            format="multipart",
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)
        self.assertTrue(response.data["photo"])
        self.assertIn(f"visits/{visit_id}", response.data["photo"])

    def test_cannot_create_visit_for_another_businesss_job(self):
        _, other_business = self.make_other_business_client()
        from crm.models import Customer as CustomerModel
        from work.models import Job as JobModel

        other_customer = CustomerModel.objects.create(
            business=other_business, name="Other Co", phone="+27000000000"
        )
        other_job = JobModel.objects.create(business=other_business, customer=other_customer, title="Other job")

        response = self.client.post(
            "/api/visits/",
            {"job_id": str(other_job.id), "scheduled_date": "2026-08-25"},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

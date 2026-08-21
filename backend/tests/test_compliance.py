from rest_framework import status

from compliance.models import ComplianceItem

from .helpers import AuthenticatedAPITestCase


class ComplianceItemWorkflowTests(AuthenticatedAPITestCase):
    def _create_item(self, **overrides):
        payload = {
            "category": "cipc_annual_return",
            "title": "CIPC annual return",
            "due_date": "2026-11-30",
            "is_recurring": True,
            "notes": "Due within the anniversary month of registration.",
        }
        payload.update(overrides)
        return self.client.post("/api/compliance-items/", payload, format="json")

    def test_create_compliance_item(self):
        response = self._create_item()
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertEqual(response.data["title"], "CIPC annual return")
        self.assertIsNone(response.data["completed_date"])

    def test_title_is_required(self):
        response = self._create_item(title="")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("title", response.data)

    def test_blank_title_after_stripping_whitespace_is_rejected(self):
        response = self._create_item(title="   ")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("title", response.data)

    def test_notes_and_is_recurring_are_optional(self):
        response = self._create_item(notes="", is_recurring=False)
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertFalse(response.data["is_recurring"])

    def test_category_defaults_to_other_when_omitted(self):
        payload = {"title": "Renew fire extinguisher certificate", "due_date": "2026-09-01"}
        response = self.client.post("/api/compliance-items/", payload, format="json")
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertEqual(response.data["category"], "other")

    def test_update_compliance_item(self):
        item_id = self._create_item().data["id"]
        response = self.client.patch(f"/api/compliance-items/{item_id}/", {"due_date": "2026-12-15"}, format="json")
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["due_date"], "2026-12-15")

    def test_mark_completed(self):
        item_id = self._create_item().data["id"]
        response = self.client.patch(
            f"/api/compliance-items/{item_id}/", {"completed_date": "2026-11-20"}, format="json"
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["completed_date"], "2026-11-20")

    def test_delete_compliance_item_is_soft_delete(self):
        item_id = self._create_item().data["id"]
        response = self.client.delete(f"/api/compliance-items/{item_id}/")
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(ComplianceItem.objects.filter(id=item_id, deleted_at__isnull=True).exists())
        self.assertTrue(ComplianceItem.objects.filter(id=item_id).exists())

        listed = self.client.get("/api/compliance-items/")
        self.assertEqual(listed.data["count"], 0)

    def test_list_only_returns_own_business_items(self):
        other_client, _ = self.make_other_business_client()
        self._create_item()
        response = self.client.get("/api/compliance-items/")
        self.assertEqual(response.data["count"], 1)
        other_response = other_client.get("/api/compliance-items/")
        self.assertEqual(other_response.data["count"], 0)

    def test_ordered_by_due_date_ascending(self):
        self._create_item(title="Later", due_date="2026-12-01")
        self._create_item(title="Sooner", due_date="2026-09-01")
        response = self.client.get("/api/compliance-items/")
        titles = [i["title"] for i in response.data["results"]]
        self.assertEqual(titles, ["Sooner", "Later"])

    def test_no_claim_of_filing_anywhere_in_the_model(self):
        # Guard against scope creep: this model has no field that could be
        # mistaken for "submitted to SARS/CIPC" — only a due date and an
        # owner-set completed_date, per DISCOVERY.md's compliance-honesty note.
        field_names = {f.name for f in ComplianceItem._meta.get_fields()}
        self.assertNotIn("filed_at", field_names)
        self.assertNotIn("submitted_at", field_names)
        self.assertNotIn("sars_reference", field_names)

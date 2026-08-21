import uuid
from datetime import timedelta

from django.utils import timezone
from rest_framework import status

from crm.models import Lead
from finance.models import Invoice
from sales.models import Quote

from .helpers import AuthenticatedAPITestCase


def change(model, record_id, updated_at, fields, deleted_at=None):
    return {
        "model": model,
        "id": str(record_id),
        "updated_at": updated_at.isoformat(),
        "deleted_at": deleted_at.isoformat() if deleted_at else None,
        "fields": fields,
    }


class SyncPushTests(AuthenticatedAPITestCase):
    def push(self, changes):
        return self.client.post("/api/sync/push/", {"changes": changes}, format="json")

    def test_push_new_lead_is_accepted(self):
        now = timezone.now()
        lead_id = uuid.uuid4()
        response = self.push(
            [change("lead", lead_id, now, {"name": "Nomsa Dlamini", "phone": "+27835551122", "source": "whatsapp"})]
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)
        result = response.data["results"][0]
        self.assertEqual(result["status"], "accepted")
        self.assertEqual(result["server_record"]["name"], "Nomsa Dlamini")
        self.assertEqual(Lead.objects.filter(id=lead_id).count(), 1)

    def test_push_invalid_payload_is_rejected_without_side_effects(self):
        now = timezone.now()
        lead_id = uuid.uuid4()
        response = self.push([change("lead", lead_id, now, {"phone": "+27835551122"})])  # missing name
        result = response.data["results"][0]
        self.assertEqual(result["status"], "error")
        self.assertIn("name", result["errors"])
        self.assertFalse(Lead.objects.filter(id=lead_id).exists())

    def test_replaying_the_same_push_is_idempotent(self):
        now = timezone.now()
        lead_id = uuid.uuid4()
        payload = [change("lead", lead_id, now, {"name": "Nomsa Dlamini", "phone": "+27835551122"})]

        first = self.push(payload)
        self.assertEqual(first.data["results"][0]["status"], "accepted")

        second = self.push(payload)  # dropped connection, client retries the exact same batch
        self.assertEqual(second.data["results"][0]["status"], "conflict")
        self.assertEqual(second.data["results"][0]["server_record"]["name"], "Nomsa Dlamini")
        self.assertEqual(Lead.objects.filter(id=lead_id).count(), 1)

    def test_newer_update_overwrites_older_server_state(self):
        lead_id = uuid.uuid4()
        t1 = timezone.now()
        t2 = t1 + timedelta(minutes=5)

        self.push([change("lead", lead_id, t1, {"name": "Nomsa Dlamini", "phone": "+27835551122"})])
        response = self.push(
            [change("lead", lead_id, t2, {"name": "Nomsa Dlamini", "phone": "+27835551122", "status": "contacted"})]
        )
        self.assertEqual(response.data["results"][0]["status"], "accepted")
        self.assertEqual(Lead.objects.get(id=lead_id).status, "contacted")

    def test_older_update_is_rejected_as_conflict_and_does_not_overwrite(self):
        lead_id = uuid.uuid4()
        t1 = timezone.now()
        t2 = t1 + timedelta(minutes=5)

        # Newest state lands first (e.g. a second device synced sooner).
        self.push(
            [change("lead", lead_id, t2, {"name": "Nomsa Dlamini", "phone": "+27835551122", "status": "contacted"})]
        )
        # An older offline edit arrives afterwards and must not clobber it.
        response = self.push(
            [change("lead", lead_id, t1, {"name": "Nomsa Dlamini", "phone": "+27835551122", "status": "new"})]
        )
        self.assertEqual(response.data["results"][0]["status"], "conflict")
        self.assertEqual(response.data["results"][0]["server_record"]["status"], "contacted")
        self.assertEqual(Lead.objects.get(id=lead_id).status, "contacted")

    def test_soft_delete_via_sync(self):
        lead_id = uuid.uuid4()
        t1 = timezone.now()
        t2 = t1 + timedelta(minutes=1)
        self.push([change("lead", lead_id, t1, {"name": "Nomsa Dlamini", "phone": "+27835551122"})])
        response = self.push(
            [change("lead", lead_id, t2, {"name": "Nomsa Dlamini", "phone": "+27835551122"}, deleted_at=t2)]
        )
        self.assertEqual(response.data["results"][0]["status"], "accepted")
        self.assertIsNotNone(Lead.objects.get(id=lead_id).deleted_at)

    def test_cannot_reuse_an_id_that_belongs_to_another_business(self):
        other_client, other_business = self.make_other_business_client()
        shared_id = uuid.uuid4()
        now = timezone.now()

        mine = self.push([change("lead", shared_id, now, {"name": "Mine", "phone": "+27000000001"})])
        self.assertEqual(mine.data["results"][0]["status"], "accepted")

        theirs = other_client.post(
            "/api/sync/push/",
            {"changes": [change("lead", shared_id, now, {"name": "Theirs", "phone": "+27000000002"})]},
            format="json",
        )
        self.assertEqual(theirs.data["results"][0]["status"], "error")
        self.assertEqual(Lead.objects.get(id=shared_id).name, "Mine")

    def test_quote_and_line_items_in_one_batch_compute_totals_regardless_of_order(self):
        now = timezone.now()
        quote_id = uuid.uuid4()
        item_id = uuid.uuid4()

        quote_change = change(
            "quote",
            quote_id,
            now,
            {
                "customer_id": str(self.customer.id),
                "status": "draft",
                "issue_date": "2026-08-01",
                "is_vat_applicable": False,
            },
        )
        item_change = change(
            "quote_line_item",
            item_id,
            now,
            {"quote_id": str(quote_id), "description": "Toilet install", "quantity": "1", "unit_price": "2200.00"},
        )

        # Line item arrives before its parent quote in the batch.
        response = self.push([item_change, quote_change])
        self.assertEqual([r["status"] for r in response.data["results"]], ["accepted", "accepted"])
        quote = Quote.objects.get(id=quote_id)
        self.assertEqual(str(quote.total), "2200.00")
        self.assertIsNotNone(quote.number)

    def test_full_offline_creation_synced_in_one_batch_updates_invoice_payment_state(self):
        """
        Simulates a device that created a customer, invoice, line item and
        payment entirely offline, then syncs everything in one push once
        connectivity returns.
        """
        now = timezone.now()
        invoice_id = uuid.uuid4()
        item_id = uuid.uuid4()
        payment_id = uuid.uuid4()

        response = self.push(
            [
                change(
                    "invoice",
                    invoice_id,
                    now,
                    {
                        "customer_id": str(self.customer.id),
                        "status": "sent",
                        "issue_date": "2026-08-01",
                        "is_vat_applicable": False,
                    },
                ),
                change(
                    "invoice_line_item",
                    item_id,
                    now,
                    {"invoice_id": str(invoice_id), "description": "Blocked drain", "quantity": "1", "unit_price": "980.00"},
                ),
                change(
                    "payment",
                    payment_id,
                    now,
                    {
                        "customer_id": str(self.customer.id),
                        "invoice_id": str(invoice_id),
                        "amount": "980.00",
                        "method": "cash",
                        "paid_date": "2026-08-01",
                    },
                ),
            ]
        )
        self.assertTrue(all(r["status"] == "accepted" for r in response.data["results"]), response.data)
        invoice = Invoice.objects.get(id=invoice_id)
        self.assertEqual(str(invoice.total), "980.00")
        self.assertEqual(str(invoice.amount_paid), "980.00")
        self.assertEqual(invoice.status, "paid")
        self.assertIsNotNone(invoice.number)


class SyncPullTests(AuthenticatedAPITestCase):
    def test_pull_with_no_since_returns_full_snapshot(self):
        response = self.client.get("/api/sync/pull/")
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        models_present = {c["model"] for c in response.data["changes"]}
        self.assertIn("customer", models_present)  # the setUp customer

    def test_pull_since_cursor_excludes_earlier_records(self):
        cursor = self.client.get("/api/sync/pull/").data["server_time"]

        lead_id = uuid.uuid4()
        self.client.post(
            "/api/sync/push/",
            {
                "changes": [
                    change("lead", lead_id, timezone.now(), {"name": "New Lead", "phone": "+27835551122"})
                ]
            },
            format="json",
        )

        response = self.client.get(f"/api/sync/pull/?since={cursor}")
        ids_returned = {c["id"] for c in response.data["changes"]}
        self.assertIn(str(lead_id), ids_returned)
        self.assertNotIn(str(self.customer.id), ids_returned)  # created before the cursor

    def test_pull_does_not_leak_another_business_data(self):
        other_client, other_business = self.make_other_business_client()
        from crm.models import Customer

        Customer.objects.create(business=other_business, name="Not Mine", phone="+27000000000")

        response = self.client.get("/api/sync/pull/")
        names = {c["fields"].get("name") for c in response.data["changes"] if c["model"] == "customer"}
        self.assertNotIn("Not Mine", names)

import uuid
from datetime import timedelta
from decimal import Decimal

from django.utils import timezone
from rest_framework import status

from crm.models import Lead
from finance.models import Expense, Invoice, Supplier
from people.models import Employee, Payslip
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


class ExpenseSyncTests(AuthenticatedAPITestCase):
    def push(self, changes):
        return self.client.post("/api/sync/push/", {"changes": changes}, format="json")

    def test_push_new_expense_computes_vat_and_is_accepted(self):
        now = timezone.now()
        expense_id = uuid.uuid4()
        response = self.push(
            [
                change(
                    "expense",
                    expense_id,
                    now,
                    {
                        "category": "fuel_travel",
                        "description": "Diesel — bakkie",
                        "amount": "575.00",
                        "is_vat_applicable": True,
                        "date": "2026-08-01",
                    },
                )
            ]
        )
        result = response.data["results"][0]
        self.assertEqual(result["status"], "accepted", response.data)
        self.assertEqual(result["server_record"]["vat_amount"], "75.00")
        self.assertEqual(Expense.objects.get(id=expense_id).description, "Diesel — bakkie")

    def test_push_rejects_invalid_expense_without_side_effects(self):
        now = timezone.now()
        expense_id = uuid.uuid4()
        response = self.push(
            [change("expense", expense_id, now, {"category": "fuel_travel", "amount": "-10.00", "date": "2026-08-01"})]
        )
        self.assertEqual(response.data["results"][0]["status"], "error")
        self.assertFalse(Expense.objects.filter(id=expense_id).exists())

    def test_expense_and_its_job_in_one_batch_regardless_of_order(self):
        now = timezone.now()
        job_id = uuid.uuid4()
        expense_id = uuid.uuid4()

        job_change = change(
            "job", job_id, now, {"customer_id": str(self.customer.id), "title": "Kitchen reno", "status": "in_progress"}
        )
        expense_change = change(
            "expense",
            expense_id,
            now,
            {"category": "materials_stock", "amount": "230.00", "is_vat_applicable": True, "date": "2026-08-01", "job_id": str(job_id)},
        )

        # Expense listed before its not-yet-applied parent job.
        response = self.push([expense_change, job_change])
        self.assertEqual([r["status"] for r in response.data["results"]], ["accepted", "accepted"])
        expense = Expense.objects.get(id=expense_id)
        self.assertEqual(expense.job_id, job_id)
        self.assertEqual(str(expense.vat_amount), "30.00")

    def test_receipt_image_is_not_writable_through_sync(self):
        now = timezone.now()
        expense_id = uuid.uuid4()
        response = self.push(
            [
                change(
                    "expense",
                    expense_id,
                    now,
                    {
                        "category": "other",
                        "amount": "100.00",
                        "is_vat_applicable": False,
                        "date": "2026-08-01",
                        "receipt_image": "not-a-real-upload",
                    },
                )
            ]
        )
        self.assertEqual(response.data["results"][0]["status"], "accepted")
        self.assertFalse(Expense.objects.get(id=expense_id).receipt_image)

    def test_pulled_expense_includes_receipt_url_after_upload(self):
        import io

        from django.core.files.uploadedfile import SimpleUploadedFile
        from PIL import Image

        expense_id = self.client.post(
            "/api/expenses/",
            {"category": "other", "amount": "100.00", "is_vat_applicable": False, "date": "2026-08-01"},
            format="json",
        ).data["id"]
        buf = io.BytesIO()
        Image.new("RGB", (16, 16)).save(buf, format="PNG")
        photo = SimpleUploadedFile("r.png", buf.getvalue(), content_type="image/png")
        self.client.post(f"/api/expenses/{expense_id}/receipt/", {"receipt": photo}, format="multipart")

        response = self.client.get("/api/sync/pull/")
        expense_change = next(c for c in response.data["changes"] if c["model"] == "expense" and c["id"] == str(expense_id))
        self.assertTrue(expense_change["fields"]["receipt_image"])


class SupplierSyncTests(AuthenticatedAPITestCase):
    def push(self, changes):
        return self.client.post("/api/sync/push/", {"changes": changes}, format="json")

    def test_push_new_supplier_is_accepted(self):
        now = timezone.now()
        supplier_id = uuid.uuid4()
        response = self.push([change("supplier", supplier_id, now, {"name": "Cashbuild", "phone": "+27215551000"})])
        result = response.data["results"][0]
        self.assertEqual(result["status"], "accepted", response.data)
        self.assertEqual(Supplier.objects.get(id=supplier_id).name, "Cashbuild")

    def test_push_rejects_blank_name_without_side_effects(self):
        now = timezone.now()
        supplier_id = uuid.uuid4()
        response = self.push([change("supplier", supplier_id, now, {"name": "  "})])
        self.assertEqual(response.data["results"][0]["status"], "error")
        self.assertFalse(Supplier.objects.filter(id=supplier_id).exists())

    def test_replaying_the_same_supplier_push_is_idempotent(self):
        now = timezone.now()
        supplier_id = uuid.uuid4()
        payload = [change("supplier", supplier_id, now, {"name": "Cashbuild"})]
        self.push(payload)
        second = self.push(payload)
        self.assertEqual(second.data["results"][0]["status"], "conflict")
        self.assertEqual(Supplier.objects.filter(id=supplier_id).count(), 1)

    def test_older_supplier_update_does_not_overwrite_newer(self):
        supplier_id = uuid.uuid4()
        t1 = timezone.now()
        t2 = t1 + timedelta(minutes=5)
        self.push([change("supplier", supplier_id, t2, {"name": "Cashbuild HQ"})])
        response = self.push([change("supplier", supplier_id, t1, {"name": "Cashbuild (old name)"})])
        self.assertEqual(response.data["results"][0]["status"], "conflict")
        self.assertEqual(Supplier.objects.get(id=supplier_id).name, "Cashbuild HQ")

    def test_expense_and_its_supplier_in_one_batch_regardless_of_order(self):
        now = timezone.now()
        supplier_id = uuid.uuid4()
        expense_id = uuid.uuid4()

        supplier_change = change("supplier", supplier_id, now, {"name": "Cashbuild"})
        expense_change = change(
            "expense",
            expense_id,
            now,
            {
                "category": "materials_stock",
                "amount": "230.00",
                "is_vat_applicable": True,
                "date": "2026-08-01",
                "supplier_id": str(supplier_id),
            },
        )

        # Expense listed before its not-yet-applied supplier.
        response = self.push([expense_change, supplier_change])
        self.assertEqual([r["status"] for r in response.data["results"]], ["accepted", "accepted"])
        expense = Expense.objects.get(id=expense_id)
        self.assertEqual(expense.supplier_id, supplier_id)

    def test_pull_includes_supplier_and_scopes_to_business(self):
        other_client, other_business = self.make_other_business_client()
        Supplier.objects.create(business=other_business, name="Not Mine")

        self.push([change("supplier", uuid.uuid4(), timezone.now(), {"name": "Mine Hardware"})])
        response = self.client.get("/api/sync/pull/")
        names = {c["fields"].get("name") for c in response.data["changes"] if c["model"] == "supplier"}
        self.assertIn("Mine Hardware", names)
        self.assertNotIn("Not Mine", names)


class EmployeeSyncTests(AuthenticatedAPITestCase):
    def push(self, changes):
        return self.client.post("/api/sync/push/", {"changes": changes}, format="json")

    def test_push_new_employee_is_accepted(self):
        now = timezone.now()
        employee_id = uuid.uuid4()
        response = self.push([change("employee", employee_id, now, {"name": "Nomsa Dlamini", "pay_rate": "85.00"})])
        result = response.data["results"][0]
        self.assertEqual(result["status"], "accepted", response.data)
        self.assertEqual(Employee.objects.get(id=employee_id).name, "Nomsa Dlamini")

    def test_push_rejects_blank_name_without_side_effects(self):
        now = timezone.now()
        employee_id = uuid.uuid4()
        response = self.push([change("employee", employee_id, now, {"name": "  "})])
        self.assertEqual(response.data["results"][0]["status"], "error")
        self.assertFalse(Employee.objects.filter(id=employee_id).exists())

    def test_replaying_the_same_employee_push_is_idempotent(self):
        now = timezone.now()
        employee_id = uuid.uuid4()
        payload = [change("employee", employee_id, now, {"name": "Nomsa Dlamini"})]
        self.push(payload)
        second = self.push(payload)
        self.assertEqual(second.data["results"][0]["status"], "conflict")
        self.assertEqual(Employee.objects.filter(id=employee_id).count(), 1)

    def test_older_employee_update_does_not_overwrite_newer(self):
        employee_id = uuid.uuid4()
        t1 = timezone.now()
        t2 = t1 + timedelta(minutes=5)
        self.push([change("employee", employee_id, t2, {"name": "Nomsa Dlamini (Senior)"})])
        response = self.push([change("employee", employee_id, t1, {"name": "Nomsa Dlamini (old)"})])
        self.assertEqual(response.data["results"][0]["status"], "conflict")
        self.assertEqual(Employee.objects.get(id=employee_id).name, "Nomsa Dlamini (Senior)")

    def test_pull_includes_employee_and_scopes_to_business(self):
        other_client, other_business = self.make_other_business_client()
        Employee.objects.create(business=other_business, name="Not Mine")

        self.push([change("employee", uuid.uuid4(), timezone.now(), {"name": "Mine Employee"})])
        response = self.client.get("/api/sync/pull/")
        names = {c["fields"].get("name") for c in response.data["changes"] if c["model"] == "employee"}
        self.assertIn("Mine Employee", names)
        self.assertNotIn("Not Mine", names)


class PayslipSyncTests(AuthenticatedAPITestCase):
    def setUp(self):
        super().setUp()
        self.employee = Employee.objects.create(business=self.business, name="Nomsa Dlamini")

    def push(self, changes):
        return self.client.post("/api/sync/push/", {"changes": changes}, format="json")

    def _payslip_fields(self, **overrides):
        fields = {
            "employee_id": str(self.employee.id),
            "period_start": "2026-08-01",
            "period_end": "2026-08-07",
            "gross_pay": "3400.00",
            "deductions": "150.00",
        }
        fields.update(overrides)
        return fields

    def test_push_new_payslip_is_accepted_and_net_pay_computed(self):
        now = timezone.now()
        payslip_id = uuid.uuid4()
        response = self.push([change("payslip", payslip_id, now, self._payslip_fields())])
        result = response.data["results"][0]
        self.assertEqual(result["status"], "accepted", response.data)
        self.assertEqual(result["server_record"]["net_pay"], "3250.00")
        self.assertEqual(Payslip.objects.get(id=payslip_id).net_pay, Decimal("3250.00"))

    def test_push_rejects_deductions_exceeding_gross_pay(self):
        now = timezone.now()
        payslip_id = uuid.uuid4()
        response = self.push(
            [change("payslip", payslip_id, now, self._payslip_fields(gross_pay="1000.00", deductions="1500.00"))]
        )
        self.assertEqual(response.data["results"][0]["status"], "error")
        self.assertFalse(Payslip.objects.filter(id=payslip_id).exists())

    def test_replaying_the_same_payslip_push_is_idempotent(self):
        now = timezone.now()
        payslip_id = uuid.uuid4()
        payload = [change("payslip", payslip_id, now, self._payslip_fields())]
        self.push(payload)
        second = self.push(payload)
        self.assertEqual(second.data["results"][0]["status"], "conflict")
        self.assertEqual(Payslip.objects.filter(id=payslip_id).count(), 1)

    def test_payslip_and_its_employee_in_one_batch_regardless_of_order(self):
        now = timezone.now()
        new_employee_id = uuid.uuid4()
        payslip_id = uuid.uuid4()

        employee_change = change("employee", new_employee_id, now, {"name": "Zola Mthembu"})
        payslip_change = change(
            "payslip", payslip_id, now, self._payslip_fields(employee_id=str(new_employee_id))
        )

        # Payslip listed before its not-yet-applied employee.
        response = self.push([payslip_change, employee_change])
        self.assertEqual([r["status"] for r in response.data["results"]], ["accepted", "accepted"])
        payslip = Payslip.objects.get(id=payslip_id)
        self.assertEqual(payslip.employee_id, new_employee_id)

    def test_pull_includes_payslip_and_scopes_to_business(self):
        other_client, other_business = self.make_other_business_client()
        other_employee = Employee.objects.create(business=other_business, name="Other Employee")
        Payslip.objects.create(
            business=other_business,
            employee=other_employee,
            period_start="2026-08-01",
            period_end="2026-08-07",
            gross_pay=Decimal("1000.00"),
        )

        self.push([change("payslip", uuid.uuid4(), timezone.now(), self._payslip_fields())])
        response = self.client.get("/api/sync/pull/")
        payslip_ids = {c["id"] for c in response.data["changes"] if c["model"] == "payslip"}
        mine = Payslip.objects.filter(business=self.business).values_list("id", flat=True)
        self.assertTrue(all(str(pid) in payslip_ids for pid in mine))
        other_ids = Payslip.objects.filter(business=other_business).values_list("id", flat=True)
        self.assertFalse(any(str(pid) in payslip_ids for pid in other_ids))

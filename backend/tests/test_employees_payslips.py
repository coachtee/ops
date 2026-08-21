from rest_framework import status

from people.models import Employee, Payslip

from .helpers import AuthenticatedAPITestCase


class EmployeeWorkflowTests(AuthenticatedAPITestCase):
    def _create_employee(self, **overrides):
        payload = {
            "name": "Nomsa Dlamini",
            "role": "Plumber's assistant",
            "phone": "+27835551122",
            "email": "nomsa@example.co.za",
            "pay_rate_type": "hourly",
            "pay_rate": "85.00",
            "start_date": "2025-01-15",
            "notes": "Started as an apprentice.",
        }
        payload.update(overrides)
        return self.client.post("/api/employees/", payload, format="json")

    def test_create_employee(self):
        response = self._create_employee()
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertEqual(response.data["name"], "Nomsa Dlamini")
        self.assertEqual(response.data["pay_rate_type"], "hourly")
        self.assertEqual(response.data["pay_rate"], "85.00")

    def test_name_is_required(self):
        response = self._create_employee(name="")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("name", response.data)

    def test_blank_name_after_stripping_whitespace_is_rejected(self):
        response = self._create_employee(name="   ")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("name", response.data)

    def test_role_phone_email_and_start_date_are_optional(self):
        response = self._create_employee(role="", phone="", email="", start_date=None)
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)

    def test_negative_pay_rate_is_rejected(self):
        response = self._create_employee(pay_rate="-10.00")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("pay_rate", response.data)

    def test_update_employee(self):
        employee_id = self._create_employee().data["id"]
        response = self.client.patch(f"/api/employees/{employee_id}/", {"pay_rate": "95.00"}, format="json")
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["pay_rate"], "95.00")

    def test_delete_employee_is_soft_delete(self):
        employee_id = self._create_employee().data["id"]
        response = self.client.delete(f"/api/employees/{employee_id}/")
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(Employee.objects.filter(id=employee_id, deleted_at__isnull=True).exists())
        self.assertTrue(Employee.objects.filter(id=employee_id).exists())

        listed = self.client.get("/api/employees/")
        self.assertEqual(listed.data["count"], 0)

    def test_list_only_returns_own_business_employees(self):
        other_client, _ = self.make_other_business_client()
        self._create_employee()
        response = self.client.get("/api/employees/")
        self.assertEqual(response.data["count"], 1)
        other_response = other_client.get("/api/employees/")
        self.assertEqual(other_response.data["count"], 0)

    def test_ordered_alphabetically_by_name(self):
        self._create_employee(name="Zola Mthembu")
        self._create_employee(name="Andile Khumalo")
        response = self.client.get("/api/employees/")
        names = [e["name"] for e in response.data["results"]]
        self.assertEqual(names, ["Andile Khumalo", "Zola Mthembu"])


class PayslipWorkflowTests(AuthenticatedAPITestCase):
    def setUp(self):
        super().setUp()
        self.employee = Employee.objects.create(
            business=self.business, name="Nomsa Dlamini", pay_rate_type="hourly", pay_rate="85.00"
        )

    def _create_payslip(self, **overrides):
        payload = {
            "employee_id": str(self.employee.id),
            "period_start": "2026-08-01",
            "period_end": "2026-08-07",
            "gross_pay": "3400.00",
            "deductions": "150.00",
            "deductions_note": "UIF",
        }
        payload.update(overrides)
        return self.client.post("/api/payslips/", payload, format="json")

    def test_create_payslip_computes_net_pay(self):
        response = self._create_payslip()
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertEqual(response.data["net_pay"], "3250.00")

    def test_net_pay_cannot_be_set_by_the_client(self):
        response = self._create_payslip(net_pay="999999.00")
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertEqual(response.data["net_pay"], "3250.00")

    def test_gross_pay_must_be_greater_than_zero(self):
        response = self._create_payslip(gross_pay="0.00")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("gross_pay", response.data)

    def test_deductions_cannot_be_negative(self):
        response = self._create_payslip(deductions="-1.00")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("deductions", response.data)

    def test_deductions_cannot_exceed_gross_pay(self):
        response = self._create_payslip(gross_pay="1000.00", deductions="1500.00")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("deductions", response.data)

    def test_period_end_cannot_be_before_period_start(self):
        response = self._create_payslip(period_start="2026-08-07", period_end="2026-08-01")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("period_end", response.data)

    def test_deductions_defaults_to_zero_when_omitted(self):
        payload = {
            "employee_id": str(self.employee.id),
            "period_start": "2026-08-01",
            "period_end": "2026-08-07",
            "gross_pay": "3400.00",
        }
        response = self.client.post("/api/payslips/", payload, format="json")
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertEqual(response.data["net_pay"], "3400.00")

    def test_update_payslip_recomputes_net_pay(self):
        payslip_id = self._create_payslip().data["id"]
        response = self.client.patch(f"/api/payslips/{payslip_id}/", {"deductions": "400.00"}, format="json")
        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)
        self.assertEqual(response.data["net_pay"], "3000.00")

    def test_marking_a_payslip_paid(self):
        payslip_id = self._create_payslip().data["id"]
        response = self.client.patch(f"/api/payslips/{payslip_id}/", {"paid_date": "2026-08-08"}, format="json")
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["paid_date"], "2026-08-08")

    def test_delete_payslip_is_soft_delete(self):
        payslip_id = self._create_payslip().data["id"]
        response = self.client.delete(f"/api/payslips/{payslip_id}/")
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(Payslip.objects.filter(id=payslip_id, deleted_at__isnull=True).exists())
        self.assertTrue(Payslip.objects.filter(id=payslip_id).exists())

        listed = self.client.get("/api/payslips/")
        self.assertEqual(listed.data["count"], 0)

    def test_cannot_link_payslip_to_another_businesss_employee(self):
        _, other_business = self.make_other_business_client()
        other_employee = Employee.objects.create(business=other_business, name="Other Co Employee")
        response = self._create_payslip(employee_id=str(other_employee.id))
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_deleting_an_employee_does_not_delete_their_payslips(self):
        # Soft delete only (see BusinessScopedViewSet.perform_destroy) — the
        # FK's on_delete=CASCADE only fires on a real DB row delete, which
        # the API never performs. Payslip history survives an employee
        # being removed from the active list, same as Supplier/Expense.
        payslip_id = self._create_payslip().data["id"]
        self.client.delete(f"/api/employees/{self.employee.id}/")

        payslip = self.client.get(f"/api/payslips/{payslip_id}/")
        self.assertEqual(payslip.status_code, status.HTTP_200_OK)
        self.assertEqual(str(payslip.data["employee_id"]), str(self.employee.id))

        employees = self.client.get("/api/employees/")
        self.assertEqual(employees.data["count"], 0)

    def test_list_only_returns_own_business_payslips(self):
        other_client, _ = self.make_other_business_client()
        self._create_payslip()
        response = self.client.get("/api/payslips/")
        self.assertEqual(response.data["count"], 1)
        other_response = other_client.get("/api/payslips/")
        self.assertEqual(other_response.data["count"], 0)

    def test_ordered_by_period_end_descending(self):
        self._create_payslip(period_start="2026-07-01", period_end="2026-07-07")
        self._create_payslip(period_start="2026-08-01", period_end="2026-08-07")
        response = self.client.get("/api/payslips/")
        periods = [p["period_end"] for p in response.data["results"]]
        self.assertEqual(periods, ["2026-08-07", "2026-07-07"])

from rest_framework import status

from finance.models import Supplier

from .helpers import AuthenticatedAPITestCase


class SupplierWorkflowTests(AuthenticatedAPITestCase):
    def _create_supplier(self, **overrides):
        payload = {
            "name": "Builders Warehouse",
            "contact_person": "Sipho Dlamini",
            "phone": "+27215551234",
            "email": "sipho@builderswarehouse.co.za",
            "notes": "Main materials supplier for Kuils River jobs.",
        }
        payload.update(overrides)
        return self.client.post("/api/suppliers/", payload, format="json")

    def test_create_supplier(self):
        response = self._create_supplier()
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertEqual(response.data["name"], "Builders Warehouse")
        self.assertEqual(response.data["contact_person"], "Sipho Dlamini")

    def test_name_is_required(self):
        response = self._create_supplier(name="")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("name", response.data)

    def test_blank_name_after_stripping_whitespace_is_rejected(self):
        response = self._create_supplier(name="   ")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("name", response.data)

    def test_phone_and_email_are_optional(self):
        response = self._create_supplier(phone="", email="")
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)

    def test_update_supplier(self):
        supplier_id = self._create_supplier().data["id"]
        response = self.client.patch(f"/api/suppliers/{supplier_id}/", {"phone": "+27215559999"}, format="json")
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["phone"], "+27215559999")

    def test_delete_supplier_is_soft_delete(self):
        supplier_id = self._create_supplier().data["id"]
        response = self.client.delete(f"/api/suppliers/{supplier_id}/")
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(Supplier.objects.filter(id=supplier_id, deleted_at__isnull=True).exists())
        self.assertTrue(Supplier.objects.filter(id=supplier_id).exists())  # still there, just soft-deleted

        listed = self.client.get("/api/suppliers/")
        self.assertEqual(listed.data["count"], 0)

    def test_list_only_returns_own_business_suppliers(self):
        other_client, _ = self.make_other_business_client()
        self._create_supplier()
        response = self.client.get("/api/suppliers/")
        self.assertEqual(response.data["count"], 1)
        other_response = other_client.get("/api/suppliers/")
        self.assertEqual(other_response.data["count"], 0)

    def test_ordered_alphabetically_by_name(self):
        self._create_supplier(name="Zeta Fasteners")
        self._create_supplier(name="Ace Hardware")
        response = self.client.get("/api/suppliers/")
        names = [s["name"] for s in response.data["results"]]
        self.assertEqual(names, ["Ace Hardware", "Zeta Fasteners"])


class ExpenseSupplierLinkTests(AuthenticatedAPITestCase):
    def _supplier_id(self, name="Builders Warehouse"):
        return self.client.post("/api/suppliers/", {"name": name}, format="json").data["id"]

    def _create_expense(self, **overrides):
        payload = {
            "category": "materials_stock",
            "amount": "500.00",
            "is_vat_applicable": True,
            "date": "2026-08-01",
        }
        payload.update(overrides)
        return self.client.post("/api/expenses/", payload, format="json")

    def test_expense_can_link_to_a_supplier(self):
        supplier_id = self._supplier_id()
        response = self._create_expense(supplier_id=supplier_id)
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertEqual(str(response.data["supplier_id"]), supplier_id)

    def test_expense_without_supplier_is_allowed(self):
        response = self._create_expense()
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertIsNone(response.data["supplier_id"])

    def test_cannot_link_expense_to_another_businesss_supplier(self):
        other_client, other_business = self.make_other_business_client()
        other_supplier = Supplier.objects.create(business=other_business, name="Other Co Supplies")
        response = self._create_expense(supplier_id=str(other_supplier.id))
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_deleting_a_supplier_does_not_delete_its_expenses(self):
        # Deletion here is a soft delete (deleted_at set, row stays), same as
        # every other business-owned model — it never triggers the FK's
        # on_delete=SET_NULL, which only fires on a real DB row delete. The
        # expense keeps its history intact; only the supplier itself drops
        # out of the active supplier list.
        supplier_id = self._supplier_id()
        expense_id = self._create_expense(supplier_id=supplier_id).data["id"]
        self.client.delete(f"/api/suppliers/{supplier_id}/")

        expense = self.client.get(f"/api/expenses/{expense_id}/")
        self.assertEqual(expense.status_code, status.HTTP_200_OK)
        self.assertEqual(str(expense.data["supplier_id"]), supplier_id)

        suppliers = self.client.get("/api/suppliers/")
        self.assertEqual(suppliers.data["count"], 0)

from rest_framework import status
from rest_framework.test import APITestCase

from accounts.models import Business, Membership, User

VALID_BUSINESS = {
    "name": "Thabo's Plumbing & Maintenance",
    "phone": "+27821234567",
    "email": "info@thabosplumbing.co.za",
    "address_line1": "12 Vygie Street",
    "suburb": "Delft",
    "city": "Cape Town",
    "province": "WC",
    "postal_code": "7100",
    "industry": "plumbing",
    "is_vat_registered": False,
}


class RegisterTests(APITestCase):
    def test_register_creates_user_business_and_owner_membership(self):
        response = self.client.post(
            "/api/auth/register/",
            {
                "email": "Thabo@ThabosPlumbing.co.za",
                "password": "supersecret1",
                "first_name": "Thabo",
                "last_name": "Nkosi",
                "business": VALID_BUSINESS,
            },
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertIn("access", response.data)
        self.assertIn("refresh", response.data)

        user = User.objects.get(email="thabo@thabosplumbing.co.za")
        business = Business.objects.get(name="Thabo's Plumbing & Maintenance")
        membership = Membership.objects.get(user=user, business=business)
        self.assertEqual(membership.role, Membership.ROLE_OWNER)

    def test_register_rejects_duplicate_email(self):
        payload = {
            "email": "dupe@example.co.za",
            "password": "supersecret1",
            "business": VALID_BUSINESS,
        }
        first = self.client.post("/api/auth/register/", payload, format="json")
        self.assertEqual(first.status_code, status.HTTP_201_CREATED)
        second = self.client.post("/api/auth/register/", payload, format="json")
        self.assertEqual(second.status_code, status.HTTP_400_BAD_REQUEST)

    def test_register_rejects_short_password(self):
        payload = {"email": "short@example.co.za", "password": "abc", "business": VALID_BUSINESS}
        response = self.client.post("/api/auth/register/", payload, format="json")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)


class LoginTests(APITestCase):
    def setUp(self):
        self.client.post(
            "/api/auth/register/",
            {"email": "owner@example.co.za", "password": "correcthorse1", "business": VALID_BUSINESS},
            format="json",
        )

    def test_login_with_correct_credentials(self):
        response = self.client.post(
            "/api/auth/login/",
            {"email": "owner@example.co.za", "password": "correcthorse1"},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("access", response.data)
        self.assertEqual(response.data["business"]["name"], "Thabo's Plumbing & Maintenance")

    def test_login_with_wrong_password_is_rejected(self):
        response = self.client.post(
            "/api/auth/login/",
            {"email": "owner@example.co.za", "password": "wrong-password"},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)


class BusinessProfileTests(APITestCase):
    def setUp(self):
        response = self.client.post(
            "/api/auth/register/",
            {"email": "owner2@example.co.za", "password": "correcthorse1", "business": VALID_BUSINESS},
            format="json",
        )
        self.access = response.data["access"]

    def test_get_my_business_requires_auth(self):
        response = self.client.get("/api/business/me/")
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_get_and_update_my_business(self):
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {self.access}")
        get_response = self.client.get("/api/business/me/")
        self.assertEqual(get_response.status_code, status.HTTP_200_OK)
        self.assertEqual(get_response.data["name"], "Thabo's Plumbing & Maintenance")

        patch_response = self.client.patch(
            "/api/business/me/", {"trading_name": "Thabo's Plumbing"}, format="json"
        )
        self.assertEqual(patch_response.status_code, status.HTTP_200_OK)
        self.assertEqual(patch_response.data["trading_name"], "Thabo's Plumbing")

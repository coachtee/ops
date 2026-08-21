from rest_framework.test import APITestCase

from accounts.models import Business, Membership, User
from crm.models import Customer


def register_business(client, email, business_name="Test Business"):
    response = client.post(
        "/api/auth/register/",
        {
            "email": email,
            "password": "supersecret1",
            "first_name": "Owner",
            "last_name": "Test",
            "business": {
                "name": business_name,
                "phone": "+27821234567",
                "email": "info@example.co.za",
                "address_line1": "1 Main Road",
                "suburb": "Central",
                "city": "Cape Town",
                "province": "WC",
                "postal_code": "8001",
                "industry": "plumbing",
                "is_vat_registered": False,
            },
        },
        format="json",
    )
    assert response.status_code == 201, response.data
    return response.data


class AuthenticatedAPITestCase(APITestCase):
    """
    Sets up one business (Thabo's Plumbing), an authenticated client for its
    owner, and one customer — the common fixture most workflow tests need.
    """

    def setUp(self):
        data = register_business(self.client, "thabo@thabosplumbing.co.za", "Thabo's Plumbing")
        self.access = data["access"]
        self.business = Business.objects.get(id=data["business"]["id"])
        self.user = User.objects.get(id=data["user"]["id"])
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {self.access}")

        self.customer = Customer.objects.create(
            business=self.business,
            name="Sipho Khumalo",
            phone="+27823405566",
            email="sipho@example.co.za",
            address_line1="45 Protea Avenue",
            city="Cape Town",
        )

    def make_other_business_client(self):
        from rest_framework.test import APIClient

        other_client = APIClient()
        data = register_business(other_client, "other@otherbusiness.co.za", "Other Business")
        other_client.credentials(HTTP_AUTHORIZATION=f"Bearer {data['access']}")
        other_business = Business.objects.get(id=data["business"]["id"])
        return other_client, other_business

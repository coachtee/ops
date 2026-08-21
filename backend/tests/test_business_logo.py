import io

from django.core.files.uploadedfile import SimpleUploadedFile
from PIL import Image
from rest_framework import status

from .helpers import AuthenticatedAPITestCase


def _png_bytes():
    buf = io.BytesIO()
    Image.new("RGB", (64, 64), color=(10, 20, 30)).save(buf, format="PNG")
    return buf.getvalue()


class BusinessLogoTests(AuthenticatedAPITestCase):
    def test_uploaded_logo_is_stored_and_returned_on_the_business_profile(self):
        logo = SimpleUploadedFile("logo.png", _png_bytes(), content_type="image/png")
        response = self.client.patch("/api/business/me/", {"logo": logo}, format="multipart")
        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)
        self.assertTrue(response.data["logo"])

        fetched = self.client.get("/api/business/me/")
        self.assertTrue(fetched.data["logo"].endswith(".png"))

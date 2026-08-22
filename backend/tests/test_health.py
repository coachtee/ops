from rest_framework.test import APITestCase


class HealthCheckTests(APITestCase):
    """GET /api/health/ — infrastructure reachability, no auth required.
    See common/views.py's HealthView docstring: this is not a substitute
    for a real authenticated UAT pass, just the first thing to check when
    a client can't reach the server at all."""

    def test_health_check_requires_no_authentication(self):
        response = self.client.get("/api/health/")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data["status"], "ok")
        self.assertEqual(response.data["service"], "ops-api")
        self.assertEqual(response.data["database"], "ok")

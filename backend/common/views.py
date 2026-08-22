from django.db import connection
from django.utils import timezone
from rest_framework import viewsets
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.services import get_current_business


class HealthView(APIView):
    """
    GET /api/health/ — infrastructure reachability only: is Django up, and
    can it reach PostgreSQL. No auth required (that's the point — this has
    to answer before a client has any token at all), and it proves nothing
    about the actual application working. See docs/API_CONTRACT.md's
    "Health check" section: this is not a substitute for a real
    authenticated UAT pass.
    """

    permission_classes = [AllowAny]

    def get(self, request):
        try:
            with connection.cursor() as cursor:
                cursor.execute("SELECT 1")
            database_status = "ok"
        except Exception as exc:
            database_status = f"error: {exc}"

        return Response(
            {
                "status": "ok",
                "service": "ops-api",
                "database": database_status,
            },
        )


class BusinessScopedViewSet(viewsets.ModelViewSet):
    """
    Standard CRUD used for direct reads/admin/testing. The Android app's
    normal read/write path is the sync endpoints (sync/views.py); see
    docs/API_CONTRACT.md.
    """

    def get_queryset(self):
        business = get_current_business(self.request.user)
        return self.queryset.model.objects.filter(business=business, deleted_at__isnull=True)

    def get_serializer_context(self):
        context = super().get_serializer_context()
        context["business"] = get_current_business(self.request.user)
        return context

    def perform_create(self, serializer):
        serializer.save(business=get_current_business(self.request.user), updated_at=timezone.now())

    def perform_update(self, serializer):
        serializer.save(updated_at=timezone.now())

    def perform_destroy(self, instance):
        instance.deleted_at = timezone.now()
        instance.updated_at = timezone.now()
        instance.save(update_fields=["deleted_at", "updated_at"])

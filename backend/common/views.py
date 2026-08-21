from django.utils import timezone
from rest_framework import viewsets

from accounts.services import get_current_business


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

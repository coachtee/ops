from django.utils import timezone
from rest_framework import status
from rest_framework.decorators import action
from rest_framework.parsers import FormParser, JSONParser, MultiPartParser
from rest_framework.response import Response

from common.views import BusinessScopedViewSet

from .models import Job, Visit
from .serializers import JobSerializer, VisitPhotoUploadSerializer, VisitSerializer
from .services import assign_job_number_if_needed


class JobViewSet(BusinessScopedViewSet):
    queryset = Job.objects.all()
    serializer_class = JobSerializer

    def perform_create(self, serializer):
        super().perform_create(serializer)
        assign_job_number_if_needed(serializer.instance)


class VisitViewSet(BusinessScopedViewSet):
    queryset = Visit.objects.all()
    serializer_class = VisitSerializer

    @action(detail=True, methods=["post"], parser_classes=[MultiPartParser, FormParser, JSONParser])
    def photo(self, request, pk=None):
        """`POST /api/visits/{id}/photo/` — see VisitPhotoUploadSerializer."""
        visit = self.get_object()
        upload = VisitPhotoUploadSerializer(data=request.data)
        upload.is_valid(raise_exception=True)
        visit.photo = upload.validated_data["photo"]
        visit.updated_at = timezone.now()
        visit.save(update_fields=["photo", "updated_at"])
        return Response(VisitSerializer(visit, context=self.get_serializer_context()).data, status=status.HTTP_200_OK)

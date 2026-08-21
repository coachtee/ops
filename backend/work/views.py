from common.views import BusinessScopedViewSet

from .models import Job
from .serializers import JobSerializer
from .services import assign_job_number_if_needed


class JobViewSet(BusinessScopedViewSet):
    queryset = Job.objects.all()
    serializer_class = JobSerializer

    def perform_create(self, serializer):
        super().perform_create(serializer)
        assign_job_number_if_needed(serializer.instance)

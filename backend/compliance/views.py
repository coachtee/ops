from common.views import BusinessScopedViewSet

from .models import ComplianceItem
from .serializers import ComplianceItemSerializer


class ComplianceItemViewSet(BusinessScopedViewSet):
    queryset = ComplianceItem.objects.all()
    serializer_class = ComplianceItemSerializer

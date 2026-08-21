from common.views import BusinessScopedViewSet

from .models import Customer, Lead
from .serializers import CustomerSerializer, LeadSerializer


class LeadViewSet(BusinessScopedViewSet):
    queryset = Lead.objects.all()
    serializer_class = LeadSerializer


class CustomerViewSet(BusinessScopedViewSet):
    queryset = Customer.objects.all()
    serializer_class = CustomerSerializer

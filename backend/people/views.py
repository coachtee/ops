from common.views import BusinessScopedViewSet

from .models import Employee, Payslip
from .serializers import EmployeeSerializer, PayslipSerializer
from .services import recompute_payslip_net_pay


class EmployeeViewSet(BusinessScopedViewSet):
    queryset = Employee.objects.all()
    serializer_class = EmployeeSerializer


class PayslipViewSet(BusinessScopedViewSet):
    queryset = Payslip.objects.all()
    serializer_class = PayslipSerializer

    def perform_create(self, serializer):
        super().perform_create(serializer)
        recompute_payslip_net_pay(serializer.instance, bump_updated_at=False)

    def perform_update(self, serializer):
        super().perform_update(serializer)
        recompute_payslip_net_pay(serializer.instance, bump_updated_at=False)

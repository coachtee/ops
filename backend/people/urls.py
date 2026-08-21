from rest_framework.routers import DefaultRouter

from .views import EmployeeViewSet, PayslipViewSet

router = DefaultRouter()
router.register("employees", EmployeeViewSet, basename="employee")
router.register("payslips", PayslipViewSet, basename="payslip")

urlpatterns = router.urls

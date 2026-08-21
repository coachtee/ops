from rest_framework.routers import DefaultRouter

from .views import InvoiceLineItemViewSet, InvoiceViewSet, PaymentViewSet

router = DefaultRouter()
router.register("invoices", InvoiceViewSet, basename="invoice")
router.register("invoice-line-items", InvoiceLineItemViewSet, basename="invoice-line-item")
router.register("payments", PaymentViewSet, basename="payment")

urlpatterns = router.urls

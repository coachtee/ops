from rest_framework.routers import DefaultRouter

from .views import ExpenseViewSet, InvoiceLineItemViewSet, InvoiceViewSet, PaymentViewSet, SupplierViewSet

router = DefaultRouter()
router.register("invoices", InvoiceViewSet, basename="invoice")
router.register("invoice-line-items", InvoiceLineItemViewSet, basename="invoice-line-item")
router.register("payments", PaymentViewSet, basename="payment")
router.register("expenses", ExpenseViewSet, basename="expense")
router.register("suppliers", SupplierViewSet, basename="supplier")

urlpatterns = router.urls

from django.utils import timezone

from accounts.services import get_current_business
from common.views import BusinessScopedViewSet

from .models import Invoice, InvoiceLineItem, Payment
from .serializers import InvoiceLineItemSerializer, InvoiceSerializer, PaymentSerializer
from .services import (
    assign_invoice_number_if_needed,
    recompute_invoice_line_item_total,
    recompute_invoice_payment_state,
    recompute_invoice_totals,
)


class InvoiceViewSet(BusinessScopedViewSet):
    queryset = Invoice.objects.all()
    serializer_class = InvoiceSerializer

    def perform_create(self, serializer):
        super().perform_create(serializer)
        assign_invoice_number_if_needed(serializer.instance)


class InvoiceLineItemViewSet(BusinessScopedViewSet):
    queryset = InvoiceLineItem.objects.all()
    serializer_class = InvoiceLineItemSerializer

    def _save_with_total(self, serializer, **extra):
        instance = serializer.save(**extra)
        recompute_invoice_line_item_total(instance)
        instance.save(update_fields=["line_total"])
        recompute_invoice_totals(instance.invoice)
        return instance

    def perform_create(self, serializer):
        self._save_with_total(
            serializer, business=get_current_business(self.request.user), updated_at=timezone.now()
        )

    def perform_update(self, serializer):
        self._save_with_total(serializer, updated_at=timezone.now())

    def perform_destroy(self, instance):
        invoice = instance.invoice
        super().perform_destroy(instance)
        recompute_invoice_totals(invoice)


class PaymentViewSet(BusinessScopedViewSet):
    queryset = Payment.objects.all()
    serializer_class = PaymentSerializer

    def perform_create(self, serializer):
        super().perform_create(serializer)
        if serializer.instance.invoice_id:
            recompute_invoice_payment_state(serializer.instance.invoice)

    def perform_update(self, serializer):
        super().perform_update(serializer)
        if serializer.instance.invoice_id:
            recompute_invoice_payment_state(serializer.instance.invoice)

    def perform_destroy(self, instance):
        invoice = instance.invoice
        super().perform_destroy(instance)
        if invoice:
            recompute_invoice_payment_state(invoice)

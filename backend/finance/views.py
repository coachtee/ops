from django.utils import timezone
from rest_framework import status
from rest_framework.decorators import action
from rest_framework.parsers import FormParser, JSONParser, MultiPartParser
from rest_framework.response import Response

from accounts.services import get_current_business
from common.views import BusinessScopedViewSet

from .models import Expense, Invoice, InvoiceLineItem, Payment
from .serializers import (
    ExpenseReceiptUploadSerializer,
    ExpenseSerializer,
    InvoiceLineItemSerializer,
    InvoiceSerializer,
    PaymentSerializer,
)
from .services import (
    assign_invoice_number_if_needed,
    recompute_expense_vat,
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


class ExpenseViewSet(BusinessScopedViewSet):
    queryset = Expense.objects.all()
    serializer_class = ExpenseSerializer

    def perform_create(self, serializer):
        super().perform_create(serializer)
        recompute_expense_vat(serializer.instance, bump_updated_at=False)

    def perform_update(self, serializer):
        super().perform_update(serializer)
        recompute_expense_vat(serializer.instance, bump_updated_at=False)

    @action(detail=True, methods=["post"], parser_classes=[MultiPartParser, FormParser, JSONParser])
    def receipt(self, request, pk=None):
        """
        `POST /api/expenses/{id}/receipt/` — see API_CONTRACT.md's "Expense
        receipt attachments" addendum. Requires the expense to already exist
        (404 otherwise, same tenant-scoped lookup as everything else) —
        photos captured offline are held on-device until their parent
        expense has synced, precisely to avoid hitting this 404.
        """
        expense = self.get_object()
        upload = ExpenseReceiptUploadSerializer(data=request.data)
        upload.is_valid(raise_exception=True)
        expense.receipt_image = upload.validated_data["receipt"]
        expense.updated_at = timezone.now()
        expense.save(update_fields=["receipt_image", "updated_at"])
        return Response(ExpenseSerializer(expense, context=self.get_serializer_context()).data, status=status.HTTP_200_OK)

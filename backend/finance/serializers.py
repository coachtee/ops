from rest_framework import serializers

from common.serializers import validate_same_business
from crm.models import Customer
from sales.models import Quote
from work.models import Job

from .models import Invoice, InvoiceLineItem, Payment


class InvoiceLineItemSerializer(serializers.ModelSerializer):
    id = serializers.UUIDField(required=False)
    invoice_id = serializers.PrimaryKeyRelatedField(source="invoice", queryset=Invoice.objects.all())

    class Meta:
        model = InvoiceLineItem
        fields = [
            "id",
            "invoice_id",
            "description",
            "quantity",
            "unit_price",
            "line_total",
            "sort_order",
            "created_at",
            "updated_at",
            "deleted_at",
        ]
        read_only_fields = ["line_total", "created_at", "updated_at", "deleted_at"]

    def validate_invoice_id(self, value):
        return validate_same_business(value, self.context.get("business"))


class InvoiceSerializer(serializers.ModelSerializer):
    id = serializers.UUIDField(required=False)
    customer_id = serializers.PrimaryKeyRelatedField(source="customer", queryset=Customer.objects.all())
    job_id = serializers.PrimaryKeyRelatedField(
        source="job", queryset=Job.objects.all(), required=False, allow_null=True
    )
    quote_id = serializers.PrimaryKeyRelatedField(
        source="quote", queryset=Quote.objects.all(), required=False, allow_null=True
    )
    line_items = InvoiceLineItemSerializer(many=True, read_only=True)

    class Meta:
        model = Invoice
        fields = [
            "id",
            "customer_id",
            "job_id",
            "quote_id",
            "number",
            "status",
            "issue_date",
            "due_date",
            "notes",
            "terms",
            "is_vat_applicable",
            "discount_amount",
            "subtotal",
            "vat_amount",
            "total",
            "amount_paid",
            "sent_at",
            "line_items",
            "created_at",
            "updated_at",
            "deleted_at",
        ]
        read_only_fields = [
            "number",
            "subtotal",
            "vat_amount",
            "total",
            "amount_paid",
            "created_at",
            "updated_at",
            "deleted_at",
        ]

    def validate_customer_id(self, value):
        return validate_same_business(value, self.context.get("business"))

    def validate_job_id(self, value):
        return validate_same_business(value, self.context.get("business"))

    def validate_quote_id(self, value):
        return validate_same_business(value, self.context.get("business"))


class PaymentSerializer(serializers.ModelSerializer):
    id = serializers.UUIDField(required=False)
    customer_id = serializers.PrimaryKeyRelatedField(source="customer", queryset=Customer.objects.all())
    invoice_id = serializers.PrimaryKeyRelatedField(
        source="invoice", queryset=Invoice.objects.all(), required=False, allow_null=True
    )

    class Meta:
        model = Payment
        fields = [
            "id",
            "customer_id",
            "invoice_id",
            "amount",
            "method",
            "reference",
            "paid_date",
            "notes",
            "created_at",
            "updated_at",
            "deleted_at",
        ]
        read_only_fields = ["created_at", "updated_at", "deleted_at"]

    def validate_customer_id(self, value):
        return validate_same_business(value, self.context.get("business"))

    def validate_invoice_id(self, value):
        return validate_same_business(value, self.context.get("business"))

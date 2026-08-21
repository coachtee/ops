from datetime import timedelta

from django.utils import timezone
from rest_framework import serializers

from common.serializers import validate_same_business
from crm.models import Customer
from sales.models import Quote
from work.models import Job

from .models import Expense, Invoice, InvoiceLineItem, Payment, Supplier


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


class SupplierSerializer(serializers.ModelSerializer):
    id = serializers.UUIDField(required=False)

    class Meta:
        model = Supplier
        fields = [
            "id",
            "name",
            "contact_person",
            "phone",
            "email",
            "notes",
            "created_at",
            "updated_at",
            "deleted_at",
        ]
        read_only_fields = ["created_at", "updated_at", "deleted_at"]

    def validate_name(self, value):
        if not value.strip():
            raise serializers.ValidationError("Supplier name is required.")
        return value


class ExpenseSerializer(serializers.ModelSerializer):
    id = serializers.UUIDField(required=False)
    job_id = serializers.PrimaryKeyRelatedField(
        source="job", queryset=Job.objects.all(), required=False, allow_null=True
    )
    supplier_id = serializers.PrimaryKeyRelatedField(
        source="supplier", queryset=Supplier.objects.all(), required=False, allow_null=True
    )

    class Meta:
        model = Expense
        fields = [
            "id",
            "job_id",
            "supplier_id",
            "category",
            "description",
            "amount",
            "is_vat_applicable",
            "vat_amount",
            "date",
            "receipt_image",
            "created_at",
            "updated_at",
            "deleted_at",
        ]
        read_only_fields = ["vat_amount", "receipt_image", "created_at", "updated_at", "deleted_at"]

    def validate_job_id(self, value):
        return validate_same_business(value, self.context.get("business"))

    def validate_supplier_id(self, value):
        return validate_same_business(value, self.context.get("business"))

    def validate_amount(self, value):
        if value <= 0:
            raise serializers.ValidationError("Amount must be greater than zero.")
        return value

    def validate_date(self, value):
        # A day of slack for timezone edge cases — this is a hard stop
        # against fat-fingering a wrong year, not a strict same-day rule.
        if value > timezone.localdate() + timedelta(days=1):
            raise serializers.ValidationError("Expense date can't be in the future.")
        return value


MAX_RECEIPT_SIZE_BYTES = 10 * 1024 * 1024  # 10MB — a phone camera photo, not a scanned book.


class ExpenseReceiptUploadSerializer(serializers.Serializer):
    """
    Receipt photos travel outside the JSON sync protocol — see
    API_CONTRACT.md's "Expense receipt attachments" addendum for why:
    binary data doesn't fit the `changes` batch shape, and the parent
    Expense must already exist server-side before its receipt can be
    attached to it (this endpoint 404s otherwise).
    """

    receipt = serializers.ImageField(required=True)

    def validate_receipt(self, value):
        if value.size > MAX_RECEIPT_SIZE_BYTES:
            raise serializers.ValidationError("Receipt photo must be 10MB or smaller.")
        return value

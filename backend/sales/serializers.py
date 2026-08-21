from rest_framework import serializers

from common.serializers import validate_same_business
from crm.models import Customer, Lead

from .models import Quote, QuoteLineItem


class QuoteLineItemSerializer(serializers.ModelSerializer):
    id = serializers.UUIDField(required=False)
    quote_id = serializers.PrimaryKeyRelatedField(source="quote", queryset=Quote.objects.all())

    class Meta:
        model = QuoteLineItem
        fields = [
            "id",
            "quote_id",
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

    def validate_quote_id(self, value):
        return validate_same_business(value, self.context.get("business"))


class QuoteSerializer(serializers.ModelSerializer):
    id = serializers.UUIDField(required=False)
    customer_id = serializers.PrimaryKeyRelatedField(source="customer", queryset=Customer.objects.all())
    lead_id = serializers.PrimaryKeyRelatedField(
        source="lead", queryset=Lead.objects.all(), required=False, allow_null=True
    )
    line_items = QuoteLineItemSerializer(many=True, read_only=True)

    class Meta:
        model = Quote
        fields = [
            "id",
            "customer_id",
            "lead_id",
            "number",
            "status",
            "issue_date",
            "valid_until",
            "notes",
            "terms",
            "is_vat_applicable",
            "discount_amount",
            "subtotal",
            "vat_amount",
            "total",
            "sent_at",
            "accepted_at",
            "declined_at",
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
            "created_at",
            "updated_at",
            "deleted_at",
        ]

    def validate_customer_id(self, value):
        return validate_same_business(value, self.context.get("business"))

    def validate_lead_id(self, value):
        return validate_same_business(value, self.context.get("business"))

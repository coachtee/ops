from rest_framework import serializers

from common.serializers import validate_same_business

from .models import Customer, Lead


class CustomerSerializer(serializers.ModelSerializer):
    id = serializers.UUIDField(required=False)
    source_lead_id = serializers.PrimaryKeyRelatedField(
        source="source_lead", queryset=Lead.objects.all(), required=False, allow_null=True
    )

    class Meta:
        model = Customer
        fields = [
            "id",
            "name",
            "customer_type",
            "phone",
            "email",
            "address_line1",
            "address_line2",
            "suburb",
            "city",
            "province",
            "postal_code",
            "notes",
            "source_lead_id",
            "created_at",
            "updated_at",
            "deleted_at",
        ]
        read_only_fields = ["created_at", "updated_at", "deleted_at"]

    def validate_source_lead_id(self, value):
        return validate_same_business(value, self.context.get("business"))


class LeadSerializer(serializers.ModelSerializer):
    id = serializers.UUIDField(required=False)
    converted_customer_id = serializers.PrimaryKeyRelatedField(
        source="converted_customer",
        queryset=Customer.objects.all(),
        required=False,
        allow_null=True,
    )

    class Meta:
        model = Lead
        fields = [
            "id",
            "name",
            "phone",
            "email",
            "source",
            "enquiry",
            "notes",
            "status",
            "follow_up_date",
            "converted_customer_id",
            "created_at",
            "updated_at",
            "deleted_at",
        ]
        read_only_fields = ["created_at", "updated_at", "deleted_at"]

    def validate_converted_customer_id(self, value):
        return validate_same_business(value, self.context.get("business"))

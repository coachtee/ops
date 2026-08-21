from rest_framework import serializers

from common.serializers import validate_same_business
from crm.models import Customer
from sales.models import Quote

from .models import Job


class JobSerializer(serializers.ModelSerializer):
    id = serializers.UUIDField(required=False)
    customer_id = serializers.PrimaryKeyRelatedField(source="customer", queryset=Customer.objects.all())
    quote_id = serializers.PrimaryKeyRelatedField(
        source="quote", queryset=Quote.objects.all(), required=False, allow_null=True
    )

    class Meta:
        model = Job
        fields = [
            "id",
            "customer_id",
            "quote_id",
            "number",
            "title",
            "description",
            "status",
            "start_date",
            "due_date",
            "completed_date",
            "created_at",
            "updated_at",
            "deleted_at",
        ]
        read_only_fields = ["number", "created_at", "updated_at", "deleted_at"]

    def validate_customer_id(self, value):
        return validate_same_business(value, self.context.get("business"))

    def validate_quote_id(self, value):
        return validate_same_business(value, self.context.get("business"))

from rest_framework import serializers

from common.serializers import validate_same_business
from crm.models import Customer
from people.models import Employee
from sales.models import Quote

from .models import Job, Visit


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


class VisitSerializer(serializers.ModelSerializer):
    id = serializers.UUIDField(required=False)
    job_id = serializers.PrimaryKeyRelatedField(source="job", queryset=Job.objects.all())
    employee_id = serializers.PrimaryKeyRelatedField(
        source="employee", queryset=Employee.objects.all(), required=False, allow_null=True
    )

    class Meta:
        model = Visit
        fields = [
            "id",
            "job_id",
            "employee_id",
            "scheduled_date",
            "start_time",
            "end_time",
            "status",
            "notes",
            "started_at",
            "completed_at",
            "photo",
            "created_at",
            "updated_at",
            "deleted_at",
        ]
        # `photo` travels outside the JSON sync payload — see
        # VisitPhotoUploadSerializer / VisitViewSet.photo, same pattern as
        # Expense.receipt_image.
        read_only_fields = ["photo", "created_at", "updated_at", "deleted_at"]

    def validate_job_id(self, value):
        return validate_same_business(value, self.context.get("business"))

    def validate_employee_id(self, value):
        return validate_same_business(value, self.context.get("business"))


MAX_VISIT_PHOTO_SIZE_BYTES = 10 * 1024 * 1024  # 10MB — a phone camera photo, not a scanned book.


class VisitPhotoUploadSerializer(serializers.Serializer):
    """
    `POST /api/visits/{id}/photo/` — see API_CONTRACT.md's "Visit photo
    attachment" addendum. Requires the visit to already exist server-side
    (404 otherwise); a photo captured offline is held on-device until its
    parent visit has synced, exactly like an Expense receipt.
    """

    photo = serializers.ImageField(required=True)

    def validate_photo(self, value):
        if value.size > MAX_VISIT_PHOTO_SIZE_BYTES:
            raise serializers.ValidationError("Photo must be 10MB or smaller.")
        return value

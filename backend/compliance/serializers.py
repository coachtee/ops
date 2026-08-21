from rest_framework import serializers

from .models import ComplianceItem


class ComplianceItemSerializer(serializers.ModelSerializer):
    id = serializers.UUIDField(required=False)

    class Meta:
        model = ComplianceItem
        fields = [
            "id",
            "category",
            "title",
            "due_date",
            "completed_date",
            "is_recurring",
            "notes",
            "created_at",
            "updated_at",
            "deleted_at",
        ]
        read_only_fields = ["created_at", "updated_at", "deleted_at"]

    def validate_title(self, value):
        if not value.strip():
            raise serializers.ValidationError("Title is required.")
        return value

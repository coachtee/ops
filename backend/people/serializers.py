from rest_framework import serializers

from common.serializers import validate_same_business

from .models import Employee, Payslip


class EmployeeSerializer(serializers.ModelSerializer):
    id = serializers.UUIDField(required=False)

    class Meta:
        model = Employee
        fields = [
            "id",
            "name",
            "role",
            "phone",
            "email",
            "pay_rate_type",
            "pay_rate",
            "start_date",
            "notes",
            "created_at",
            "updated_at",
            "deleted_at",
        ]
        read_only_fields = ["created_at", "updated_at", "deleted_at"]

    def validate_name(self, value):
        if not value.strip():
            raise serializers.ValidationError("Employee name is required.")
        return value

    def validate_pay_rate(self, value):
        if value < 0:
            raise serializers.ValidationError("Pay rate can't be negative.")
        return value


class PayslipSerializer(serializers.ModelSerializer):
    id = serializers.UUIDField(required=False)
    employee_id = serializers.PrimaryKeyRelatedField(source="employee", queryset=Employee.objects.all())

    class Meta:
        model = Payslip
        fields = [
            "id",
            "employee_id",
            "period_start",
            "period_end",
            "gross_pay",
            "deductions",
            "deductions_note",
            "net_pay",
            "paid_date",
            "notes",
            "created_at",
            "updated_at",
            "deleted_at",
        ]
        read_only_fields = ["net_pay", "created_at", "updated_at", "deleted_at"]

    def validate_employee_id(self, value):
        return validate_same_business(value, self.context.get("business"))

    def validate_gross_pay(self, value):
        if value <= 0:
            raise serializers.ValidationError("Gross pay must be greater than zero.")
        return value

    def validate_deductions(self, value):
        if value < 0:
            raise serializers.ValidationError("Deductions can't be negative.")
        return value

    def validate(self, attrs):
        period_start = attrs.get("period_start", getattr(self.instance, "period_start", None))
        period_end = attrs.get("period_end", getattr(self.instance, "period_end", None))
        if period_start and period_end and period_end < period_start:
            raise serializers.ValidationError({"period_end": "Period end can't be before period start."})

        gross_pay = attrs.get("gross_pay", getattr(self.instance, "gross_pay", None))
        deductions = attrs.get("deductions", getattr(self.instance, "deductions", None))
        if gross_pay is not None and deductions is not None and deductions > gross_pay:
            raise serializers.ValidationError({"deductions": "Deductions can't be more than gross pay."})
        return attrs

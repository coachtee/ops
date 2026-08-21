from django.contrib import admin

from .models import Employee, Payslip


class PayslipInline(admin.TabularInline):
    model = Payslip
    extra = 0


@admin.register(Employee)
class EmployeeAdmin(admin.ModelAdmin):
    list_display = ["name", "role", "pay_rate_type", "pay_rate", "phone", "business"]
    list_filter = ["pay_rate_type", "business"]
    search_fields = ["name", "role"]
    inlines = [PayslipInline]


@admin.register(Payslip)
class PayslipAdmin(admin.ModelAdmin):
    list_display = ["employee", "period_start", "period_end", "gross_pay", "net_pay", "paid_date", "business"]
    list_filter = ["business"]
    search_fields = ["employee__name"]

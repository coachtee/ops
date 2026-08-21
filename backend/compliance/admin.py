from django.contrib import admin

from .models import ComplianceItem


@admin.register(ComplianceItem)
class ComplianceItemAdmin(admin.ModelAdmin):
    list_display = ["title", "category", "due_date", "completed_date", "business"]
    list_filter = ["category", "business"]
    search_fields = ["title"]

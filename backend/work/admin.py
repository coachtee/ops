from django.contrib import admin

from .models import Job


@admin.register(Job)
class JobAdmin(admin.ModelAdmin):
    list_display = ["number", "title", "customer", "status", "due_date", "business"]
    list_filter = ["status", "business"]
    search_fields = ["number", "title", "customer__name"]

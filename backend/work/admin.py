from django.contrib import admin

from .models import Job, Visit


@admin.register(Job)
class JobAdmin(admin.ModelAdmin):
    list_display = ["number", "title", "customer", "status", "due_date", "business"]
    list_filter = ["status", "business"]
    search_fields = ["number", "title", "customer__name"]


@admin.register(Visit)
class VisitAdmin(admin.ModelAdmin):
    list_display = ["job", "employee", "scheduled_date", "status", "business"]
    list_filter = ["status", "business"]
    search_fields = ["job__number", "job__title"]

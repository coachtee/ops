from django.contrib import admin

from .models import Customer, Lead


@admin.register(Lead)
class LeadAdmin(admin.ModelAdmin):
    list_display = ["name", "phone", "source", "status", "follow_up_date", "business"]
    list_filter = ["status", "source", "business"]
    search_fields = ["name", "phone", "email"]


@admin.register(Customer)
class CustomerAdmin(admin.ModelAdmin):
    list_display = ["name", "phone", "customer_type", "city", "business"]
    list_filter = ["customer_type", "business"]
    search_fields = ["name", "phone", "email"]

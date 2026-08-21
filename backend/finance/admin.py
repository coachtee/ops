from django.contrib import admin

from .models import Expense, Invoice, InvoiceLineItem, Payment, Supplier


class InvoiceLineItemInline(admin.TabularInline):
    model = InvoiceLineItem
    extra = 0


@admin.register(Invoice)
class InvoiceAdmin(admin.ModelAdmin):
    list_display = ["number", "customer", "status", "total", "amount_paid", "issue_date", "business"]
    list_filter = ["status", "business"]
    search_fields = ["number", "customer__name"]
    inlines = [InvoiceLineItemInline]


@admin.register(Payment)
class PaymentAdmin(admin.ModelAdmin):
    list_display = ["customer", "invoice", "amount", "method", "paid_date", "business"]
    list_filter = ["method", "business"]


@admin.register(Expense)
class ExpenseAdmin(admin.ModelAdmin):
    list_display = ["date", "category", "description", "amount", "vat_amount", "job", "supplier", "business"]
    list_filter = ["category", "business"]
    search_fields = ["description"]


@admin.register(Supplier)
class SupplierAdmin(admin.ModelAdmin):
    list_display = ["name", "contact_person", "phone", "email", "business"]
    list_filter = ["business"]
    search_fields = ["name", "contact_person"]

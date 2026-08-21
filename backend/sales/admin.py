from django.contrib import admin

from .models import Quote, QuoteLineItem


class QuoteLineItemInline(admin.TabularInline):
    model = QuoteLineItem
    extra = 0


@admin.register(Quote)
class QuoteAdmin(admin.ModelAdmin):
    list_display = ["number", "customer", "status", "total", "issue_date", "business"]
    list_filter = ["status", "business"]
    search_fields = ["number", "customer__name"]
    inlines = [QuoteLineItemInline]

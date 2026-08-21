from django.apps import AppConfig


class SyncConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    name = "sync"

    def ready(self):
        from crm.models import Customer, Lead
        from crm.serializers import CustomerSerializer, LeadSerializer
        from finance.models import Expense, Invoice, InvoiceLineItem, Payment
        from finance.serializers import (
            ExpenseSerializer,
            InvoiceLineItemSerializer,
            InvoiceSerializer,
            PaymentSerializer,
        )
        from sales.models import Quote, QuoteLineItem
        from sales.serializers import QuoteLineItemSerializer, QuoteSerializer
        from work.models import Job
        from work.serializers import JobSerializer

        from . import registry

        registry.register("lead", Lead, LeadSerializer)
        registry.register("customer", Customer, CustomerSerializer)
        registry.register("quote", Quote, QuoteSerializer)
        registry.register("quote_line_item", QuoteLineItem, QuoteLineItemSerializer)
        registry.register("job", Job, JobSerializer)
        registry.register("invoice", Invoice, InvoiceSerializer)
        registry.register("invoice_line_item", InvoiceLineItem, InvoiceLineItemSerializer)
        registry.register("payment", Payment, PaymentSerializer)
        registry.register("expense", Expense, ExpenseSerializer)

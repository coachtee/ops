from decimal import Decimal

from django.db import models

from common.models import BusinessOwnedModel


class Invoice(BusinessOwnedModel):
    STATUS_DRAFT = "draft"
    STATUS_SENT = "sent"
    STATUS_PARTIALLY_PAID = "partially_paid"
    STATUS_PAID = "paid"
    STATUS_OVERDUE = "overdue"
    STATUS_CANCELLED = "cancelled"
    STATUS_CHOICES = [
        (STATUS_DRAFT, "Draft"),
        (STATUS_SENT, "Sent"),
        (STATUS_PARTIALLY_PAID, "Partially paid"),
        (STATUS_PAID, "Paid"),
        (STATUS_OVERDUE, "Overdue"),
        (STATUS_CANCELLED, "Cancelled"),
    ]

    customer = models.ForeignKey("crm.Customer", on_delete=models.CASCADE, related_name="invoices")
    job = models.ForeignKey(
        "work.Job", on_delete=models.SET_NULL, null=True, blank=True, related_name="invoices"
    )
    quote = models.ForeignKey(
        "sales.Quote", on_delete=models.SET_NULL, null=True, blank=True, related_name="invoices"
    )
    number = models.CharField(max_length=20, null=True, blank=True)
    status = models.CharField(max_length=15, choices=STATUS_CHOICES, default=STATUS_DRAFT)
    issue_date = models.DateField()
    due_date = models.DateField(null=True, blank=True)
    notes = models.TextField(blank=True)
    terms = models.TextField(blank=True)
    is_vat_applicable = models.BooleanField(default=True)
    discount_amount = models.DecimalField(max_digits=12, decimal_places=2, default=Decimal("0.00"))
    subtotal = models.DecimalField(max_digits=12, decimal_places=2, default=Decimal("0.00"))
    vat_amount = models.DecimalField(max_digits=12, decimal_places=2, default=Decimal("0.00"))
    total = models.DecimalField(max_digits=12, decimal_places=2, default=Decimal("0.00"))
    amount_paid = models.DecimalField(max_digits=12, decimal_places=2, default=Decimal("0.00"))
    sent_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        ordering = ["-created_at"]

    @property
    def outstanding_amount(self) -> Decimal:
        return self.total - self.amount_paid

    def __str__(self):
        return self.number or f"Draft invoice for {self.customer.name}"


class InvoiceLineItem(BusinessOwnedModel):
    invoice = models.ForeignKey(Invoice, on_delete=models.CASCADE, related_name="line_items")
    description = models.CharField(max_length=255)
    quantity = models.DecimalField(max_digits=10, decimal_places=2, default=Decimal("1.00"))
    unit_price = models.DecimalField(max_digits=12, decimal_places=2, default=Decimal("0.00"))
    line_total = models.DecimalField(max_digits=12, decimal_places=2, default=Decimal("0.00"))
    sort_order = models.PositiveIntegerField(default=0)

    class Meta:
        ordering = ["sort_order", "created_at"]

    def __str__(self):
        return self.description


class Payment(BusinessOwnedModel):
    METHOD_CASH = "cash"
    METHOD_EFT = "eft"
    METHOD_CARD = "card"
    METHOD_SNAPSCAN = "snapscan"
    METHOD_OTHER = "other"
    METHOD_CHOICES = [
        (METHOD_CASH, "Cash"),
        (METHOD_EFT, "EFT"),
        (METHOD_CARD, "Card"),
        (METHOD_SNAPSCAN, "SnapScan"),
        (METHOD_OTHER, "Other"),
    ]

    customer = models.ForeignKey("crm.Customer", on_delete=models.CASCADE, related_name="payments")
    invoice = models.ForeignKey(
        Invoice, on_delete=models.SET_NULL, null=True, blank=True, related_name="payments"
    )
    amount = models.DecimalField(max_digits=12, decimal_places=2)
    method = models.CharField(max_length=10, choices=METHOD_CHOICES, default=METHOD_EFT)
    reference = models.CharField(max_length=100, blank=True)
    paid_date = models.DateField()
    notes = models.TextField(blank=True)

    class Meta:
        ordering = ["-paid_date"]

    def __str__(self):
        return f"R{self.amount} from {self.customer.name}"


class Supplier(BusinessOwnedModel):
    """Modelled for V1.1 (see docs/DISCOVERY.md); not exposed via API in this slice."""

    name = models.CharField(max_length=255)
    phone = models.CharField(max_length=20, blank=True)
    email = models.EmailField(blank=True)
    notes = models.TextField(blank=True)

    def __str__(self):
        return self.name


class Expense(BusinessOwnedModel):
    """Modelled for V1.1 (see docs/DISCOVERY.md); not exposed via API in this slice."""

    supplier = models.ForeignKey(
        Supplier, on_delete=models.SET_NULL, null=True, blank=True, related_name="expenses"
    )
    job = models.ForeignKey(
        "work.Job", on_delete=models.SET_NULL, null=True, blank=True, related_name="expenses"
    )
    category = models.CharField(max_length=100, blank=True)
    description = models.CharField(max_length=255, blank=True)
    amount = models.DecimalField(max_digits=12, decimal_places=2)
    vat_amount = models.DecimalField(max_digits=12, decimal_places=2, default=Decimal("0.00"))
    date = models.DateField()
    receipt_image = models.ImageField(upload_to="expenses/receipts/", null=True, blank=True)

    def __str__(self):
        return self.description or self.category

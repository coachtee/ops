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
    """Who the business buys from — kept deliberately simple (a contact
    record, not a procurement/vendor-management module). Linked from
    Expense.supplier (see below) so "what have I bought from them" is just
    that supplier's expenses, not a separate ledger."""

    name = models.CharField(max_length=255)
    contact_person = models.CharField(max_length=255, blank=True)
    phone = models.CharField(max_length=20, blank=True)
    email = models.EmailField(blank=True)
    notes = models.TextField(blank=True)

    class Meta:
        ordering = ["name"]

    def __str__(self):
        return self.name


def expense_receipt_upload_path(instance, filename):
    return f"business/{instance.business_id}/expenses/{instance.id}/receipt/{filename}"


class Expense(BusinessOwnedModel):
    """
    Money out. `amount` is the VAT-INCLUSIVE total the owner actually paid —
    what a receipt or bank statement shows — not a subtotal VAT gets added
    to. This is the opposite direction from Quote/Invoice: there the owner
    builds up a subtotal and VAT is added on top; here the owner already
    knows the total and `vat_amount` is the portion of it that was already
    VAT, extracted for their SARS input-VAT records. See
    common/money.py:extract_vat_from_inclusive.

    `supplier` is nullable — most expenses (fuel, bank charges, a cash
    purchase) have no supplier record at all, and that's fine.
    """

    CATEGORY_MATERIALS_STOCK = "materials_stock"
    CATEGORY_FUEL_TRAVEL = "fuel_travel"
    CATEGORY_TOOLS_EQUIPMENT = "tools_equipment"
    CATEGORY_RENT = "rent"
    CATEGORY_UTILITIES = "utilities"
    CATEGORY_INSURANCE = "insurance"
    CATEGORY_BANK_CHARGES = "bank_charges"
    CATEGORY_PROFESSIONAL_FEES = "professional_fees"
    CATEGORY_MARKETING = "marketing"
    CATEGORY_TELEPHONE_INTERNET = "telephone_internet"
    CATEGORY_VEHICLE = "vehicle"
    CATEGORY_REPAIRS_MAINTENANCE = "repairs_maintenance"
    CATEGORY_WAGES_SUBCONTRACTORS = "wages_subcontractors"
    CATEGORY_OTHER = "other"
    CATEGORY_CHOICES = [
        (CATEGORY_MATERIALS_STOCK, "Materials & stock"),
        (CATEGORY_FUEL_TRAVEL, "Fuel & travel"),
        (CATEGORY_TOOLS_EQUIPMENT, "Tools & equipment"),
        (CATEGORY_RENT, "Rent"),
        (CATEGORY_UTILITIES, "Utilities"),
        (CATEGORY_INSURANCE, "Insurance"),
        (CATEGORY_BANK_CHARGES, "Bank charges"),
        (CATEGORY_PROFESSIONAL_FEES, "Professional fees"),
        (CATEGORY_MARKETING, "Marketing & advertising"),
        (CATEGORY_TELEPHONE_INTERNET, "Telephone & internet"),
        (CATEGORY_VEHICLE, "Vehicle expenses"),
        (CATEGORY_REPAIRS_MAINTENANCE, "Repairs & maintenance"),
        (CATEGORY_WAGES_SUBCONTRACTORS, "Wages & subcontractors"),
        (CATEGORY_OTHER, "Other"),
    ]

    supplier = models.ForeignKey(
        Supplier, on_delete=models.SET_NULL, null=True, blank=True, related_name="expenses"
    )
    job = models.ForeignKey(
        "work.Job", on_delete=models.SET_NULL, null=True, blank=True, related_name="expenses"
    )
    category = models.CharField(max_length=30, choices=CATEGORY_CHOICES, default=CATEGORY_OTHER)
    description = models.CharField(max_length=255, blank=True)
    amount = models.DecimalField(max_digits=12, decimal_places=2)
    is_vat_applicable = models.BooleanField(default=False)
    vat_amount = models.DecimalField(max_digits=12, decimal_places=2, default=Decimal("0.00"))
    date = models.DateField()
    receipt_image = models.ImageField(
        upload_to=expense_receipt_upload_path, max_length=255, null=True, blank=True
    )

    class Meta:
        ordering = ["-date", "-created_at"]

    def __str__(self):
        return self.description or self.get_category_display()

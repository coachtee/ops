from decimal import Decimal

from django.db import models

from common.models import BusinessOwnedModel


class Employee(BusinessOwnedModel):
    """
    Who works for the business — kept deliberately simple (a staff contact
    plus the agreed pay rate, not a workforce-management system: no shift
    scheduling, no leave tracking, no org chart). `role` is free text (e.g.
    "Plumber's assistant"), not a fixed job-title taxonomy — trades vary too
    much for a rigid enum to be worth it. `pay_rate`/`pay_rate_type` are
    what the owner agreed with this person, shown back on the payslip form
    as a reminder — never used to auto-compute a payslip's gross_pay, since
    that would need hours/shift tracking this app deliberately doesn't do.
    """

    PAY_RATE_HOURLY = "hourly"
    PAY_RATE_DAILY = "daily"
    PAY_RATE_MONTHLY = "monthly"
    PAY_RATE_TYPE_CHOICES = [
        (PAY_RATE_HOURLY, "Hourly"),
        (PAY_RATE_DAILY, "Daily"),
        (PAY_RATE_MONTHLY, "Monthly"),
    ]

    name = models.CharField(max_length=255)
    role = models.CharField(max_length=255, blank=True)
    phone = models.CharField(max_length=20, blank=True)
    email = models.EmailField(blank=True)
    pay_rate_type = models.CharField(max_length=10, choices=PAY_RATE_TYPE_CHOICES, default=PAY_RATE_MONTHLY)
    pay_rate = models.DecimalField(max_digits=12, decimal_places=2, default=Decimal("0.00"))
    start_date = models.DateField(null=True, blank=True)
    notes = models.TextField(blank=True)

    class Meta:
        ordering = ["name"]

    def __str__(self):
        return self.name


class Payslip(BusinessOwnedModel):
    """
    One pay period for one employee. `gross_pay` and `deductions` are both
    entered by the owner (or copied from whatever number their bookkeeper
    gives them) — this app deliberately does not compute PAYE/UIF tax
    tables or claim any payroll-tax accuracy (see DISCOVERY.md's explicit
    "not built" list: "Payroll tax computation or e-filing; any claim of
    submitting to SARS"). `net_pay` is the one derived field, same pattern
    as Expense.vat_amount: always gross_pay - deductions, never entered by
    hand, so it can't drift from the two numbers it's made of.
    """

    employee = models.ForeignKey(Employee, on_delete=models.CASCADE, related_name="payslips")
    period_start = models.DateField()
    period_end = models.DateField()
    gross_pay = models.DecimalField(max_digits=12, decimal_places=2)
    deductions = models.DecimalField(max_digits=12, decimal_places=2, default=Decimal("0.00"))
    deductions_note = models.CharField(max_length=255, blank=True)
    net_pay = models.DecimalField(max_digits=12, decimal_places=2, default=Decimal("0.00"))
    paid_date = models.DateField(null=True, blank=True)
    notes = models.TextField(blank=True)

    class Meta:
        ordering = ["-period_end", "-created_at"]

    def __str__(self):
        return f"{self.employee.name} {self.period_start}–{self.period_end}"

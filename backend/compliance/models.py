from django.db import models

from common.models import BusinessOwnedModel


class ComplianceItem(BusinessOwnedModel):
    """
    A tracked regulatory deadline — SARS/CIPC obligations a South African
    small business needs to not miss, kept deliberately as a simple
    checklist-with-due-dates, not a filing system. This app never submits
    anything to SARS or CIPC, never computes what's owed, and never claims
    to know a business's actual filing status — it's the owner's own
    reminder list, in their own words. `category` only drives a suggested
    default `title` and how the Android app nudges "add the next one" once
    an item is marked done (both purely client-side conveniences); the
    server holds no recurrence/scheduling logic at all.
    """

    CATEGORY_VAT_RETURN = "vat_return"
    CATEGORY_PAYE_UIF_SDL = "paye_uif_sdl"
    CATEGORY_PROVISIONAL_TAX = "provisional_tax"
    CATEGORY_CIPC_ANNUAL_RETURN = "cipc_annual_return"
    CATEGORY_OTHER = "other"
    CATEGORY_CHOICES = [
        (CATEGORY_VAT_RETURN, "VAT return"),
        (CATEGORY_PAYE_UIF_SDL, "PAYE / UIF / SDL"),
        (CATEGORY_PROVISIONAL_TAX, "Provisional tax"),
        (CATEGORY_CIPC_ANNUAL_RETURN, "CIPC annual return"),
        (CATEGORY_OTHER, "Other"),
    ]

    category = models.CharField(max_length=20, choices=CATEGORY_CHOICES, default=CATEGORY_OTHER)
    title = models.CharField(max_length=255)
    due_date = models.DateField()
    completed_date = models.DateField(null=True, blank=True)
    is_recurring = models.BooleanField(default=True)
    notes = models.TextField(blank=True)

    class Meta:
        ordering = ["due_date"]

    def __str__(self):
        return self.title

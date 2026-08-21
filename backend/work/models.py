from django.db import models

from common.models import BusinessOwnedModel


class Job(BusinessOwnedModel):
    """
    "Work" in the product UI — the plain-language wrapper that adapts to
    whichever trade the owner is in (job, project, contract, site work).
    """

    STATUS_NOT_STARTED = "not_started"
    STATUS_IN_PROGRESS = "in_progress"
    STATUS_COMPLETED = "completed"
    STATUS_CANCELLED = "cancelled"
    STATUS_CHOICES = [
        (STATUS_NOT_STARTED, "Not started"),
        (STATUS_IN_PROGRESS, "In progress"),
        (STATUS_COMPLETED, "Completed"),
        (STATUS_CANCELLED, "Cancelled"),
    ]

    customer = models.ForeignKey("crm.Customer", on_delete=models.CASCADE, related_name="jobs")
    quote = models.ForeignKey(
        "sales.Quote", on_delete=models.SET_NULL, null=True, blank=True, related_name="jobs"
    )
    number = models.CharField(max_length=20, null=True, blank=True)
    title = models.CharField(max_length=255)
    description = models.TextField(blank=True)
    status = models.CharField(max_length=15, choices=STATUS_CHOICES, default=STATUS_NOT_STARTED)
    start_date = models.DateField(null=True, blank=True)
    due_date = models.DateField(null=True, blank=True)
    completed_date = models.DateField(null=True, blank=True)

    class Meta:
        ordering = ["-created_at"]

    def __str__(self):
        return self.number or self.title

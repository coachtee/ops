from django.db import models

from common.models import BusinessOwnedModel


def visit_photo_upload_path(instance, filename):
    return f"business/{instance.business_id}/visits/{instance.id}/photo/{filename}"


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


class Visit(BusinessOwnedModel):
    """
    One scheduled attendance against a [Job] — the operational layer between
    "the job exists" and "the job is invoiced": a Job can have more than one
    Visit (a multi-day installation, a callback), each with its own
    schedule, assigned employee, and completion record. Customer/address
    are deliberately NOT duplicated here — they're reached via `job.customer`,
    same as everywhere else in this system that avoids storing a second copy
    of relationship data that can drift.

    `photo` is a single slot, not a gallery — same simplification Expense's
    `receipt_image` makes (one photo per record, uploaded via its own
    dedicated endpoint below, outside the JSON sync payload). A future pass
    can promote this to a proper one-to-many attachment model if a single
    "proof of work" photo per visit turns out not to be enough in practice.
    """

    STATUS_SCHEDULED = "scheduled"
    STATUS_EN_ROUTE = "en_route"
    STATUS_IN_PROGRESS = "in_progress"
    STATUS_COMPLETED = "completed"
    STATUS_CANCELLED = "cancelled"
    STATUS_NEEDS_FOLLOW_UP = "needs_follow_up"
    STATUS_CHOICES = [
        (STATUS_SCHEDULED, "Scheduled"),
        (STATUS_EN_ROUTE, "En route"),
        (STATUS_IN_PROGRESS, "In progress"),
        (STATUS_COMPLETED, "Completed"),
        (STATUS_CANCELLED, "Cancelled"),
        (STATUS_NEEDS_FOLLOW_UP, "Needs follow-up"),
    ]

    job = models.ForeignKey(Job, on_delete=models.CASCADE, related_name="visits")
    employee = models.ForeignKey(
        "people.Employee", on_delete=models.SET_NULL, null=True, blank=True, related_name="visits"
    )
    scheduled_date = models.DateField()
    start_time = models.TimeField(null=True, blank=True)
    end_time = models.TimeField(null=True, blank=True)
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default=STATUS_SCHEDULED)
    notes = models.TextField(blank=True)
    started_at = models.DateTimeField(null=True, blank=True)
    completed_at = models.DateTimeField(null=True, blank=True)
    photo = models.ImageField(upload_to=visit_photo_upload_path, max_length=255, null=True, blank=True)

    class Meta:
        ordering = ["scheduled_date", "start_time", "-created_at"]

    def __str__(self):
        return f"Visit for {self.job} on {self.scheduled_date}"

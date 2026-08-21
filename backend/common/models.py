import uuid

from django.db import models
from django.utils import timezone


class BusinessOwnedModel(models.Model):
    """
    Base for every syncable, tenant-owned record.

    `id` is a client-generated UUID (set on the Android device at creation
    time) so offline creation never needs a round trip to get an id.
    `updated_at` is NOT auto_now: it is the sync "version" timestamp and is
    set explicitly wherever a record is written (see sync/services.py and
    each app's services), so last-write-wins comparisons stay meaningful
    even when a client's offline edit lands with an older wall-clock time
    than a later save. `deleted_at` is a soft delete so deletions sync too.
    """

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    business = models.ForeignKey(
        "accounts.Business", on_delete=models.CASCADE, related_name="+"
    )
    created_at = models.DateTimeField(default=timezone.now)
    updated_at = models.DateTimeField(default=timezone.now)
    deleted_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        abstract = True
        ordering = ["-created_at"]


class DocumentSequence(models.Model):
    """
    Per-business, per-document-type counter used to assign human-readable
    numbers (Q-0001, J-0001, INV-0001) atomically the first time a quote,
    job or invoice is synced to the server. See docs/API_CONTRACT.md.
    """

    business = models.ForeignKey(
        "accounts.Business", on_delete=models.CASCADE, related_name="sequences"
    )
    doc_type = models.CharField(max_length=20)
    last_number = models.PositiveIntegerField(default=0)

    class Meta:
        unique_together = ("business", "doc_type")

    def __str__(self):
        return f"{self.business_id}:{self.doc_type}={self.last_number}"


def next_document_number(business, doc_type: str, prefix: str) -> str:
    from django.db import transaction

    with transaction.atomic():
        seq, _ = DocumentSequence.objects.select_for_update().get_or_create(
            business=business, doc_type=doc_type
        )
        seq.last_number += 1
        seq.save(update_fields=["last_number"])
        return f"{prefix}-{seq.last_number:04d}"

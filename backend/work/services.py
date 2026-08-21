from common.models import next_document_number

from .models import Job


def assign_job_number_if_needed(job: Job) -> Job:
    if not job.number:
        job.number = next_document_number(job.business, "job", "J")
        job.save(update_fields=["number"])
    return job

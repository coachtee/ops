from django.db import models

from common.models import BusinessOwnedModel


class Customer(BusinessOwnedModel):
    TYPE_INDIVIDUAL = "individual"
    TYPE_COMPANY = "company"
    TYPE_CHOICES = [(TYPE_INDIVIDUAL, "Individual"), (TYPE_COMPANY, "Company")]

    name = models.CharField(max_length=255)
    customer_type = models.CharField(max_length=12, choices=TYPE_CHOICES, default=TYPE_INDIVIDUAL)
    phone = models.CharField(max_length=20)
    email = models.EmailField(blank=True)

    address_line1 = models.CharField(max_length=255, blank=True)
    address_line2 = models.CharField(max_length=255, blank=True)
    suburb = models.CharField(max_length=120, blank=True)
    city = models.CharField(max_length=120, blank=True)
    province = models.CharField(max_length=120, blank=True)
    postal_code = models.CharField(max_length=10, blank=True)

    notes = models.TextField(blank=True)
    source_lead = models.ForeignKey(
        "crm.Lead", on_delete=models.SET_NULL, null=True, blank=True, related_name="customers"
    )

    def __str__(self):
        return self.name


class Lead(BusinessOwnedModel):
    SOURCE_CHOICES = [
        ("whatsapp", "WhatsApp"),
        ("call", "Phone call"),
        ("facebook", "Facebook"),
        ("website", "Website"),
        ("email", "Email"),
        ("referral", "Referral"),
        ("walkin", "Walk-in"),
        ("tender", "Tender / RFQ"),
        ("other", "Other"),
    ]
    STATUS_NEW = "new"
    STATUS_CONTACTED = "contacted"
    STATUS_QUOTED = "quoted"
    STATUS_CONVERTED = "converted"
    STATUS_LOST = "lost"
    STATUS_CHOICES = [
        (STATUS_NEW, "New"),
        (STATUS_CONTACTED, "Contacted"),
        (STATUS_QUOTED, "Quoted"),
        (STATUS_CONVERTED, "Converted"),
        (STATUS_LOST, "Lost"),
    ]

    name = models.CharField(max_length=255)
    phone = models.CharField(max_length=20)
    email = models.EmailField(blank=True)
    source = models.CharField(max_length=20, choices=SOURCE_CHOICES, default="other")
    enquiry = models.TextField(blank=True)
    notes = models.TextField(blank=True)
    status = models.CharField(max_length=12, choices=STATUS_CHOICES, default=STATUS_NEW)
    follow_up_date = models.DateField(null=True, blank=True)
    converted_customer = models.ForeignKey(
        Customer, on_delete=models.SET_NULL, null=True, blank=True, related_name="converted_from_leads"
    )

    def __str__(self):
        return f"{self.name} ({self.get_status_display()})"

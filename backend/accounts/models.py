import uuid

from django.contrib.auth.base_user import AbstractBaseUser, BaseUserManager
from django.contrib.auth.models import PermissionsMixin
from django.db import models
from django.utils import timezone


class UserManager(BaseUserManager):
    def create_user(self, email, password=None, **extra_fields):
        if not email:
            raise ValueError("Users must have an email address")
        email = self.normalize_email(email)
        user = self.model(email=email, **extra_fields)
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_superuser(self, email, password=None, **extra_fields):
        extra_fields.setdefault("is_staff", True)
        extra_fields.setdefault("is_superuser", True)
        return self.create_user(email, password, **extra_fields)


class User(AbstractBaseUser, PermissionsMixin):
    """
    Email-based user. An owner installing OPS for the first time creates
    both their User and their Business in one step (see accounts.views.RegisterView).
    """

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    email = models.EmailField(unique=True)
    first_name = models.CharField(max_length=150, blank=True)
    last_name = models.CharField(max_length=150, blank=True)
    phone = models.CharField(max_length=20, blank=True)
    is_active = models.BooleanField(default=True)
    is_staff = models.BooleanField(default=False)
    date_joined = models.DateTimeField(default=timezone.now)

    objects = UserManager()

    USERNAME_FIELD = "email"
    REQUIRED_FIELDS = []

    def __str__(self):
        return self.email


INDUSTRY_CHOICES = [
    ("plumbing", "Plumbing"),
    ("electrical", "Electrical"),
    ("construction", "Construction / Building"),
    ("paving", "Paving / Landscaping"),
    ("security", "Security services"),
    ("consulting", "Consulting"),
    ("retail", "Retail / Spaza"),
    ("cleaning", "Cleaning services"),
    ("other", "Other"),
]

PROVINCE_CHOICES = [
    ("EC", "Eastern Cape"),
    ("FS", "Free State"),
    ("GP", "Gauteng"),
    ("KZN", "KwaZulu-Natal"),
    ("LP", "Limpopo"),
    ("MP", "Mpumalanga"),
    ("NC", "Northern Cape"),
    ("NW", "North West"),
    ("WC", "Western Cape"),
]


def logo_upload_path(instance, filename):
    return f"business/{instance.id}/logo/{filename}"


class Business(models.Model):
    """
    The tenant root. Every business-owned record carries a `business` FK
    (see common.models.BusinessOwnedModel). One business per user in V1
    (see docs/DISCOVERY.md, "Risks and assumptions").
    """

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    name = models.CharField(max_length=255)
    trading_name = models.CharField(max_length=255, blank=True)
    registration_number = models.CharField(
        "CIPC registration number", max_length=50, blank=True
    )
    tax_number = models.CharField("SARS income tax number", max_length=50, blank=True)
    vat_number = models.CharField(max_length=50, blank=True)
    is_vat_registered = models.BooleanField(default=False)
    industry = models.CharField(max_length=30, choices=INDUSTRY_CHOICES, default="other")

    phone = models.CharField(max_length=20, blank=True)
    email = models.EmailField(blank=True)

    address_line1 = models.CharField(max_length=255, blank=True)
    address_line2 = models.CharField(max_length=255, blank=True)
    suburb = models.CharField(max_length=120, blank=True)
    city = models.CharField(max_length=120, blank=True)
    province = models.CharField(max_length=3, choices=PROVINCE_CHOICES, blank=True)
    postal_code = models.CharField(max_length=10, blank=True)

    logo = models.ImageField(upload_to=logo_upload_path, max_length=255, null=True, blank=True)

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return self.name


class Membership(models.Model):
    ROLE_OWNER = "owner"
    ROLE_STAFF = "staff"
    ROLE_CHOICES = [(ROLE_OWNER, "Owner"), (ROLE_STAFF, "Staff")]

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name="memberships")
    business = models.ForeignKey(
        Business, on_delete=models.CASCADE, related_name="memberships"
    )
    role = models.CharField(max_length=10, choices=ROLE_CHOICES, default=ROLE_OWNER)
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ("user", "business")

    def __str__(self):
        return f"{self.user.email} @ {self.business.name} ({self.role})"

from django.contrib.auth import authenticate
from django.db import transaction
from rest_framework import serializers
from rest_framework_simplejwt.tokens import RefreshToken

from .models import Business, Membership, User


class BusinessSerializer(serializers.ModelSerializer):
    class Meta:
        model = Business
        fields = [
            "id",
            "name",
            "trading_name",
            "registration_number",
            "tax_number",
            "vat_number",
            "is_vat_registered",
            "industry",
            "phone",
            "email",
            "address_line1",
            "address_line2",
            "suburb",
            "city",
            "province",
            "postal_code",
            "logo",
            "created_at",
            "updated_at",
        ]
        read_only_fields = ["id", "created_at", "updated_at"]


class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ["id", "email", "first_name", "last_name", "phone"]
        read_only_fields = ["id"]


def _tokens_for(user):
    refresh = RefreshToken.for_user(user)
    return {"access": str(refresh.access_token), "refresh": str(refresh)}


class RegisterSerializer(serializers.Serializer):
    email = serializers.EmailField()
    password = serializers.CharField(min_length=8, write_only=True)
    first_name = serializers.CharField(max_length=150, required=False, allow_blank=True)
    last_name = serializers.CharField(max_length=150, required=False, allow_blank=True)
    business = BusinessSerializer()

    def validate_email(self, value):
        value = value.lower()
        if User.objects.filter(email=value).exists():
            raise serializers.ValidationError("An account with this email already exists.")
        return value

    @transaction.atomic
    def create(self, validated_data):
        business_data = validated_data.pop("business")
        password = validated_data.pop("password")
        business = Business.objects.create(**business_data)
        user = User.objects.create_user(password=password, **validated_data)
        Membership.objects.create(user=user, business=business, role=Membership.ROLE_OWNER)
        return {"user": user, "business": business}

    def to_representation(self, instance):
        user, business = instance["user"], instance["business"]
        return {
            "user": UserSerializer(user).data,
            "business": BusinessSerializer(business).data,
            **_tokens_for(user),
        }


class LoginSerializer(serializers.Serializer):
    email = serializers.EmailField()
    password = serializers.CharField(write_only=True)

    def validate(self, attrs):
        user = authenticate(email=attrs["email"].lower(), password=attrs["password"])
        if user is None or not user.is_active:
            raise serializers.ValidationError("Incorrect email or password.")
        attrs["user"] = user
        return attrs

    def to_representation(self, instance):
        user = instance["user"]
        from .services import get_current_business

        business = get_current_business(user)
        return {
            "user": UserSerializer(user).data,
            "business": BusinessSerializer(business).data,
            **_tokens_for(user),
        }

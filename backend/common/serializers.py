from rest_framework import serializers


def validate_same_business(value, business):
    """
    Cross-tenant guard: a related record (customer_id, quote_id, invoice_id,
    ...) supplied by the client must belong to the caller's own business,
    otherwise this is an IDOR — one business referencing another's data.
    """
    if value is not None and business is not None and value.business_id != business.id:
        raise serializers.ValidationError("This record does not belong to your business.")
    return value

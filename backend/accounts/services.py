from django.core.exceptions import PermissionDenied

from .models import Membership


def get_current_business(user):
    """
    V1 assumes one business per user (see docs/DISCOVERY.md). Every
    authenticated request derives its tenant from this — the client never
    sends a business id.
    """
    membership = (
        Membership.objects.filter(user=user, is_active=True)
        .select_related("business")
        .first()
    )
    if membership is None:
        raise PermissionDenied("This account is not linked to a business.")
    return membership.business

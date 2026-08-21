from django.utils import timezone

from common.views import BusinessScopedViewSet

from .models import Quote, QuoteLineItem
from .serializers import QuoteLineItemSerializer, QuoteSerializer
from .services import (
    assign_quote_number_if_needed,
    recompute_quote_line_item_total,
    recompute_quote_totals,
)


class QuoteViewSet(BusinessScopedViewSet):
    queryset = Quote.objects.all()
    serializer_class = QuoteSerializer

    def perform_create(self, serializer):
        super().perform_create(serializer)
        assign_quote_number_if_needed(serializer.instance)


class QuoteLineItemViewSet(BusinessScopedViewSet):
    queryset = QuoteLineItem.objects.all()
    serializer_class = QuoteLineItemSerializer

    def _save_with_total(self, serializer, **extra):
        instance = serializer.save(**extra)
        recompute_quote_line_item_total(instance)
        instance.save(update_fields=["line_total"])
        recompute_quote_totals(instance.quote)
        return instance

    def perform_create(self, serializer):
        from accounts.services import get_current_business

        self._save_with_total(
            serializer, business=get_current_business(self.request.user), updated_at=timezone.now()
        )

    def perform_update(self, serializer):
        self._save_with_total(serializer, updated_at=timezone.now())

    def perform_destroy(self, instance):
        quote = instance.quote
        super().perform_destroy(instance)
        recompute_quote_totals(quote)

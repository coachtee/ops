from rest_framework.routers import DefaultRouter

from .views import QuoteLineItemViewSet, QuoteViewSet

router = DefaultRouter()
router.register("quotes", QuoteViewSet, basename="quote")
router.register("quote-line-items", QuoteLineItemViewSet, basename="quote-line-item")

urlpatterns = router.urls

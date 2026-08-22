from rest_framework.routers import DefaultRouter

from .views import JobViewSet, VisitViewSet

router = DefaultRouter()
router.register("jobs", JobViewSet, basename="job")
router.register("visits", VisitViewSet, basename="visit")

urlpatterns = router.urls

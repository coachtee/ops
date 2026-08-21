from django.urls import path

from .views import MyBusinessView

urlpatterns = [
    path("business/me/", MyBusinessView.as_view(), name="business-me"),
]

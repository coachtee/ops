from django.conf import settings
from django.conf.urls.static import static
from django.contrib import admin
from django.urls import include, path

urlpatterns = [
    path("admin/", admin.site.urls),
    path("api/auth/", include("accounts.urls")),
    path("api/", include("accounts.business_urls")),
    path("api/", include("crm.urls")),
    path("api/", include("sales.urls")),
    path("api/", include("work.urls")),
    path("api/", include("finance.urls")),
    path("api/", include("people.urls")),
    path("api/", include("compliance.urls")),
    path("api/", include("reports.urls")),
    path("api/sync/", include("sync.urls")),
]

if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)

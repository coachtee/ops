from django.db import transaction
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.services import get_current_business

from .services import apply_push, build_pull


class SyncPushView(APIView):
    def post(self, request):
        business = get_current_business(request.user)
        changes = request.data.get("changes", [])
        with transaction.atomic():
            results = apply_push(business, changes)
        return Response({"results": results})


class SyncPullView(APIView):
    def get(self, request):
        business = get_current_business(request.user)
        since = request.query_params.get("since")
        return Response(build_pull(business, since))

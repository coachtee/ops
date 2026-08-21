from rest_framework import generics, permissions, status
from rest_framework.parsers import FormParser, JSONParser, MultiPartParser
from rest_framework.response import Response
from rest_framework.views import APIView

from .serializers import BusinessSerializer, LoginSerializer, RegisterSerializer
from .services import get_current_business


class RegisterView(APIView):
    permission_classes = [permissions.AllowAny]

    def post(self, request):
        serializer = RegisterSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        instance = serializer.save()
        return Response(RegisterSerializer(instance).data, status=status.HTTP_201_CREATED)


class LoginView(APIView):
    permission_classes = [permissions.AllowAny]

    def post(self, request):
        serializer = LoginSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        return Response(serializer.data, status=status.HTTP_200_OK)


class MyBusinessView(generics.RetrieveUpdateAPIView):
    serializer_class = BusinessSerializer
    parser_classes = [JSONParser, MultiPartParser, FormParser]

    def get_object(self):
        return get_current_business(self.request.user)

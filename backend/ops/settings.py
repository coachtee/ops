"""
Django settings for the OPS backend.

Kept deliberately small: a modular monolith, one deployable, apps as internal
boundaries. See docs/DISCOVERY.md and docs/API_CONTRACT.md at the repo root
for the architecture this implements.
"""

import os
import sys
from datetime import timedelta
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent

SECRET_KEY = os.environ.get("OPS_SECRET_KEY", "dev-only-insecure-secret-key-change-me")
DEBUG = os.environ.get("OPS_DEBUG", "1") == "1"
ALLOWED_HOSTS = os.environ.get("OPS_ALLOWED_HOSTS", "*").split(",")

# DEBUG defaults to on so `manage.py runserver`/`test` keep working with zero
# env vars, exactly as every README in this repo documents — flipping the
# default to "secure" would break that on every contributor's first run.
# What actually matters is that nobody can reach a real environment running
# on the insecure defaults: the moment OPS_DEBUG=0 is set (i.e. this is an
# actual shared/staging/production run, not someone's laptop), refuse to
# start at all if the secret key or allowed-hosts list is still a dev
# placeholder. This is deliberately loud and unconditional — see the task
# brief's "do not disable security simply to make the phone connect."
if not DEBUG and "test" not in sys.argv:
    if SECRET_KEY == "dev-only-insecure-secret-key-change-me":
        raise RuntimeError(
            "OPS_DEBUG=0 (a non-development run) but OPS_SECRET_KEY is unset or still the "
            "placeholder dev value. Set OPS_SECRET_KEY to a real random secret before running "
            "with DEBUG off."
        )
    if ALLOWED_HOSTS == ["*"]:
        raise RuntimeError(
            "OPS_DEBUG=0 (a non-development run) but OPS_ALLOWED_HOSTS is unset (defaults to "
            "'*'). Set OPS_ALLOWED_HOSTS to the real hostname(s) this server answers to."
        )

INSTALLED_APPS = [
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",
    "rest_framework",
    "rest_framework_simplejwt",
    "corsheaders",
    "common",
    "accounts",
    "crm",
    "sales",
    "work",
    "finance",
    "people",
    "compliance",
    "reports",
    "sync",
]

MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "corsheaders.middleware.CorsMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
]

ROOT_URLCONF = "ops.urls"

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [BASE_DIR / "templates"],
        "APP_DIRS": True,
        "OPTIONS": {
            "context_processors": [
                "django.template.context_processors.debug",
                "django.template.context_processors.request",
                "django.contrib.auth.context_processors.auth",
                "django.contrib.messages.context_processors.messages",
            ],
        },
    },
]

WSGI_APPLICATION = "ops.wsgi.application"

if os.environ.get("OPS_USE_SQLITE") == "1":
    DATABASES = {
        "default": {
            "ENGINE": "django.db.backends.sqlite3",
            "NAME": BASE_DIR / "db.sqlite3",
        }
    }
else:
    DATABASES = {
        "default": {
            "ENGINE": "django.db.backends.postgresql",
            "NAME": os.environ.get("OPS_DB_NAME", "ops"),
            "USER": os.environ.get("OPS_DB_USER", "ops"),
            "PASSWORD": os.environ.get("OPS_DB_PASSWORD", "ops"),
            "HOST": os.environ.get("OPS_DB_HOST", "localhost"),
            "PORT": os.environ.get("OPS_DB_PORT", "5432"),
        }
    }

AUTH_USER_MODEL = "accounts.User"

AUTH_PASSWORD_VALIDATORS = [
    {"NAME": "django.contrib.auth.password_validation.MinimumLengthValidator", "OPTIONS": {"min_length": 8}},
]

LANGUAGE_CODE = "en-za"
TIME_ZONE = "Africa/Johannesburg"
USE_I18N = True
USE_TZ = True

STATIC_URL = "static/"
MEDIA_URL = "/media/"
MEDIA_ROOT = BASE_DIR / "media"

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": (
        "rest_framework_simplejwt.authentication.JWTAuthentication",
    ),
    "DEFAULT_PERMISSION_CLASSES": (
        "rest_framework.permissions.IsAuthenticated",
    ),
    "DEFAULT_PAGINATION_CLASS": "rest_framework.pagination.PageNumberPagination",
    "PAGE_SIZE": 50,
    "DEFAULT_FILTER_BACKENDS": (),
}

SIMPLE_JWT = {
    "ACCESS_TOKEN_LIFETIME": timedelta(hours=8),
    "REFRESH_TOKEN_LIFETIME": timedelta(days=30),
    "ROTATE_REFRESH_TOKENS": True,
    "AUTH_HEADER_TYPES": ("Bearer",),
}

# The Android app is the only real client and doesn't send browser-style
# CORS preflight requests at all (CORS is a browser-enforced mechanism), so
# this setting has no effect on it either way. It only matters if a browser
# client (e.g. Django admin from another origin, or a future web app) is
# ever added — left wide open in DEBUG for convenience, restricted to an
# explicit allowlist otherwise so a real deployment doesn't silently accept
# cross-origin requests from anywhere.
CORS_ALLOW_ALL_ORIGINS = DEBUG
if not DEBUG:
    CORS_ALLOWED_ORIGINS = [o for o in os.environ.get("OPS_CORS_ALLOWED_ORIGINS", "").split(",") if o]

# HTTPS hardening — only applied outside DEBUG, so local HTTP dev (and a
# physical phone talking to a plain-HTTP LAN dev server, see android/README.md's
# "API configuration" section) is completely unaffected. SECURE_PROXY_SSL_HEADER
# assumes TLS is terminated by a reverse proxy/PaaS in front of Django (the
# normal deployment shape), which is expected to set X-Forwarded-Proto.
if not DEBUG:
    SECURE_SSL_REDIRECT = True
    SESSION_COOKIE_SECURE = True
    CSRF_COOKIE_SECURE = True
    SECURE_PROXY_SSL_HEADER = ("HTTP_X_FORWARDED_PROTO", "https")

# South African business context.
CURRENCY = "ZAR"
VAT_RATE = "0.15"

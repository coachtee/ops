"""
Model registry the sync engine is driven by. Each app registers its
syncable models (and the serializer that already validates its CRUD API)
in its AppConfig.ready() — see sync/apps.py — so adding a new syncable
model later is one line, not a bespoke endpoint.
"""

_REGISTRY: dict[str, tuple] = {}


def register(model_key: str, model_cls, serializer_cls):
    _REGISTRY[model_key] = (model_cls, serializer_cls)


def get_registered(model_key: str):
    try:
        return _REGISTRY[model_key]
    except KeyError:
        raise ValueError(f"'{model_key}' is not a syncable model")


def all_model_keys():
    return list(_REGISTRY.keys())

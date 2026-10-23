"""Load i18n/locales.yml — single source of truth for required locales."""
from __future__ import annotations

from functools import lru_cache
from pathlib import Path
from typing import Any

import yaml

REPO_ROOT = Path(__file__).resolve().parents[2]
LOCALES_CONFIG = REPO_ROOT / "i18n" / "locales.yml"


@lru_cache(maxsize=1)
def load_locales_config(path: Path | None = None) -> dict[str, Any]:
    cfg_path = path or LOCALES_CONFIG
    if not cfg_path.is_file():
        raise FileNotFoundError(f"Missing locales config: {cfg_path}")
    data = yaml.safe_load(cfg_path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError(f"locales.yml must be a mapping: {cfg_path}")
    required = data.get("required") or []
    source = data.get("source")
    if not source or source not in required:
        raise ValueError("locales.yml: source must be listed in required")
    if "ru" in {str(x).lower() for x in required}:
        raise ValueError("locales.yml must never list ru as required")
    return data


def required_locales(cfg: dict[str, Any] | None = None) -> list[str]:
    data = cfg or load_locales_config()
    return list(data["required"])


def source_locale(cfg: dict[str, Any] | None = None) -> str:
    data = cfg or load_locales_config()
    return str(data["source"])


def target_locales(cfg: dict[str, Any] | None = None) -> list[str]:
    data = cfg or load_locales_config()
    src = source_locale(data)
    return [loc for loc in required_locales(data) if loc != src]


def plural_categories(locale: str, cfg: dict[str, Any] | None = None) -> list[str]:
    data = cfg or load_locales_config()
    plurals = data.get("plurals") or {}
    cats = plurals.get(locale)
    if not cats:
        return ["other"]
    return list(cats)

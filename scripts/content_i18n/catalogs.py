"""Discover content locale catalog directories."""
from __future__ import annotations

from pathlib import Path
from typing import Any

from content_i18n.config import REPO_ROOT, source_locale


def discover_locale_dirs(cfg: dict[str, Any] | None = None) -> list[Path]:
    """Return locale dirs that contain the source en-GB.json, sorted.

    Always includes paths.content_locales when present. Also discovers
    content/<domain>/locales directories that ship an en-GB catalog.
    """
    from content_i18n.config import load_locales_config

    data = cfg or load_locales_config()
    paths = data.get("paths") or {}
    src = source_locale(data)
    found: list[Path] = []

    primary = REPO_ROOT / paths.get("content_locales", "content/exercises/locales")
    if (primary / f"{src}.json").is_file():
        found.append(primary)

    content_root = REPO_ROOT / "content"
    if content_root.is_dir():
        for domain in sorted(content_root.iterdir()):
            if not domain.is_dir():
                continue
            locales = domain / "locales"
            if locales.resolve() == primary.resolve():
                continue
            if (locales / f"{src}.json").is_file():
                found.append(locales)

    # de-dupe while preserving order
    out: list[Path] = []
    seen: set[Path] = set()
    for path in found:
        key = path.resolve()
        if key in seen:
            continue
        seen.add(key)
        out.append(path)
    return out


def catalog_id(locales_dir: Path) -> str:
    """Stable short id: content/yoga/locales -> yoga."""
    try:
        rel = locales_dir.resolve().relative_to(REPO_ROOT.resolve())
    except ValueError:
        return locales_dir.name
    parts = rel.parts
    if len(parts) >= 2 and parts[0] == "content":
        return parts[1]
    return locales_dir.name

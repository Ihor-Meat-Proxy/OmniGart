#!/usr/bin/env python3
"""Create/update per-locale status sidecars (machine-drafted | reviewed).

Default: mark all target-locale keys as machine-drafted with current source_hash.
Does not downgrade keys already marked reviewed unless --force.
Scans every discovered content/*/locales catalog.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from content_i18n.catalogs import discover_locale_dirs  # noqa: E402
from content_i18n.config import (  # noqa: E402
    REPO_ROOT,
    load_locales_config,
    source_locale,
    target_locales,
)


def _sha(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()[:16]


def build(force: bool = False, status_value: str = "machine-drafted") -> None:
    cfg = load_locales_config()
    paths = cfg.get("paths") or {}
    status_dir = REPO_ROOT / paths.get("status", "content/i18n/status")
    status_dir.mkdir(parents=True, exist_ok=True)
    src = source_locale(cfg)

    source: dict[str, str] = {}
    for locales_dir in discover_locale_dirs(cfg):
        src_path = locales_dir / f"{src}.json"
        data = json.loads(src_path.read_text(encoding="utf-8"))
        overlap = set(source) & set(data)
        if overlap:
            raise SystemExit(
                f"Duplicate i18n keys across catalogs near {sorted(overlap)[:5]}"
            )
        source.update(data)

    for loc in target_locales(cfg):
        path = status_dir / f"{loc}.json"
        existing = {}
        if path.is_file() and not force:
            existing = json.loads(path.read_text(encoding="utf-8"))
        out = dict(existing)
        for key, src_val in source.items():
            prev = out.get(key) or {}
            if prev.get("status") == "reviewed" and not force:
                out[key] = {
                    "status": "reviewed",
                    "source_hash": prev.get("source_hash") or _sha(src_val),
                }
            else:
                out[key] = {
                    "status": status_value,
                    "source_hash": _sha(src_val),
                }
        out = {k: v for k, v in out.items() if k in source}
        path.write_text(
            json.dumps(dict(sorted(out.items())), ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        print(f"wrote {path} ({len(out)} keys)")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--force", action="store_true")
    parser.add_argument(
        "--status",
        default="machine-drafted",
        choices=["machine-drafted", "reviewed"],
    )
    args = parser.parse_args()
    build(force=args.force, status_value=args.status)


if __name__ == "__main__":
    main()

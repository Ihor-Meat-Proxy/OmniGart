#!/usr/bin/env python3
"""Import Weblate-exported JSON components back into content/exercises/locales."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from content_i18n.config import (  # noqa: E402
    REPO_ROOT,
    load_locales_config,
    required_locales,
)


def import_tree(src_dir: Path, dry_run: bool = False) -> None:
    cfg = load_locales_config()
    paths = cfg.get("paths") or {}
    locales_dir = REPO_ROOT / paths.get("content_locales", "content/exercises/locales")
    locales_dir.mkdir(parents=True, exist_ok=True)

    for loc in required_locales(cfg):
        merged: dict[str, str] = {}
        existing = locales_dir / f"{loc}.json"
        if existing.is_file():
            merged.update(json.loads(existing.read_text(encoding="utf-8")))
        for component in ("exercises", "templates"):
            path = src_dir / component / f"{loc}.json"
            if path.is_file():
                merged.update(json.loads(path.read_text(encoding="utf-8")))
        if dry_run:
            print(f"{loc}: {len(merged)} keys")
            continue
        existing.write_text(
            json.dumps(dict(sorted(merged.items())), ensure_ascii=False, indent=2)
            + "\n",
            encoding="utf-8",
        )
        print(f"wrote {existing}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--from",
        dest="src",
        type=Path,
        default=REPO_ROOT / "content/i18n/weblate",
    )
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    import_tree(args.src, dry_run=args.dry_run)


if __name__ == "__main__":
    main()

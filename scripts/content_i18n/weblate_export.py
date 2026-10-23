#!/usr/bin/env python3
"""Export content locale catalogs to a Weblate-compatible directory layout.

Layout (JSON bilingual component style):
  content/i18n/weblate/<component>/<locale>.json

Components:
  exercises — keys under exercise.*
  templates — keys under template.*
  glossary  — term_id -> en-GB / translations as separate files

Also writes a weblate-friendly monolingual base (en-GB) plus per-locale files.
"""
from __future__ import annotations

import argparse
import csv
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from content_i18n.config import (  # noqa: E402
    REPO_ROOT,
    load_locales_config,
    required_locales,
    source_locale,
)


def _load_locale(path: Path) -> dict[str, str]:
    return json.loads(path.read_text(encoding="utf-8"))


def export(out_dir: Path) -> None:
    cfg = load_locales_config()
    paths = cfg.get("paths") or {}
    locales_dir = REPO_ROOT / paths.get("content_locales", "content/exercises/locales")
    glossary_path = REPO_ROOT / paths.get("glossary", "content/i18n/glossary/glossary.csv")
    src = source_locale(cfg)

    out_dir.mkdir(parents=True, exist_ok=True)
    for loc in required_locales(cfg):
        path = locales_dir / f"{loc}.json"
        if not path.is_file():
            continue
        data = _load_locale(path)
        exercises = {k: v for k, v in data.items() if k.startswith("exercise.")}
        templates = {k: v for k, v in data.items() if k.startswith("template.")}
        for component, subset in ("exercises", exercises), ("templates", templates):
            dest = out_dir / component
            dest.mkdir(parents=True, exist_ok=True)
            (dest / f"{loc}.json").write_text(
                json.dumps(subset, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
                encoding="utf-8",
            )

    # Glossary as JSON map term_id -> string per locale
    if glossary_path.is_file():
        with glossary_path.open(encoding="utf-8", newline="") as fh:
            rows = list(csv.DictReader(fh))
        gdir = out_dir / "glossary"
        gdir.mkdir(parents=True, exist_ok=True)
        for loc in required_locales(cfg):
            col = loc
            mapping = {
                (r.get("term_id") or ""): (r.get(col) or "")
                for r in rows
                if r.get("term_id")
            }
            (gdir / f"{loc}.json").write_text(
                json.dumps(mapping, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
                encoding="utf-8",
            )

    meta = {
        "source_language": src,
        "file_format": "json-flat",
        "required_locales": required_locales(cfg),
        "components": ["exercises", "templates", "glossary"],
    }
    (out_dir / "weblate-project.json").write_text(
        json.dumps(meta, indent=2) + "\n", encoding="utf-8"
    )
    print(f"exported Weblate tree -> {out_dir}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--out",
        type=Path,
        default=REPO_ROOT / "content/i18n/weblate",
    )
    args = parser.parse_args()
    export(args.out)


if __name__ == "__main__":
    main()

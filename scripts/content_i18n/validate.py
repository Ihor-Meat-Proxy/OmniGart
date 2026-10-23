#!/usr/bin/env python3
"""Validate OmniGart content + app UI translations.

Exit codes:
  0 — pass (warnings may be printed / summarised)
  1 — failures (missing keys, empty strings, ICU errors, ru locale, etc.)
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import os
import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Iterable

# Allow `python scripts/content_i18n/validate.py` without PYTHONPATH hacks.
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from content_i18n.config import (  # noqa: E402
    REPO_ROOT,
    load_locales_config,
    plural_categories,
    required_locales,
    source_locale,
    target_locales,
)
from content_i18n.icu_util import validate_icu  # noqa: E402
from content_i18n.catalogs import catalog_id, discover_locale_dirs  # noqa: E402

RU_FILE_RE = re.compile(
    r"(^|/)(ru|ru-[A-Za-z0-9]+)(\.json|\.xml|\.strings|\.tsv)?$",
    re.IGNORECASE,
)
RU_DIR_RE = re.compile(
    r"(^|/)(values-ru|values-ru-[^/]+|values-b\+ru[^/]*|ru\.lproj|ru-[^/]+\.lproj|"
    r"i18n/ru|i18n/ru-[^/]+|locales/ru\.json|locales/ru-[^.]+\.json)(/|$)",
    re.IGNORECASE,
)
PLACEHOLDER_TOKEN = re.compile(r"\{[^{}]+\}")


@dataclass
class Finding:
    severity: str  # error | warning | info
    code: str
    message: str
    path: str | None = None
    line: int | None = None
    locale: str | None = None
    key: str | None = None


@dataclass
class Report:
    findings: list[Finding] = field(default_factory=list)
    coverage: dict[str, dict[str, int]] = field(default_factory=dict)
    status_counts: dict[str, dict[str, int]] = field(default_factory=dict)

    def add(self, finding: Finding) -> None:
        self.findings.append(finding)

    @property
    def errors(self) -> list[Finding]:
        return [f for f in self.findings if f.severity == "error"]

    @property
    def warnings(self) -> list[Finding]:
        return [f for f in self.findings if f.severity == "warning"]


def _sha(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()[:16]


def load_flat_json(path: Path) -> dict[str, str]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError(f"expected flat object in {path}")
    out: dict[str, str] = {}
    for k, v in data.items():
        if not isinstance(k, str):
            raise ValueError(f"non-string key in {path}")
        if v is None:
            out[k] = ""
        elif isinstance(v, str):
            out[k] = v
        else:
            raise ValueError(f"non-string value for {k} in {path}")
    return out


def load_status(status_dir: Path, locale: str) -> dict[str, dict[str, Any]]:
    path = status_dir / f"{locale}.json"
    if not path.is_file():
        return {}
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError(f"status file must be object: {path}")
    return data


def load_glossary(path: Path) -> list[dict[str, str]]:
    if not path.is_file():
        return []
    with path.open(encoding="utf-8", newline="") as fh:
        reader = csv.DictReader(fh)
        return list(reader)


def annotate(finding: Finding) -> None:
    """Emit GitHub workflow command annotations when running in Actions."""
    if os.environ.get("GITHUB_ACTIONS") != "true":
        return
    sev = "error" if finding.severity == "error" else "warning"
    parts = [f"::{sev}"]
    meta = []
    if finding.path:
        meta.append(f"file={finding.path}")
    if finding.line:
        meta.append(f"line={finding.line}")
    meta.append(f"title={finding.code}")
    parts.append(" ".join(meta))
    parts.append(f"::{finding.message}")
    print("".join(parts))


def find_key_line(path: Path, key: str) -> int | None:
    if not path.is_file():
        return None
    needle = json.dumps(key)
    for i, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if needle in line:
            return i
    return None


def check_no_ru(root: Path, report: Report) -> None:
    prune = {".git", ".gradle", "build", "node_modules", ".android-sdk", ".idea"}
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in prune and not d.startswith(".")]
        rel_dir = str(Path(dirpath).relative_to(root)).replace("\\", "/")
        full = rel_dir if rel_dir != "." else ""
        probe = f"/{full}/" if full else "/"
        if RU_DIR_RE.search(probe):
            report.add(
                Finding(
                    "error",
                    "ru-locale",
                    f"Forbidden Russian locale path: {full or '.'}",
                    path=full or str(root),
                )
            )
        for name in filenames:
            rel = f"{full}/{name}" if full else name
            if RU_FILE_RE.search(rel.replace("\\", "/")):
                report.add(
                    Finding(
                        "error",
                        "ru-locale",
                        f"Forbidden Russian locale file: {rel}",
                        path=rel,
                    )
                )
            # JSON / xcstrings containing "ru" locale keys
            if name.endswith((".json", ".xcstrings", ".yml", ".yaml")):
                try:
                    text = (root / rel).read_text(encoding="utf-8")
                except OSError:
                    continue
                if re.search(r'"ru"\s*:', text) or re.search(
                    r'"ru-[A-Za-z]+"\s*:', text
                ):
                    # Allow mentions in docs/config forbidden lists
                    if rel.replace("\\", "/").startswith("i18n/"):
                        continue
                    if "forbidden" in text and "ru" in text:
                        continue
                    report.add(
                        Finding(
                            "error",
                            "ru-locale",
                            f"Forbidden ru locale key in {rel}",
                            path=rel,
                        )
                    )



def check_one_content_catalog(
    locales_dir: Path,
    cfg: dict[str, Any],
    report: Report,
    glossary_by_en: dict[str, dict[str, str]],
    status_dir: Path,
) -> None:
    """Validate a single content/*/locales catalog against required locales."""
    name_warn = int(cfg.get("name_length_warn") or 48)
    src = source_locale(cfg)
    required = required_locales(cfg)
    cid = catalog_id(locales_dir)

    catalogs: dict[str, dict[str, str]] = {}
    for loc in required:
        path = locales_dir / f"{loc}.json"
        if not path.is_file():
            report.add(
                Finding(
                    "error",
                    "missing-locale-file",
                    f"[{cid}] Missing content locale file for {loc}",
                    path=str(path.relative_to(REPO_ROOT)),
                    locale=loc,
                )
            )
            catalogs[loc] = {}
            continue
        try:
            catalogs[loc] = load_flat_json(path)
        except (ValueError, json.JSONDecodeError) as exc:
            report.add(
                Finding(
                    "error",
                    "malformed-locale",
                    f"[{cid}] Malformed content locale {loc}: {exc}",
                    path=str(path.relative_to(REPO_ROOT)),
                    locale=loc,
                )
            )
            catalogs[loc] = {}

    source_keys = catalogs.get(src) or {}
    if not source_keys:
        report.add(
            Finding(
                "error",
                "empty-source",
                f"[{cid}] Source locale {src} has no keys",
                locale=src,
                path=str((locales_dir / f"{src}.json").relative_to(REPO_ROOT)),
            )
        )
        return

    for loc in required:
        path = locales_dir / f"{loc}.json"
        rel = str(path.relative_to(REPO_ROOT))
        cat = catalogs.get(loc) or {}
        missing = sorted(set(source_keys) - set(cat))
        orphans = sorted(set(cat) - set(source_keys))
        empty = sorted(k for k, v in cat.items() if k in source_keys and not v.strip())

        status: dict[str, Any] = {}
        try:
            status = load_status(status_dir, loc) if loc != src else {}
        except (ValueError, json.JSONDecodeError) as exc:
            report.add(
                Finding(
                    "error",
                    "malformed-status",
                    f"[{cid}] Malformed status for {loc}: {exc}",
                    path=str((status_dir / f"{loc}.json").relative_to(REPO_ROOT)),
                    locale=loc,
                )
            )

        status_counter: dict[str, int] = defaultdict(int)
        stale_count = 0
        reviewed_stale = 0

        for key in missing:
            line = find_key_line(locales_dir / f"{src}.json", key)
            report.add(
                Finding(
                    "error",
                    "missing-key",
                    f"[{cid}/{loc}] missing key: {key}",
                    path=rel,
                    line=line,
                    locale=loc,
                    key=key,
                )
            )
        for key in orphans:
            line = find_key_line(path, key) if path.is_file() else None
            report.add(
                Finding(
                    "error",
                    "orphan-key",
                    f"[{cid}/{loc}] orphan key not in {src}: {key}",
                    path=rel,
                    line=line,
                    locale=loc,
                    key=key,
                )
            )
        for key in empty:
            line = find_key_line(path, key) if path.is_file() else None
            report.add(
                Finding(
                    "error",
                    "empty-string",
                    f"[{cid}/{loc}] empty string: {key}",
                    path=rel,
                    line=line,
                    locale=loc,
                    key=key,
                )
            )

        if loc != src:
            expected_plurals = plural_categories(loc, cfg)
            for key, src_val in source_keys.items():
                if key not in cat or not cat[key].strip():
                    continue
                dst_val = cat[key]
                for issue in validate_icu(src_val, dst_val, loc, expected_plurals):
                    report.add(
                        Finding(
                            "error",
                            "icu",
                            f"[{cid}/{loc}] {key}: {issue.message}",
                            path=rel,
                            line=find_key_line(path, key),
                            locale=loc,
                            key=key,
                        )
                    )

                if key.endswith(".name") and len(dst_val) > name_warn:
                    report.add(
                        Finding(
                            "warning",
                            "name-length",
                            f"[{cid}/{loc}] UI name long ({len(dst_val)}>{name_warn}): {key}",
                            path=rel,
                            line=find_key_line(path, key),
                            locale=loc,
                            key=key,
                        )
                    )

                _check_glossary(
                    report,
                    glossary_by_en,
                    loc,
                    key,
                    src_val,
                    dst_val,
                    rel,
                    path,
                )

                meta = status.get(key) or {}
                st = meta.get("status") or "machine-drafted"
                if st not in {"machine-drafted", "reviewed"}:
                    report.add(
                        Finding(
                            "error",
                            "bad-status",
                            f"[{cid}/{loc}] invalid status '{st}' for {key}",
                            path=str((status_dir / f"{loc}.json").relative_to(REPO_ROOT))
                            if (status_dir / f"{loc}.json").is_file()
                            else rel,
                            locale=loc,
                            key=key,
                        )
                    )
                    st = "machine-drafted"
                status_counter[st] += 1
                src_hash = meta.get("source_hash")
                current = _sha(src_val)
                if src_hash and src_hash != current:
                    stale_count += 1
                    sev = "error" if st == "reviewed" else "warning"
                    report.add(
                        Finding(
                            sev,
                            "stale",
                            f"[{cid}/{loc}] stale translation (source changed): {key}",
                            path=rel,
                            line=find_key_line(path, key),
                            locale=loc,
                            key=key,
                        )
                    )
                    if st == "reviewed":
                        reviewed_stale += 1

        translated = len(source_keys) - len(missing)
        non_empty = translated - len(empty)
        cov_key = f"{cid}:{loc}"
        report.coverage[cov_key] = {
            "catalog": cid,
            "locale": loc,
            "source_keys": len(source_keys),
            "present": translated,
            "non_empty": non_empty,
            "missing": len(missing),
            "orphans": len(orphans),
            "empty": len(empty),
            "stale": stale_count,
            "reviewed_stale": reviewed_stale,
        }
        # Aggregate status counts per locale across catalogs
        agg = report.status_counts.setdefault(loc, {})
        for k, v in status_counter.items():
            agg[k] = agg.get(k, 0) + v


def check_content_locales(
    cfg: dict[str, Any],
    report: Report,
) -> None:
    paths = cfg.get("paths") or {}
    status_dir = REPO_ROOT / paths.get("status", "content/i18n/status")
    glossary_path = REPO_ROOT / paths.get("glossary", "content/i18n/glossary/glossary.csv")
    required = required_locales(cfg)

    glossary_rows = load_glossary(glossary_path)
    glossary_by_en: dict[str, dict[str, str]] = {}
    for row in glossary_rows:
        en = (row.get("en-GB") or "").strip()
        if en:
            glossary_by_en[en.lower()] = row

    if glossary_rows:
        fieldnames = set(glossary_rows[0].keys())
        for loc in required:
            if loc not in fieldnames:
                report.add(
                    Finding(
                        "error",
                        "glossary-column",
                        f"glossary.csv missing column for locale {loc}",
                        path=str(glossary_path.relative_to(REPO_ROOT)),
                        locale=loc,
                    )
                )
            else:
                blanks = [
                    r.get("term_id", "?")
                    for r in glossary_rows
                    if not (r.get(loc) or "").strip()
                ]
                if blanks:
                    report.add(
                        Finding(
                            "error",
                            "glossary-empty",
                            f"glossary.csv has {len(blanks)} empty {loc} term(s) "
                            f"(e.g. {blanks[0]})",
                            path=str(glossary_path.relative_to(REPO_ROOT)),
                            locale=loc,
                        )
                    )

    dirs = discover_locale_dirs(cfg)
    if not dirs:
        report.add(
            Finding(
                "error",
                "empty-source",
                "No content locale catalogs with en-GB.json were found under content/",
            )
        )
        return

    for locales_dir in dirs:
        check_one_content_catalog(
            locales_dir, cfg, report, glossary_by_en, status_dir
        )



# Overly generic en stems that create noisy false positives in free prose.
_GLOSSARY_SKIP_EN = {
    "pull",
    "push",
    "core",
    "set",
    "sets",
    "rep",
    "reps",
    "run",
    "walk",
    "hold",
    "press",
    "row",
    "drill",
}


def _glossary_alternatives(cell: str) -> list[str]:
    """Split glossary cells that list alternatives with / or ;."""
    parts: list[str] = []
    for chunk in re.split(r"[/;|]", cell):
        part = chunk.strip()
        if part:
            parts.append(part)
    return parts or ([cell.strip()] if cell.strip() else [])


def _check_glossary(
    report: Report,
    glossary_by_en: dict[str, dict[str, str]],
    loc: str,
    key: str,
    src_val: str,
    dst_val: str,
    rel: str,
    path: Path,
) -> None:
    if not glossary_by_en:
        return
    terms = sorted(glossary_by_en.keys(), key=len, reverse=True)
    dst_lower = dst_val.lower()
    for term in terms:
        if len(term) < 5 or term.lower() in _GLOSSARY_SKIP_EN:
            continue
        pattern = re.compile(rf"(?i)(?<![A-Za-z]){re.escape(term)}(?![A-Za-z])")
        if not pattern.search(src_val):
            continue
        row = glossary_by_en[term]
        expected_cell = (row.get(loc) or "").strip()
        if not expected_cell:
            continue
        alts = _glossary_alternatives(expected_cell)
        # Pass if any alternative appears, or English term is an accepted loan
        if any(alt.lower() in dst_lower for alt in alts):
            continue
        if pattern.search(dst_val) and all(alt.lower() != term.lower() for alt in alts):
            # English still present and glossary wants a different form
            report.add(
                Finding(
                    "warning",
                    "glossary",
                    f"[{loc}] {key}: source uses glossary term '{row.get('en-GB')}' "
                    f"but translation may lack '{expected_cell}'",
                    path=rel,
                    line=find_key_line(path, key),
                    locale=loc,
                    key=key,
                )
            )


def _iter_app_locale_dirs(base: Path) -> list[tuple[str, Path]]:
    """Return (locale, strings.json path) under shared i18n tree."""
    found: list[tuple[str, Path]] = []
    if not base.is_dir():
        return found
    for child in sorted(base.iterdir()):
        if child.is_dir():
            candidate = child / "strings.json"
            if candidate.is_file():
                found.append((child.name, candidate))
    return found


def _android_locale_from_values_dir(name: str) -> str | None:
    if name == "values":
        return "default"
    if name.startswith("values-"):
        tag = name[len("values-") :]
        # en-rGB -> en-GB
        tag = tag.replace("-r", "-")
        return tag
    return None


def check_app_ui(cfg: dict[str, Any], report: Report) -> None:
    paths = (cfg.get("paths") or {}).get("app_ui") or []
    src = source_locale(cfg)
    required = [loc for loc in required_locales(cfg) if loc != src]
    any_present = False

    for raw in paths:
        path = REPO_ROOT / raw
        if raw.endswith(".xcstrings"):
            if path.is_file():
                any_present = True
                _check_xcstrings(path, cfg, report)
            continue
        if not path.exists():
            report.add(
                Finding(
                    "info",
                    "app-ui-absent",
                    f"App UI path not present yet (skip): {raw}",
                    path=raw,
                )
            )
            continue
        any_present = True
        if path.is_dir() and (
            (path / "en-GB").is_dir()
            or any(p.name == "en-GB" for p in path.iterdir() if p.is_dir())
        ):
            # shared resources/i18n layout
            _check_shared_i18n_tree(path, cfg, report)
        elif path.is_dir() and any(
            d.name.startswith("values") for d in path.iterdir() if d.is_dir()
        ):
            _check_android_values(path, cfg, report)
        else:
            # directory exists but unexpected shape
            report.add(
                Finding(
                    "error",
                    "app-ui-malformed",
                    f"App UI path exists but layout is unrecognized/malformed: {raw}",
                    path=raw,
                )
            )

    if not any_present:
        report.add(
            Finding(
                "info",
                "app-ui-skipped",
                "No app UI string catalogs found; content-only validation this run",
            )
        )


def _check_shared_i18n_tree(base: Path, cfg: dict[str, Any], report: Report) -> None:
    src = source_locale(cfg)
    required = required_locales(cfg)
    catalogs: dict[str, dict[str, str]] = {}
    for loc, path in _iter_app_locale_dirs(base):
        try:
            catalogs[loc] = load_flat_json(path)
        except (ValueError, json.JSONDecodeError) as exc:
            report.add(
                Finding(
                    "error",
                    "app-ui-malformed",
                    f"Malformed app UI strings for {loc}: {exc}",
                    path=str(path.relative_to(REPO_ROOT)),
                    locale=loc,
                )
            )
            catalogs[loc] = {}

    if src not in catalogs:
        report.add(
            Finding(
                "error",
                "app-ui-malformed",
                f"App UI tree at {base.relative_to(REPO_ROOT)} missing source {src}",
                path=str(base.relative_to(REPO_ROOT)),
            )
        )
        return

    source_keys = catalogs[src]
    for loc in required:
        if loc == src:
            continue
        rel_path = base / loc / "strings.json"
        rel = str(rel_path.relative_to(REPO_ROOT))
        if loc not in catalogs:
            report.add(
                Finding(
                    "error",
                    "missing-key",
                    f"[app-ui/{loc}] missing locale directory/strings.json",
                    path=str(base.relative_to(REPO_ROOT)),
                    locale=loc,
                )
            )
            continue
        cat = catalogs[loc]
        for key in sorted(set(source_keys) - set(cat)):
            report.add(
                Finding(
                    "error",
                    "missing-key",
                    f"[app-ui/{loc}] missing key: {key}",
                    path=rel,
                    locale=loc,
                    key=key,
                )
            )
        for key in sorted(set(cat) - set(source_keys)):
            report.add(
                Finding(
                    "error",
                    "orphan-key",
                    f"[app-ui/{loc}] orphan key: {key}",
                    path=rel,
                    locale=loc,
                    key=key,
                )
            )
        for key, val in cat.items():
            if key in source_keys and not str(val).strip():
                report.add(
                    Finding(
                        "error",
                        "empty-string",
                        f"[app-ui/{loc}] empty string: {key}",
                        path=rel,
                        locale=loc,
                        key=key,
                    )
                )
            if key in source_keys:
                for issue in validate_icu(
                    source_keys[key],
                    val,
                    loc,
                    plural_categories(loc, cfg),
                ):
                    report.add(
                        Finding(
                            "error",
                            "icu",
                            f"[app-ui/{loc}] {key}: {issue.message}",
                            path=rel,
                            locale=loc,
                            key=key,
                        )
                    )


def _check_android_values(res_dir: Path, cfg: dict[str, Any], report: Report) -> None:
    """Validate Android values-*/strings.xml when present (best-effort key parity)."""
    by_locale: dict[str, dict[str, str]] = {}
    for child in sorted(res_dir.iterdir()):
        if not child.is_dir():
            continue
        loc = _android_locale_from_values_dir(child.name)
        if loc is None:
            continue
        strings = child / "strings.xml"
        if not strings.is_file():
            continue
        try:
            by_locale[loc] = _parse_android_strings(strings)
        except ET.ParseError as exc:
            report.add(
                Finding(
                    "error",
                    "app-ui-malformed",
                    f"Malformed Android strings.xml: {exc}",
                    path=str(strings.relative_to(REPO_ROOT)),
                    locale=loc,
                )
            )

    # Map en-GB
    src_keys = by_locale.get("en-GB") or by_locale.get("default") or {}
    if not src_keys:
        report.add(
            Finding(
                "warning",
                "app-ui-absent",
                f"Android res at {res_dir.relative_to(REPO_ROOT)} has no en-GB/default strings",
                path=str(res_dir.relative_to(REPO_ROOT)),
            )
        )
        return

    # Only enforce locales that already have a values-* folder; missing folders for
    # new required locales are errors when *any* localized values-* exists beyond default.
    localized = {k for k in by_locale if k not in {"default", "en", "en-GB"}}
    if not localized:
        return
    required = required_locales(cfg)
    for loc in required:
        if loc in ("en-GB",):
            continue
        # Android folder naming: uk -> values-uk, zh-Hans -> values-zh-rCN (optional)
        android_aliases = {
            "zh-Hans": ["zh-CN", "zh-Hans", "b+zh+Hans"],
            "en-GB": ["en-GB", "en-rGB"],
        }
        aliases = android_aliases.get(loc, [loc])
        present = None
        for a in aliases:
            if a in by_locale:
                present = by_locale[a]
                break
        if present is None:
            report.add(
                Finding(
                    "error",
                    "missing-key",
                    f"[android/{res_dir.name}] missing values folder for locale {loc}",
                    path=str(res_dir.relative_to(REPO_ROOT)),
                    locale=loc,
                )
            )
            continue
        for key in sorted(set(src_keys) - set(present)):
            report.add(
                Finding(
                    "error",
                    "missing-key",
                    f"[android/{loc}] missing key: {key}",
                    path=str(res_dir.relative_to(REPO_ROOT)),
                    locale=loc,
                    key=key,
                )
            )


def _parse_android_strings(path: Path) -> dict[str, str]:
    tree = ET.parse(path)
    root = tree.getroot()
    out: dict[str, str] = {}
    for el in root.findall("string"):
        name = el.attrib.get("name")
        if name:
            out[name] = "".join(el.itertext())
    return out


def _check_xcstrings(path: Path, cfg: dict[str, Any], report: Report) -> None:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        report.add(
            Finding(
                "error",
                "app-ui-malformed",
                f"Malformed Localizable.xcstrings: {exc}",
                path=str(path.relative_to(REPO_ROOT)),
            )
        )
        return
    strings = data.get("strings") or {}
    src = source_locale(cfg)
    required = required_locales(cfg)
    # Collect per-locale keys
    by_locale: dict[str, set[str]] = defaultdict(set)
    for key, entry in strings.items():
        localizations = (entry or {}).get("localizations") or {}
        for loc in localizations:
            by_locale[loc].add(key)
        # source language may be implicit
        by_locale[src].add(key)
    source_keys = by_locale.get(src) or set(strings.keys())
    for loc in required:
        if loc == src:
            continue
        # xcstrings often uses "uk" / "pl" ; zh-Hans may be "zh-Hans"
        have = by_locale.get(loc) or by_locale.get(loc.replace("-", "_")) or set()
        missing = sorted(source_keys - have)
        for key in missing:
            report.add(
                Finding(
                    "error",
                    "missing-key",
                    f"[ios/{loc}] missing key: {key}",
                    path=str(path.relative_to(REPO_ROOT)),
                    locale=loc,
                    key=key,
                )
            )


def write_summary(report: Report, cfg: dict[str, Any]) -> None:
    lines = [
        "# Translations",
        "",
        f"Source locale: `{source_locale(cfg)}`",
        f"Required: {', '.join(f'`{l}`' for l in required_locales(cfg))}",
        "",
        "## Coverage (content catalogs)",
        "",
        "| Catalog | Locale | Source keys | Present | Non-empty | Missing | Orphans | Empty | Stale |",
        "| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |",
    ]
    for cov_key in sorted(report.coverage.keys()):
        c = report.coverage[cov_key]
        if "catalog" not in c:
            continue
        lines.append(
            f"| `{c.get('catalog')}` | `{c.get('locale')}` | {c.get('source_keys', 0)} | "
            f"{c.get('present', 0)} | {c.get('non_empty', 0)} | {c.get('missing', 0)} | "
            f"{c.get('orphans', 0)} | {c.get('empty', 0)} | {c.get('stale', 0)} |"
        )

    lines += ["", "## Review status", ""]
    lines.append("| Locale | machine-drafted | reviewed |")
    lines.append("| --- | ---: | ---: |")
    for loc in target_locales(cfg):
        sc = report.status_counts.get(loc) or {}
        lines.append(
            f"| `{loc}` | {sc.get('machine-drafted', 0)} | {sc.get('reviewed', 0)} |"
        )

    err_n = len(report.errors)
    warn_n = len(report.warnings)
    lines += [
        "",
        f"## Result: {'FAIL' if err_n else 'PASS'} "
        f"({err_n} errors, {warn_n} warnings)",
        "",
    ]
    if report.errors:
        lines.append("### Errors (first 50)")
        for f in report.errors[:50]:
            loc = f.locale or ""
            lines.append(f"- `{f.code}` {loc}: {f.message}")
    if report.warnings:
        lines.append("")
        lines.append("### Warnings (first 30)")
        for f in report.warnings[:30]:
            lines.append(f"- `{f.code}`: {f.message}")

    text = "\n".join(lines) + "\n"
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as fh:
            fh.write(text)
    print(text)


def run(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--config",
        type=Path,
        default=None,
        help="Path to i18n/locales.yml",
    )
    args = parser.parse_args(argv)
    cfg = load_locales_config(args.config)
    report = Report()

    check_no_ru(REPO_ROOT, report)
    check_content_locales(cfg, report)
    check_app_ui(cfg, report)

    for finding in report.findings:
        if finding.severity in {"error", "warning"}:
            annotate(finding)

    write_summary(report, cfg)

    if report.errors:
        print(f"FAILED with {len(report.errors)} error(s)", file=sys.stderr)
        return 1
    print("OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(run())

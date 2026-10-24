#!/usr/bin/env python3
"""Build the offline content bundle from repo sources into generated/content-bundle/.

Reproducible: same inputs → same manifest digests (SHA-256 of file bytes; mtimes ignored).
Used by Gradle (:packBundledContent / :verifyBundledContent) and the Xcode pre-build script.

Sections (bundled baseline — apps work fully offline):
  exercises, templates, translations (locales), evidence, icons, body-maps,
  illustrations, achievements, exercise-images

Server later may ship signed update packs that overlay this baseline after
signature + schema checks (see shared content loader).
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "generated" / "content-bundle"
MANIFEST_NAME = "manifest.json"
SCHEMA_VERSION = 1

# (section_name, list of (src_relative_to_repo, dest_relative_under_section_or_"" ))
# dest "" means copy tree contents into section root.
SECTION_SOURCES: list[tuple[str, list[tuple[str, str]]]] = [
    (
        "exercises",
        [
            ("content/exercises/data", "data"),
            ("content/exercises/templates", "templates"),
            ("content/exercises/locales", "locales"),
            ("content/exercises/schema", "schema"),
            ("content/exercises/equipment-presets.json", "equipment-presets.json"),
        ],
    ),
    (
        "evidence",
        [
            ("docs/evidence/citations.json", "citations.json"),
            ("docs/evidence/citation.schema.json", "citation.schema.json"),
        ],
    ),
    (
        "icons",
        [
            ("design/icons/svg", "svg"),
            ("design/icons/manifest.json", "manifest.json"),
        ],
    ),
    (
        "body-maps",
        [
            ("design/body-map/generated/paths", "paths"),
        ],
    ),
    (
        "illustrations",
        [
            ("design/illustrations/index.json", "index.json"),
            ("design/illustrations/equipment", "equipment"),
            ("design/illustrations/exercises", "exercises"),
        ],
    ),
    (
        "achievements",
        [
            # Stub until achievement content lands; keep directory present for loader.
            ("content/achievements", ""),
        ],
    ),
    (
        "exercise-images",
        [
            ("content/exercise-images", ""),
        ],
    ),
]


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def copy_path(src: Path, dest: Path) -> None:
    if src.is_dir():
        if dest.exists():
            shutil.rmtree(dest)
        shutil.copytree(src, dest)
    elif src.is_file():
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dest)
    else:
        raise FileNotFoundError(f"Missing content source: {src}")


def ensure_stub_dirs() -> None:
    """Create minimal source stubs so packing never depends on untracked empties."""
    achievements = ROOT / "content" / "achievements"
    images = ROOT / "content" / "exercise-images"
    achievements.mkdir(parents=True, exist_ok=True)
    images.mkdir(parents=True, exist_ok=True)
    pack = achievements / "pack.json"
    if not pack.exists():
        pack.write_text(
            json.dumps(
                {
                    "id": "achievements",
                    "version": 1,
                    "locale": "en-GB",
                    "items": [],
                },
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )
    readme = images / "README.md"
    if not readme.exists():
        readme.write_text(
            "# Exercise images\n\nOffline stills bundled with the app. Videos stay on the "
            "optional media server.\n",
            encoding="utf-8",
        )


def collect_files(section_dir: Path) -> list[Path]:
    files: list[Path] = []
    for path in sorted(section_dir.rglob("*")):
        if path.is_file():
            files.append(path)
    return files


def build_section(name: str, mappings: list[tuple[str, str]], out_root: Path) -> dict:
    section_dir = out_root / name
    if section_dir.exists():
        shutil.rmtree(section_dir)
    section_dir.mkdir(parents=True, exist_ok=True)

    for src_rel, dest_rel in mappings:
        src = ROOT / src_rel
        if not src.exists():
            # Optional equipment-presets etc.
            if src_rel.endswith("equipment-presets.json"):
                continue
            if name in {"achievements", "exercise-images"}:
                continue
            raise FileNotFoundError(f"Required source missing for section {name}: {src_rel}")
        dest = section_dir if dest_rel == "" else section_dir / dest_rel
        if src.is_dir() and dest_rel == "":
            # Copy children into section root
            for child in sorted(src.iterdir()):
                copy_path(child, section_dir / child.name)
        else:
            copy_path(src, dest)

    files = collect_files(section_dir)
    digest = hashlib.sha256()
    entries: list[dict] = []
    for path in files:
        rel = path.relative_to(section_dir).as_posix()
        file_hash = sha256_file(path)
        digest.update(rel.encode("utf-8"))
        digest.update(b"\0")
        digest.update(bytes.fromhex(file_hash))
        entries.append({"path": rel, "sha256": file_hash, "bytes": path.stat().st_size})

    return {
        "name": name,
        "fileCount": len(files),
        "sha256": digest.hexdigest(),
        "files": entries,
    }


def build_manifest(sections: list[dict]) -> dict:
    pack_digest = hashlib.sha256()
    for section in sections:
        pack_digest.update(section["name"].encode("utf-8"))
        pack_digest.update(b"\0")
        pack_digest.update(bytes.fromhex(section["sha256"]))
    return {
        "schemaVersion": SCHEMA_VERSION,
        "packId": "omnigart-baseline",
        "packVersion": 1,
        "localePolicy": {"source": "en-GB", "included": ["en-GB", "uk"], "forbidden": ["ru"]},
        "offline": True,
        "sha256": pack_digest.hexdigest(),
        "sections": {
            s["name"]: {
                "fileCount": s["fileCount"],
                "sha256": s["sha256"],
            }
            for s in sections
        },
        # Full file index kept beside manifest for verify tools; loader uses sections only.
        "fileIndex": {s["name"]: s["files"] for s in sections},
    }


def write_bundle(out_root: Path) -> dict:
    if out_root.exists():
        shutil.rmtree(out_root)
    out_root.mkdir(parents=True, exist_ok=True)

    ensure_stub_dirs()
    sections: list[dict] = []
    for name, mappings in SECTION_SOURCES:
        sections.append(build_section(name, mappings, out_root))

    manifest = build_manifest(sections)
    # Write manifest without fileIndex first for stable outer digest of shipped bytes?
    # Keep fileIndex in manifest.json for CI verify of individual files.
    manifest_path = out_root / MANIFEST_NAME
    payload = json.dumps(manifest, indent=2, sort_keys=True) + "\n"
    manifest_path.write_text(payload, encoding="utf-8")
    return manifest


def verify_bundle(out_root: Path) -> None:
    """Regenerate into a temp dir and compare digests to the committed bundle."""
    if not (out_root / MANIFEST_NAME).is_file():
        raise SystemExit(f"Missing committed bundle at {out_root}")

    committed = json.loads((out_root / MANIFEST_NAME).read_text(encoding="utf-8"))
    tmp = ROOT / "generated" / ".content-bundle-verify"
    try:
        fresh = write_bundle(tmp)
    finally:
        pass

    if fresh["sha256"] != committed["sha256"]:
        print("ERROR: content bundle is stale.", file=sys.stderr)
        print(f"  committed: {committed['sha256']}", file=sys.stderr)
        print(f"  regenerated: {fresh['sha256']}", file=sys.stderr)
        print("Run: python3 scripts/pack_bundled_content.py", file=sys.stderr)
        shutil.rmtree(tmp, ignore_errors=True)
        raise SystemExit(1)

    # Spot-check section digests
    for name, meta in committed.get("sections", {}).items():
        if fresh["sections"].get(name, {}).get("sha256") != meta.get("sha256"):
            print(f"ERROR: section {name} digest mismatch", file=sys.stderr)
            shutil.rmtree(tmp, ignore_errors=True)
            raise SystemExit(1)

    shutil.rmtree(tmp, ignore_errors=True)
    print(f"OK: content bundle up to date ({committed['sha256'][:12]}…)")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--verify",
        action="store_true",
        help="Fail if generated/content-bundle does not match a fresh pack",
    )
    parser.add_argument(
        "--out",
        type=Path,
        default=OUT,
        help="Output directory (default: generated/content-bundle)",
    )
    args = parser.parse_args()

    if args.verify:
        verify_bundle(args.out)
        return 0

    manifest = write_bundle(args.out)
    out = args.out.resolve()
    try:
        out_display = out.relative_to(ROOT.resolve())
    except ValueError:
        # Xcode may pass a temp --out outside the repo (e.g. $TMPDIR).
        out_display = out
    print(
        f"Wrote {out_display} "
        f"schema={manifest['schemaVersion']} packVersion={manifest['packVersion']} "
        f"sha256={manifest['sha256']}"
    )
    for name, meta in manifest["sections"].items():
        print(f"  - {name}: {meta['fileCount']} files")
    return 0


if __name__ == "__main__":
    # Deterministic timestamps for copy2 are fine; digests ignore mtimes.
    os.environ.setdefault("TZ", "UTC")
    sys.exit(main())

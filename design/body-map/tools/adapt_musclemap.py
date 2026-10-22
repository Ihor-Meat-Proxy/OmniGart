#!/usr/bin/env python3
"""
Adapt MIT-licensed MuscleMap path data into OmniGart body-map SVGs.

- Source: design/body-map/vendor/musclemap/*.json (@musclemap/assets 1.1.0, MIT)
- Output: design/body-map/svg/{male,female}-{front,back}.svg (+ themed variants)
- Preserves OmniGart stable region ids; subdivides coarse MuscleMap surfaces where needed.

Not traced from copyrighted atlases — adapted from original MuscleMap Inkscape traces.
"""

from __future__ import annotations

import json
import math
import re
from pathlib import Path

from shapely.geometry import LineString, MultiPolygon, Polygon, box
from shapely.ops import unary_union
from svgpathtools import parse_path

ROOT = Path(__file__).resolve().parents[1]
VENDOR = ROOT / "vendor" / "musclemap"
SVG_DIR = ROOT / "svg"
VIEW_W, VIEW_H = 1024, 1536
CX_DEFAULT = 512.0

REQUIRED_IDS = {
    "adductors_l",
    "adductors_r",
    "biceps_l",
    "biceps_r",
    "brachialis_l",
    "brachialis_r",
    "erector_spinae_l",
    "erector_spinae_r",
    "forearm_extensors_l",
    "forearm_extensors_r",
    "forearm_flexors_l",
    "forearm_flexors_r",
    "front_deltoid_l",
    "front_deltoid_r",
    "gastrocnemius_front_l",
    "gastrocnemius_front_r",
    "gastrocnemius_l",
    "gastrocnemius_r",
    "gluteus_maximus_l",
    "gluteus_maximus_r",
    "gluteus_medius_l",
    "gluteus_medius_r",
    "hamstrings_l",
    "hamstrings_r",
    "hip_flexors_l",
    "hip_flexors_r",
    "lateral_deltoid_back_l",
    "lateral_deltoid_back_r",
    "lateral_deltoid_l",
    "lateral_deltoid_r",
    "latissimus_dorsi_l",
    "latissimus_dorsi_r",
    "neck_back",
    "neck_front",
    "obliques_l",
    "obliques_r",
    "pectoralis_major_clavicular_l",
    "pectoralis_major_clavicular_r",
    "pectoralis_major_sternal_l",
    "pectoralis_major_sternal_r",
    "rear_deltoid_l",
    "rear_deltoid_r",
    "rectus_abdominis",
    "rectus_femoris_l",
    "rectus_femoris_r",
    "rhomboids_l",
    "rhomboids_r",
    "rotator_cuff_l",
    "rotator_cuff_r",
    "serratus_anterior_l",
    "serratus_anterior_r",
    "soleus_l",
    "soleus_r",
    "tibialis_anterior_l",
    "tibialis_anterior_r",
    "trapezius_middle",
    "trapezius_upper_l",
    "trapezius_upper_r",
    "triceps_l",
    "triceps_r",
    "vastus_lateralis_l",
    "vastus_lateralis_r",
    "vastus_medialis_l",
    "vastus_medialis_r",
}


def path_to_polygon(d: str, samples: int = 24) -> Polygon | MultiPolygon | None:
    try:
        path = parse_path(d)
    except Exception:
        return None
    if len(path) == 0:
        return None
    pts: list[tuple[float, float]] = []
    for seg in path:
        n = max(4, samples)
        for i in range(n):
            p = seg.point(i / n)
            pts.append((float(p.real), float(p.imag)))
    # close
    if pts and pts[0] != pts[-1]:
        pts.append(pts[0])
    if len(pts) < 4:
        return None
    poly = Polygon(pts)
    if not poly.is_valid:
        poly = poly.buffer(0)
    if poly.is_empty:
        return None
    return poly


def geom_to_d(geom, simplify_tol: float = 0.25) -> str:
    """Convert Shapely polygon(s) to an SVG path (dense polyline approximating curves)."""
    if geom is None or geom.is_empty:
        return ""
    if isinstance(geom, MultiPolygon):
        parts = [geom_to_d(g, simplify_tol) for g in geom.geoms if not g.is_empty]
        return " ".join(p for p in parts if p)
    if not isinstance(geom, Polygon):
        geom = geom.buffer(0)
        if isinstance(geom, MultiPolygon):
            return geom_to_d(geom, simplify_tol)
        if not isinstance(geom, Polygon) or geom.is_empty:
            return ""
    if simplify_tol > 0:
        simplified = geom.simplify(simplify_tol, preserve_topology=True)
        if isinstance(simplified, Polygon) and not simplified.is_empty:
            geom = simplified
    coords = list(geom.exterior.coords)
    if len(coords) < 3:
        return ""
    parts = [f"M {coords[0][0]:.2f} {coords[0][1]:.2f}"]
    for x, y in coords[1:]:
        parts.append(f"L {x:.2f} {y:.2f}")
    parts.append("Z")
    return " ".join(parts)


def multi_d(*ds: str) -> str:
    """Concatenate original SVG subpaths (preserves MuscleMap Bézier fidelity)."""
    return " ".join(d.strip() for d in ds if d and d.strip())


def curved_h_mask(poly: Polygon, frac_from_top: float, bulge: float = 0.06) -> Polygon:
    """Soft curved horizontal divider used as a clip mask for the upper portion."""
    minx, miny, maxx, maxy = poly.bounds
    w, h = maxx - minx, maxy - miny
    y = miny + h * frac_from_top
    # Gentle upward bow in the middle (follows pec / glute contours better than a flat cut)
    mid = (minx + maxx) / 2
    bow = h * bulge
    # Upper mask: rectangle above a quadratic-ish polyline
    pts = [
        (minx - 4, miny - 4),
        (maxx + 4, miny - 4),
        (maxx + 4, y + bow * 0.2),
        (mid + w * 0.25, y + bow),
        (mid, y + bow * 1.15),
        (mid - w * 0.25, y + bow),
        (minx - 4, y + bow * 0.2),
    ]
    return Polygon(pts)


def keep_d(d: str) -> str:
    """Prefer original Bézier `d` when we don't need to subdivide."""
    return d


def split_horizontal(poly: Polygon, frac_from_top: float) -> tuple:
    minx, miny, maxx, maxy = poly.bounds
    y = miny + (maxy - miny) * frac_from_top
    top = poly.intersection(box(minx - 2, miny - 2, maxx + 2, y))
    bot = poly.intersection(box(minx - 2, y, maxx + 2, maxy + 2))
    return top, bot


def split_vertical(poly: Polygon, frac_from_left: float) -> tuple:
    minx, miny, maxx, maxy = poly.bounds
    x = minx + (maxx - minx) * frac_from_left
    left = poly.intersection(box(minx - 2, miny - 2, x, maxy + 2))
    right = poly.intersection(box(x, miny - 2, maxx + 2, maxy + 2))
    return left, right


def band(poly: Polygon, y0_frac: float, y1_frac: float):
    minx, miny, maxx, maxy = poly.bounds
    y0 = miny + (maxy - miny) * y0_frac
    y1 = miny + (maxy - miny) * y1_frac
    return poly.intersection(box(minx - 2, y0, maxx + 2, y1))


def side_suffix(center_x: float, poly: Polygon) -> str:
    cx = (poly.bounds[0] + poly.bounds[2]) / 2
    return "l" if cx < center_x else "r"


def collect_by_group(muscles: list[dict]) -> dict[str, list[dict]]:
    out: dict[str, list[dict]] = {}
    for m in muscles:
        g = m.get("group") or "UNKNOWN"
        out.setdefault(g, []).append(m)
    return out


def largest(polys: list) -> Polygon | None:
    polys = [p for p in polys if p is not None and not p.is_empty]
    if not polys:
        return None
    return max(polys, key=lambda p: p.area)


def as_polys(mlist: list[dict]) -> list[tuple[dict, Polygon]]:
    out = []
    for m in mlist:
        p = path_to_polygon(m["d"])
        if p is not None and not p.is_empty:
            if isinstance(p, MultiPolygon):
                p = unary_union(p)
            out.append((m, p))
    return out


def neck_from_outline(outline_d: str, center_x: float) -> str:
    """Organic neck carved from silhouette using per-scanline width (not a rectangle)."""
    body = path_to_polygon(outline_d, samples=18)
    if body is None or body.is_empty:
        return ""
    minx, miny, maxx, maxy = body.bounds
    h = maxy - miny
    y0, y1 = miny + h * 0.095, miny + h * 0.168
    left_pts: list[tuple[float, float]] = []
    right_pts: list[tuple[float, float]] = []
    steps = 16
    for i in range(steps + 1):
        y = y0 + (y1 - y0) * i / steps
        inter = body.intersection(LineString([(minx - 20, y), (maxx + 20, y)]))
        xs: list[float] = []
        if inter.is_empty:
            continue
        if inter.geom_type == "LineString":
            xs = [c[0] for c in inter.coords]
        elif inter.geom_type == "MultiLineString":
            for g in inter.geoms:
                xs.extend(c[0] for c in g.coords)
        elif inter.geom_type == "Point":
            xs = [inter.x]
        if len(xs) < 2:
            continue
        # Prefer the segment nearest the midline (true neck, not arms)
        xs.sort()
        # pick contiguous run that contains center_x when possible
        lo, hi = xs[0], xs[-1]
        for a, b in zip(xs, xs[1:]):
            if a <= center_x <= b and (b - a) < (hi - lo):
                lo, hi = a, b
        # Clamp to a neck-like width around center if the slice is the whole shoulders
        if hi - lo > 140:
            mid = (lo + hi) / 2
            # find local minimum width near center by shrinking toward center_x
            lo = max(lo, center_x - 48)
            hi = min(hi, center_x + 48)
        left_pts.append((lo, y))
        right_pts.append((hi, y))
    if len(left_pts) < 4:
        return ""
    ring = left_pts + list(reversed(right_pts))
    poly = Polygon(ring).buffer(0.8).intersection(body)
    if isinstance(poly, MultiPolygon):
        poly = max(poly.geoms, key=lambda g: g.area)
    return geom_to_d(poly, 0.15)


def build_front(data: dict) -> dict[str, str]:
    cx = float(data.get("centerX") or CX_DEFAULT)
    by = collect_by_group(data["muscles"])
    regions: dict[str, str] = {}

    # Prefer MuscleMap anterior upper-trap paths (organic neck/shoulder junction);
    # fall back to silhouette carving.
    front_traps = [m["d"] for m in by.get("TRAPEZIUS", []) if m.get("d")]
    if front_traps:
        regions["neck_front"] = multi_d(*front_traps)
    else:
        regions["neck_front"] = neck_from_outline(data["outline"][0]["d"], cx)

    # Chest → clavicular / sternal (curved fibre-line split; keep organic edges)
    for m, poly in as_polys(by.get("CHEST", [])):
        side = side_suffix(cx, poly)
        upper_mask = curved_h_mask(poly, 0.40, bulge=0.07)
        top = poly.intersection(upper_mask)
        bot = poly.difference(upper_mask)
        regions[f"pectoralis_major_clavicular_{side}"] = geom_to_d(top, 0.2) or keep_d(m["d"])
        regions[f"pectoralis_major_sternal_{side}"] = geom_to_d(bot, 0.2) or keep_d(m["d"])

    for m, poly in as_polys(by.get("SHOULDERS_FRONT", [])):
        side = side_suffix(cx, poly)
        regions[f"front_deltoid_{side}"] = keep_d(m["d"])

    for m, poly in as_polys(by.get("SHOULDERS_SIDE", [])):
        side = side_suffix(cx, poly)
        regions[f"lateral_deltoid_{side}"] = keep_d(m["d"])

    for m, poly in as_polys(by.get("BICEPS", [])):
        side = side_suffix(cx, poly)
        # Keep full organic biceps path; brachialis is a soft distal cap
        regions[f"biceps_{side}"] = keep_d(m["d"])
        distal = poly.intersection(curved_h_mask(poly, 0.72, bulge=-0.02).envelope)
        # Use lower band with slight curve
        minx, miny, maxx, maxy = poly.bounds
        y = miny + (maxy - miny) * 0.78
        distal = poly.intersection(box(minx - 2, y, maxx + 2, maxy + 2))
        regions[f"brachialis_{side}"] = geom_to_d(distal, 0.15) or keep_d(m["d"])

    for m, poly in as_polys(by.get("FOREARMS", [])):
        side = side_suffix(cx, poly)
        regions[f"forearm_flexors_{side}"] = keep_d(m["d"])

    # Rectus = original CORE subpaths concatenated (keeps 6/8-pack Bézier segments)
    core_ds = [m["d"] for m in by.get("CORE", []) if m.get("d")]
    if core_ds:
        regions["rectus_abdominis"] = multi_d(*core_ds)

    for m, poly in as_polys(by.get("OBLIQUES", [])):
        side = side_suffix(cx, poly)
        regions[f"obliques_{side}"] = keep_d(m["d"])

    # Serratus: thin lateral strip of lower chest / upper oblique
    for m, poly in as_polys(by.get("CHEST", [])):
        side = side_suffix(cx, poly)
        minx, miny, maxx, maxy = poly.bounds
        if side == "r":
            strip = poly.intersection(box(maxx - (maxx - minx) * 0.28, miny + (maxy - miny) * 0.45, maxx + 2, maxy + 2))
        else:
            strip = poly.intersection(box(minx - 2, miny + (maxy - miny) * 0.45, minx + (maxx - minx) * 0.28, maxy + 2))
        d = geom_to_d(strip)
        if d:
            regions[f"serratus_anterior_{side}"] = d

    # Classify thigh medial/lateral by distance to midline (handles female label quirks)
    thigh_medial = []
    thigh_lateral = []
    for g in ("ADDUCTORS", "ABDUCTORS"):
        for m, poly in as_polys(by.get(g, [])):
            pcx = (poly.bounds[0] + poly.bounds[2]) / 2
            if abs(pcx - cx) < 70:
                thigh_medial.append((m, poly))
            else:
                thigh_lateral.append((m, poly))

    for m, poly in thigh_medial:
        side = side_suffix(cx, poly)
        regions[f"adductors_{side}"] = keep_d(m["d"])
        # hip flexors: upper portion of adductors / iliac area
        top, _ = split_horizontal(poly, 0.35)
        # expand upward slightly using a translated band if needed
        hf = top
        if hf is not None and not hf.is_empty:
            regions[f"hip_flexors_{side}"] = geom_to_d(hf)

    # If hip flexors missing, derive from upper quads
    quads = as_polys(by.get("QUADS", []))
    # Separate main thigh quads vs lower VM tear-drops by vertical position
    if quads:
        quads_sorted = sorted(quads, key=lambda mp: mp[1].bounds[1])
        # upper group = rectus/vastus; lower group = VM
        ys = [p.bounds[1] for _, p in quads]
        median_y = sorted(ys)[len(ys) // 2]
        main, vm = [], []
        for m, p in quads:
            if p.bounds[1] < median_y + 40 and (p.bounds[3] - p.bounds[1]) > 150:
                main.append((m, p))
            else:
                vm.append((m, p))
        if not main:
            main = quads_sorted[:2]
        if not vm:
            vm = quads_sorted[2:]

        for m, poly in main:
            side = side_suffix(cx, poly)
            # Keep organic main quad as rectus femoris; VL = soft lateral strip
            regions[f"rectus_femoris_{side}"] = keep_d(m["d"])
            minx, miny, maxx, maxy = poly.bounds
            w = maxx - minx
            if side == "r":
                lat = poly.intersection(box(minx + w * 0.52, miny - 2, maxx + 2, maxy + 2))
            else:
                lat = poly.intersection(box(minx - 2, miny - 2, minx + w * 0.48, maxy + 2))
            regions[f"vastus_lateralis_{side}"] = geom_to_d(lat, 0.2) or keep_d(m["d"])
            if f"hip_flexors_{side}" not in regions:
                top = poly.intersection(curved_h_mask(poly, 0.20, bulge=0.04))
                regions[f"hip_flexors_{side}"] = geom_to_d(top, 0.2)

        for m, poly in vm:
            side = side_suffix(cx, poly)
            regions[f"vastus_medialis_{side}"] = keep_d(m["d"])

        # Ensure VM exists: take distal medial of main if missing
        for side in ("l", "r"):
            if f"vastus_medialis_{side}" not in regions:
                cand = [p for m, p in main if side_suffix(cx, p) == side]
                if cand:
                    poly = cand[0]
                    _, bot = split_horizontal(poly, 0.72)
                    if side == "r":
                        med, _ = split_vertical(bot, 0.45)
                    else:
                        _, med = split_vertical(bot, 0.55)
                    regions[f"vastus_medialis_{side}"] = geom_to_d(med)

    # Calves front → tibialis (medial/anterior) + gastrocnemius_front (lateral belly)
    for m, poly in as_polys(by.get("CALVES", [])):
        side = side_suffix(cx, poly)
        if side == "r":
            med, lat = split_vertical(poly, 0.45)
        else:
            lat, med = split_vertical(poly, 0.55)
        regions[f"tibialis_anterior_{side}"] = geom_to_d(med) or keep_d(m["d"])
        regions[f"gastrocnemius_front_{side}"] = geom_to_d(lat) or keep_d(m["d"])

    return regions


def build_back(data: dict) -> dict[str, str]:
    cx = float(data.get("centerX") or CX_DEFAULT)
    by = collect_by_group(data["muscles"])
    regions: dict[str, str] = {}

    regions["neck_back"] = neck_from_outline(data["outline"][0]["d"], cx)

    traps = as_polys(by.get("TRAPEZIUS", []))
    # Sort by vertical center: upper pair then middle pair
    traps_sorted = sorted(traps, key=lambda mp: (mp[1].bounds[1] + mp[1].bounds[3]) / 2)
    upper = traps_sorted[:2] if len(traps_sorted) >= 2 else traps_sorted
    middle = traps_sorted[2:] if len(traps_sorted) > 2 else []
    for m, poly in upper:
        side = side_suffix(cx, poly)
        regions[f"trapezius_upper_{side}"] = keep_d(m["d"])
    if middle:
        regions["trapezius_middle"] = multi_d(*[m["d"] for m, _ in middle])
    elif upper:
        # fallback: lower halves, but prefer keeping geometry soft
        regions["trapezius_middle"] = geom_to_d(
            unary_union([split_horizontal(p, 0.55)[1] for _, p in upper]),
            0.2,
        )

    for m, poly in as_polys(by.get("RHOMBOIDS", [])):
        side = side_suffix(cx, poly)
        regions[f"rhomboids_{side}"] = keep_d(m["d"])

    for m, poly in as_polys(by.get("SHOULDERS_REAR", [])):
        side = side_suffix(cx, poly)
        regions[f"rear_deltoid_{side}"] = keep_d(m["d"])

    for m, poly in as_polys(by.get("SHOULDERS_SIDE", [])):
        side = side_suffix(cx, poly)
        regions[f"lateral_deltoid_back_{side}"] = keep_d(m["d"])

    # Rotator cuff proxy: medial fragment of rear delt ∪ upper rhomboid edge
    for side in ("l", "r"):
        bits = []
        rd = regions.get(f"rear_deltoid_{side}")
        rh = regions.get(f"rhomboids_{side}")
        if rd:
            p = path_to_polygon(rd)
            if p is not None:
                bits.append(split_vertical(p, 0.55 if side == "r" else 0.45)[0 if side == "r" else 1])
        if rh:
            p = path_to_polygon(rh)
            if p is not None:
                bits.append(band(p, 0.0, 0.55))
        bits = [b for b in bits if b is not None and not b.is_empty]
        if bits:
            regions[f"rotator_cuff_{side}"] = geom_to_d(unary_union(bits))

    for m, poly in as_polys(by.get("TRICEPS", [])):
        side = side_suffix(cx, poly)
        regions[f"triceps_{side}"] = keep_d(m["d"])

    for m, poly in as_polys(by.get("FOREARMS", [])):
        side = side_suffix(cx, poly)
        regions[f"forearm_extensors_{side}"] = keep_d(m["d"])

    for m, poly in as_polys(by.get("LATS", [])):
        side = side_suffix(cx, poly)
        regions[f"latissimus_dorsi_{side}"] = keep_d(m["d"])

    for m, poly in as_polys(by.get("BACK_LOWER", [])):
        side = side_suffix(cx, poly)
        regions[f"erector_spinae_{side}"] = keep_d(m["d"])

    for m, poly in as_polys(by.get("GLUTES", [])):
        side = side_suffix(cx, poly)
        # Medius = upper/lateral organic cap; maximus keeps original glute path
        upper = poly.intersection(curved_h_mask(poly, 0.36, bulge=0.05))
        minx, miny, maxx, maxy = poly.bounds
        w = maxx - minx
        if side == "r":
            gmed = upper.intersection(box(minx + w * 0.30, miny - 2, maxx + 2, maxy + 2))
        else:
            gmed = upper.intersection(box(minx - 2, miny - 2, minx + w * 0.70, maxy + 2))
        regions[f"gluteus_medius_{side}"] = geom_to_d(gmed, 0.2) or geom_to_d(upper, 0.2)
        regions[f"gluteus_maximus_{side}"] = keep_d(m["d"])

    for m, poly in as_polys(by.get("HAMSTRINGS", [])):
        side = side_suffix(cx, poly)
        regions[f"hamstrings_{side}"] = keep_d(m["d"])

    for m, poly in as_polys(by.get("CALVES", [])):
        side = side_suffix(cx, poly)
        # Keep organic calf as gastrocnemius; soleus = soft distal Achilles band
        regions[f"gastrocnemius_{side}"] = keep_d(m["d"])
        minx, miny, maxx, maxy = poly.bounds
        y = miny + (maxy - miny) * 0.58
        sol = poly.intersection(box(minx - 2, y, maxx + 2, maxy + 2))
        regions[f"soleus_{side}"] = geom_to_d(sol, 0.2) or keep_d(m["d"])

    return regions


def write_svg(
    sex: str,
    view: str,
    outline_d: str,
    regions: dict[str, str],
    theme: str = "light",
) -> str:
    canvas = "#F8FAFC" if theme == "light" else "#0B1220"
    idle = "#E2E8F0" if theme == "light" else "#334155"
    outline_stroke = "#94A3B8" if theme == "light" else "#64748B"
    hairline = "#CBD5E1" if theme == "light" else "#475569"

    # Deterministic order for diffs
    ordered = sorted(regions.items(), key=lambda kv: kv[0])

    parts = [
        '<?xml version="1.0" encoding="UTF-8"?>\n',
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {VIEW_W} {VIEW_H}" '
        f'width="{VIEW_W}" height="{VIEW_H}" role="img" '
        f'aria-label="OmniGart {sex} {view} body map" '
        f'data-sex="{sex}" data-view="{view}" data-theme="{theme}" '
        f'data-source="musclemap-mit-adapted">\n',
        f"  <title>OmniGart {sex} {view} body map</title>\n",
        "  <desc>Human anatomical muscle regions adapted from MuscleMap "
        "(@musclemap/assets, MIT). Restyled for OmniGart tokens. "
        "Naming follows Terminologia Anatomica.</desc>\n",
        f'  <rect id="canvas" x="0" y="0" width="{VIEW_W}" height="{VIEW_H}" fill="{canvas}"/>\n',
        '  <g id="silhouette">\n',
        f'    <path id="body_outline" class="outline" d="{outline_d}" '
        f'fill="{idle}" stroke="{outline_stroke}" stroke-width="1.6" '
        f'stroke-linejoin="round" opacity="0.30"/>\n',
        "  </g>\n",
        '  <g id="muscles" stroke-linejoin="round" stroke-linecap="round">\n',
    ]
    for rid, d in ordered:
        if not d:
            continue
        parts.append(
            f'    <path id="{rid}" class="muscle" data-region="{rid}" '
            f'd="{d}" fill="{idle}" stroke="{hairline}" stroke-width="0.85"/>\n'
        )
    parts.append("  </g>\n</svg>\n")
    return "".join(parts)


def main() -> None:
    SVG_DIR.mkdir(parents=True, exist_ok=True)
    (SVG_DIR / "themed").mkdir(parents=True, exist_ok=True)
    catalog_ids: dict[str, list[str]] = {}

    for sex in ("male", "female"):
        for view in ("front", "back"):
            data = json.loads((VENDOR / f"{sex}-{view}.json").read_text())
            outline_d = data["outline"][0]["d"]
            regions = build_front(data) if view == "front" else build_back(data)

            missing = REQUIRED_IDS - {
                rid
                for rid, d in regions.items()
                if d
                and (
                    (view == "front" and not rid.endswith("_back") and rid not in {
                        "neck_back",
                        "trapezius_middle",
                        "trapezius_upper_l",
                        "trapezius_upper_r",
                        "rhomboids_l",
                        "rhomboids_r",
                        "rear_deltoid_l",
                        "rear_deltoid_r",
                        "lateral_deltoid_back_l",
                        "lateral_deltoid_back_r",
                        "rotator_cuff_l",
                        "rotator_cuff_r",
                        "triceps_l",
                        "triceps_r",
                        "forearm_extensors_l",
                        "forearm_extensors_r",
                        "latissimus_dorsi_l",
                        "latissimus_dorsi_r",
                        "erector_spinae_l",
                        "erector_spinae_r",
                        "gluteus_maximus_l",
                        "gluteus_maximus_r",
                        "gluteus_medius_l",
                        "gluteus_medius_r",
                        "hamstrings_l",
                        "hamstrings_r",
                        "gastrocnemius_l",
                        "gastrocnemius_r",
                        "soleus_l",
                        "soleus_r",
                    })
                    or (view == "back")
                )
            }
            # Validate only regions that belong on this view
            if view == "front":
                needed = {
                    r
                    for r in REQUIRED_IDS
                    if r
                    not in {
                        "neck_back",
                        "trapezius_middle",
                        "trapezius_upper_l",
                        "trapezius_upper_r",
                        "rhomboids_l",
                        "rhomboids_r",
                        "rear_deltoid_l",
                        "rear_deltoid_r",
                        "lateral_deltoid_back_l",
                        "lateral_deltoid_back_r",
                        "rotator_cuff_l",
                        "rotator_cuff_r",
                        "triceps_l",
                        "triceps_r",
                        "forearm_extensors_l",
                        "forearm_extensors_r",
                        "latissimus_dorsi_l",
                        "latissimus_dorsi_r",
                        "erector_spinae_l",
                        "erector_spinae_r",
                        "gluteus_maximus_l",
                        "gluteus_maximus_r",
                        "gluteus_medius_l",
                        "gluteus_medius_r",
                        "hamstrings_l",
                        "hamstrings_r",
                        "gastrocnemius_l",
                        "gastrocnemius_r",
                        "soleus_l",
                        "soleus_r",
                    }
                }
            else:
                needed = {
                    r
                    for r in REQUIRED_IDS
                    if r
                    not in {
                        "neck_front",
                        "pectoralis_major_clavicular_l",
                        "pectoralis_major_clavicular_r",
                        "pectoralis_major_sternal_l",
                        "pectoralis_major_sternal_r",
                        "front_deltoid_l",
                        "front_deltoid_r",
                        "lateral_deltoid_l",
                        "lateral_deltoid_r",
                        "biceps_l",
                        "biceps_r",
                        "brachialis_l",
                        "brachialis_r",
                        "forearm_flexors_l",
                        "forearm_flexors_r",
                        "rectus_abdominis",
                        "obliques_l",
                        "obliques_r",
                        "serratus_anterior_l",
                        "serratus_anterior_r",
                        "hip_flexors_l",
                        "hip_flexors_r",
                        "adductors_l",
                        "adductors_r",
                        "rectus_femoris_l",
                        "rectus_femoris_r",
                        "vastus_lateralis_l",
                        "vastus_lateralis_r",
                        "vastus_medialis_l",
                        "vastus_medialis_r",
                        "tibialis_anterior_l",
                        "tibialis_anterior_r",
                        "gastrocnemius_front_l",
                        "gastrocnemius_front_r",
                    }
                }

            present = {rid for rid, d in regions.items() if d}
            miss = needed - present
            if miss:
                raise SystemExit(f"{sex}-{view}: missing regions {sorted(miss)}")

            # Drop empty
            regions = {k: v for k, v in regions.items() if v}

            for theme in ("light", "dark"):
                doc = write_svg(sex, view, outline_d, regions, theme)
                themed = SVG_DIR / "themed" / f"{sex}-{view}-{theme}.svg"
                themed.write_text(doc)
                if theme == "light":
                    out = SVG_DIR / f"{sex}-{view}.svg"
                    out.write_text(doc)
                    catalog_ids[f"{sex}-{view}"] = sorted(regions.keys())
                    print(f"Wrote {out} ({len(regions)} regions)")

    catalog = {
        "version": 3,
        "viewBox": [0, 0, VIEW_W, VIEW_H],
        "source": "musclemap-mit-adapted",
        "figures": {
            k: {"regionCount": len(v), "regions": v} for k, v in sorted(catalog_ids.items())
        },
        "uniqueRegions": sorted({r for ids in catalog_ids.values() for r in ids}),
    }
    (ROOT / "mapping" / "regions-catalog.json").write_text(json.dumps(catalog, indent=2) + "\n")
    got = set(catalog["uniqueRegions"])
    if got != REQUIRED_IDS:
        raise SystemExit(
            f"Region id drift missing={REQUIRED_IDS - got} extra={got - REQUIRED_IDS}"
        )
    print(f"Catalog OK — {len(got)} unique region ids")


if __name__ == "__main__":
    main()

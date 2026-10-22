#!/usr/bin/env python3
"""
Generate original OmniGart anatomical body-map SVGs.

Figures are constructed from geometric landmarks and standard artistic
proportions (≈8-head canon), not traced from copyrighted atlases.
Muscle naming follows Terminologia Anatomica (TA2) preferred English terms.
"""

from __future__ import annotations

import json
import math
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SVG_DIR = ROOT / "svg"
VIEW_W, VIEW_H = 200, 480
CX = 100.0


@dataclass(frozen=True)
class SexProps:
    name: str
    shoulder_half: float
    waist_half: float
    hip_half: float
    chest_depth: float
    arm_bulk: float
    thigh_bulk: float
    calf_bulk: float
    neck_half: float
    glute_bulk: float


MALE = SexProps(
    name="male",
    shoulder_half=38,
    waist_half=22,
    hip_half=26,
    chest_depth=1.0,
    arm_bulk=1.08,
    thigh_bulk=1.05,
    calf_bulk=1.05,
    neck_half=9,
    glute_bulk=1.0,
)

FEMALE = SexProps(
    name="female",
    shoulder_half=32,
    waist_half=19,
    hip_half=31,
    chest_depth=0.92,
    arm_bulk=0.92,
    thigh_bulk=0.98,
    calf_bulk=0.95,
    neck_half=8,
    glute_bulk=1.12,
)


def poly(points: list[tuple[float, float]]) -> str:
    return "M " + " L ".join(f"{x:.2f} {y:.2f}" for x, y in points) + " Z"


def mirror(points: list[tuple[float, float]]) -> list[tuple[float, float]]:
    return [(2 * CX - x, y) for x, y in points]


def ellipse_arc(
    cx: float,
    cy: float,
    rx: float,
    ry: float,
    a0: float,
    a1: float,
    steps: int = 12,
) -> list[tuple[float, float]]:
    pts = []
    for i in range(steps + 1):
        t = a0 + (a1 - a0) * i / steps
        pts.append((cx + rx * math.cos(t), cy + ry * math.sin(t)))
    return pts


def body_outline_front(p: SexProps) -> str:
    # Symmetric silhouette built from landmarks (head → feet).
    top = [
        (CX, 18),
        (CX + p.neck_half * 0.55, 28),
        (CX + p.neck_half, 48),
        (CX + p.shoulder_half, 72),
        (CX + p.shoulder_half + 6, 92),  # deltoid bulge
        (CX + p.shoulder_half - 2, 130),
        (CX + p.shoulder_half - 8, 168),  # elbow
        (CX + p.shoulder_half - 4, 210),  # forearm
        (CX + p.shoulder_half + 2, 248),  # wrist / hand hint
        (CX + p.waist_half + 4, 168),
        (CX + p.waist_half, 200),
        (CX + p.hip_half, 236),
        (CX + p.hip_half - 2, 268),
        (CX + p.thigh_bulk * 18, 320),
        (CX + p.thigh_bulk * 14, 360),
        (CX + p.calf_bulk * 11, 400),
        (CX + p.calf_bulk * 9, 430),
        (CX + 10, 455),
        (CX + 14, 468),
        (CX, 470),
    ]
    right = top[1:]
    left = mirror(right)[::-1]
    return poly([(CX, 18)] + right + left)


def body_outline_back(p: SexProps) -> str:
    return body_outline_front(p)  # same silhouette; muscles differ


def path_el(region_id: str, d: str, *, fill: str = "#E2E8F0", cls: str = "muscle") -> str:
    return (
        f'  <path id="{region_id}" class="{cls}" data-region="{region_id}" '
        f'd="{d}" fill="{fill}" stroke="#94A3B8" stroke-width="0.6" '
        f'stroke-linejoin="round"/>\n'
    )


def front_regions(p: SexProps) -> list[tuple[str, str]]:
    regions: list[tuple[str, str]] = []
    # Neck
    regions.append(
        (
            "neck_front",
            poly(
                [
                    (CX - p.neck_half, 48),
                    (CX + p.neck_half, 48),
                    (CX + p.neck_half * 0.7, 62),
                    (CX - p.neck_half * 0.7, 62),
                ]
            ),
        )
    )

    # Pectoralis — clavicular (upper) and sternal (lower)
    pec_clav_r = [
        (CX + 4, 78),
        (CX + 22 * p.chest_depth, 74),
        (CX + 30 * p.chest_depth, 88),
        (CX + 18 * p.chest_depth, 98),
        (CX + 4, 94),
    ]
    pec_stern_r = [
        (CX + 4, 94),
        (CX + 18 * p.chest_depth, 98),
        (CX + 28 * p.chest_depth, 112),
        (CX + 16 * p.chest_depth, 128),
        (CX + 4, 126),
    ]
    regions.append(("pectoralis_major_clavicular_r", poly(pec_clav_r)))
    regions.append(("pectoralis_major_clavicular_l", poly(mirror(pec_clav_r))))
    regions.append(("pectoralis_major_sternal_r", poly(pec_stern_r)))
    regions.append(("pectoralis_major_sternal_l", poly(mirror(pec_stern_r))))

    # Deltoids
    front_delt_r = [
        (CX + p.shoulder_half - 4, 72),
        (CX + p.shoulder_half + 6, 78),
        (CX + p.shoulder_half + 4, 98),
        (CX + p.shoulder_half - 10, 102),
        (CX + p.shoulder_half - 12, 84),
    ]
    lat_delt_r = [
        (CX + p.shoulder_half + 2, 78),
        (CX + p.shoulder_half + 8, 86),
        (CX + p.shoulder_half + 6, 108),
        (CX + p.shoulder_half - 2, 112),
        (CX + p.shoulder_half - 4, 92),
    ]
    regions.append(("front_deltoid_r", poly(front_delt_r)))
    regions.append(("front_deltoid_l", poly(mirror(front_delt_r))))
    regions.append(("lateral_deltoid_r", poly(lat_delt_r)))
    regions.append(("lateral_deltoid_l", poly(mirror(lat_delt_r))))

    # Arms
    biceps_r = [
        (CX + p.shoulder_half - 14, 108),
        (CX + p.shoulder_half - 2, 112),
        (CX + p.shoulder_half + 1 * p.arm_bulk, 140),
        (CX + p.shoulder_half - 8, 155),
        (CX + p.shoulder_half - 16, 138),
    ]
    brach_r = [
        (CX + p.shoulder_half - 8, 150),
        (CX + p.shoulder_half + 1, 148),
        (CX + p.shoulder_half - 2, 168),
        (CX + p.shoulder_half - 12, 170),
    ]
    flex_r = [
        (CX + p.shoulder_half - 12, 172),
        (CX + p.shoulder_half - 1, 170),
        (CX + p.shoulder_half + 1, 205),
        (CX + p.shoulder_half - 8, 220),
        (CX + p.shoulder_half - 14, 200),
    ]
    regions.append(("biceps_r", poly(biceps_r)))
    regions.append(("biceps_l", poly(mirror(biceps_r))))
    regions.append(("brachialis_r", poly(brach_r)))
    regions.append(("brachialis_l", poly(mirror(brach_r))))
    regions.append(("forearm_flexors_r", poly(flex_r)))
    regions.append(("forearm_flexors_l", poly(mirror(flex_r))))

    # Core
    regions.append(
        (
            "rectus_abdominis",
            poly(
                [
                    (CX - 10, 130),
                    (CX + 10, 130),
                    (CX + 11, 175),
                    (CX + 9, 210),
                    (CX - 9, 210),
                    (CX - 11, 175),
                ]
            ),
        )
    )
    obl_r = [
        (CX + 11, 132),
        (CX + 24 * p.chest_depth, 128),
        (CX + p.waist_half + 2, 170),
        (CX + p.hip_half - 4, 220),
        (CX + 10, 215),
        (CX + 12, 175),
    ]
    regions.append(("obliques_r", poly(obl_r)))
    regions.append(("obliques_l", poly(mirror(obl_r))))

    serr_r = [
        (CX + 24 * p.chest_depth, 112),
        (CX + 32 * p.chest_depth, 118),
        (CX + 30 * p.chest_depth, 138),
        (CX + 22 * p.chest_depth, 142),
        (CX + 20 * p.chest_depth, 122),
    ]
    regions.append(("serratus_anterior_r", poly(serr_r)))
    regions.append(("serratus_anterior_l", poly(mirror(serr_r))))

    # Hips / legs
    hip_flex_r = [
        (CX + 6, 212),
        (CX + 18, 208),
        (CX + p.hip_half - 2, 230),
        (CX + 16, 248),
        (CX + 6, 242),
    ]
    add_r = [
        (CX + 2, 236),
        (CX + 12, 240),
        (CX + 14, 300),
        (CX + 4, 310),
        (CX + 1, 270),
    ]
    rf_r = [
        (CX + 8, 248),
        (CX + 18, 252),
        (CX + 16, 320),
        (CX + 8, 335),
        (CX + 4, 290),
    ]
    vl_r = [
        (CX + 18, 250),
        (CX + p.thigh_bulk * 20, 255),
        (CX + p.thigh_bulk * 17, 330),
        (CX + 14, 340),
        (CX + 16, 290),
    ]
    vm_r = [
        (CX + 4, 290),
        (CX + 10, 295),
        (CX + 10, 340),
        (CX + 4, 348),
        (CX + 2, 320),
    ]
    ta_r = [
        (CX + 6, 360),
        (CX + 14, 358),
        (CX + 12, 420),
        (CX + 6, 430),
        (CX + 4, 390),
    ]
    gas_f_r = [
        (CX + 14, 360),
        (CX + p.calf_bulk * 12, 365),
        (CX + p.calf_bulk * 10, 410),
        (CX + 12, 400),
    ]
    for name, pts in [
        ("hip_flexors_r", hip_flex_r),
        ("adductors_r", add_r),
        ("rectus_femoris_r", rf_r),
        ("vastus_lateralis_r", vl_r),
        ("vastus_medialis_r", vm_r),
        ("tibialis_anterior_r", ta_r),
        ("gastrocnemius_front_r", gas_f_r),
    ]:
        regions.append((name, poly(pts)))
        regions.append((name.replace("_r", "_l"), poly(mirror(pts))))

    return regions


def back_regions(p: SexProps) -> list[tuple[str, str]]:
    regions: list[tuple[str, str]] = []
    regions.append(
        (
            "neck_back",
            poly(
                [
                    (CX - p.neck_half, 48),
                    (CX + p.neck_half, 48),
                    (CX + p.neck_half * 0.75, 64),
                    (CX - p.neck_half * 0.75, 64),
                ]
            ),
        )
    )

    trap_u_r = [
        (CX + 2, 58),
        (CX + 18, 62),
        (CX + p.shoulder_half - 4, 78),
        (CX + 16, 92),
        (CX + 2, 88),
    ]
    trap_mid = [
        (CX - 14, 88),
        (CX + 14, 88),
        (CX + 12, 130),
        (CX - 12, 130),
    ]
    rhomb_r = [
        (CX + 4, 95),
        (CX + 16, 98),
        (CX + 14, 128),
        (CX + 4, 132),
    ]
    regions.append(("trapezius_upper_r", poly(trap_u_r)))
    regions.append(("trapezius_upper_l", poly(mirror(trap_u_r))))
    regions.append(("trapezius_middle", poly(trap_mid)))
    regions.append(("rhomboids_r", poly(rhomb_r)))
    regions.append(("rhomboids_l", poly(mirror(rhomb_r))))

    rear_delt_r = [
        (CX + p.shoulder_half - 8, 74),
        (CX + p.shoulder_half + 4, 78),
        (CX + p.shoulder_half + 2, 100),
        (CX + p.shoulder_half - 12, 104),
    ]
    lat_back_r = [
        (CX + p.shoulder_half + 1, 80),
        (CX + p.shoulder_half + 7, 90),
        (CX + p.shoulder_half + 4, 112),
        (CX + p.shoulder_half - 4, 114),
    ]
    regions.append(("rear_deltoid_r", poly(rear_delt_r)))
    regions.append(("rear_deltoid_l", poly(mirror(rear_delt_r))))
    regions.append(("lateral_deltoid_back_r", poly(lat_back_r)))
    regions.append(("lateral_deltoid_back_l", poly(mirror(lat_back_r))))

    rc_r = [
        (CX + 18, 90),
        (CX + 28, 94),
        (CX + 26, 112),
        (CX + 16, 110),
    ]
    regions.append(("rotator_cuff_r", poly(rc_r)))
    regions.append(("rotator_cuff_l", poly(mirror(rc_r))))

    tri_r = [
        (CX + p.shoulder_half - 12, 108),
        (CX + p.shoulder_half + 2, 112),
        (CX + p.shoulder_half + 1 * p.arm_bulk, 155),
        (CX + p.shoulder_half - 10, 168),
        (CX + p.shoulder_half - 16, 140),
    ]
    ext_r = [
        (CX + p.shoulder_half - 12, 172),
        (CX + p.shoulder_half - 1, 170),
        (CX + p.shoulder_half + 1, 208),
        (CX + p.shoulder_half - 8, 222),
        (CX + p.shoulder_half - 14, 198),
    ]
    regions.append(("triceps_r", poly(tri_r)))
    regions.append(("triceps_l", poly(mirror(tri_r))))
    regions.append(("forearm_extensors_r", poly(ext_r)))
    regions.append(("forearm_extensors_l", poly(mirror(ext_r))))

    lat_r = [
        (CX + 14, 118),
        (CX + p.shoulder_half - 8, 112),
        (CX + p.shoulder_half - 6, 160),
        (CX + p.waist_half + 6, 200),
        (CX + 12, 195),
        (CX + 10, 150),
    ]
    erector_r = [
        (CX + 2, 130),
        (CX + 10, 132),
        (CX + 10, 220),
        (CX + 2, 230),
    ]
    regions.append(("latissimus_dorsi_r", poly(lat_r)))
    regions.append(("latissimus_dorsi_l", poly(mirror(lat_r))))
    regions.append(("erector_spinae_r", poly(erector_r)))
    regions.append(("erector_spinae_l", poly(mirror(erector_r))))

    gmed_r = [
        (CX + 8, 220),
        (CX + p.hip_half - 2, 218),
        (CX + p.hip_half, 245),
        (CX + 14, 250),
    ]
    gmax_r = [
        (CX + 4, 235),
        (CX + 18 * p.glute_bulk, 240),
        (CX + 20 * p.glute_bulk, 275),
        (CX + 8, 285),
        (CX + 2, 260),
    ]
    regions.append(("gluteus_medius_r", poly(gmed_r)))
    regions.append(("gluteus_medius_l", poly(mirror(gmed_r))))
    regions.append(("gluteus_maximus_r", poly(gmax_r)))
    regions.append(("gluteus_maximus_l", poly(mirror(gmax_r))))

    ham_r = [
        (CX + 6, 285),
        (CX + 18 * p.thigh_bulk, 288),
        (CX + 16 * p.thigh_bulk, 350),
        (CX + 8, 360),
        (CX + 4, 320),
    ]
    gas_r = [
        (CX + 6, 360),
        (CX + 14 * p.calf_bulk, 362),
        (CX + 13 * p.calf_bulk, 410),
        (CX + 7, 420),
        (CX + 4, 385),
    ]
    sol_r = [
        (CX + 7, 400),
        (CX + 12, 402),
        (CX + 10, 438),
        (CX + 6, 442),
    ]
    for name, pts in [
        ("hamstrings_r", ham_r),
        ("gastrocnemius_r", gas_r),
        ("soleus_r", sol_r),
    ]:
        regions.append((name, poly(pts)))
        regions.append((name.replace("_r", "_l"), poly(mirror(pts))))

    return regions


def svg_document(
    sex: SexProps,
    view: str,
    theme: str = "light",
) -> tuple[str, list[str]]:
    canvas = "#F8FAFC" if theme == "light" else "#0B1220"
    idle = "#E2E8F0" if theme == "light" else "#334155"
    outline = "#94A3B8" if theme == "light" else "#64748B"
    regions = front_regions(sex) if view == "front" else back_regions(sex)
    outline_d = body_outline_front(sex) if view == "front" else body_outline_back(sex)
    ids = [r[0] for r in regions]

    parts = [
        '<?xml version="1.0" encoding="UTF-8"?>\n',
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {VIEW_W} {VIEW_H}" '
        f'width="{VIEW_W}" height="{VIEW_H}" role="img" '
        f'aria-label="OmniGart {sex.name} {view} body map" '
        f'data-sex="{sex.name}" data-view="{view}" data-theme="{theme}">\n',
        f'  <title>OmniGart {sex.name} {view} body map</title>\n',
        "  <desc>Original geometric anatomical regions for workout highlighting. "
        "Naming follows Terminologia Anatomica. Not traced from copyrighted atlases.</desc>\n",
        f'  <rect id="canvas" x="0" y="0" width="{VIEW_W}" height="{VIEW_H}" fill="{canvas}"/>\n',
        '  <g id="silhouette">\n',
        f'    <path id="body_outline" class="outline" d="{outline_d}" '
        f'fill="{idle}" stroke="{outline}" stroke-width="1.2" '
        f'stroke-linejoin="round" opacity="0.35"/>\n',
        "  </g>\n",
        '  <g id="muscles">\n',
    ]
    for rid, d in regions:
        parts.append(
            f'  <path id="{rid}" class="muscle" data-region="{rid}" '
            f'd="{d}" fill="{idle}" stroke="{outline}" stroke-width="0.55" '
            f'stroke-linejoin="round"/>\n'
        )
    parts.append("  </g>\n</svg>\n")
    return "".join(parts), ids


def write_region_catalog(all_ids: dict[str, list[str]]) -> None:
    catalog = {
        "version": 1,
        "viewBox": [0, 0, VIEW_W, VIEW_H],
        "figures": {
            key: {"regionCount": len(ids), "regions": ids}
            for key, ids in sorted(all_ids.items())
        },
        "uniqueRegions": sorted({r for ids in all_ids.values() for r in ids}),
    }
    out = ROOT / "mapping" / "regions-catalog.json"
    out.write_text(json.dumps(catalog, indent=2) + "\n")
    print(f"Wrote {out} ({len(catalog['uniqueRegions'])} unique region ids)")


def main() -> None:
    SVG_DIR.mkdir(parents=True, exist_ok=True)
    all_ids: dict[str, list[str]] = {}
    for sex in (MALE, FEMALE):
        for view in ("front", "back"):
            for theme in ("light", "dark"):
                doc, ids = svg_document(sex, view, theme)
                if theme == "light":
                    path = SVG_DIR / f"{sex.name}-{view}.svg"
                    path.write_text(doc)
                    all_ids[f"{sex.name}-{view}"] = ids
                    print(f"Wrote {path} ({len(ids)} regions)")
                # Also emit themed variants for previews
                themed = SVG_DIR / "themed" / f"{sex.name}-{view}-{theme}.svg"
                themed.parent.mkdir(parents=True, exist_ok=True)
                themed.write_text(doc)
    write_region_catalog(all_ids)


if __name__ == "__main__":
    main()

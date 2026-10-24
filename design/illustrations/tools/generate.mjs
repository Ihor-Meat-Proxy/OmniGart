#!/usr/bin/env node
/**
 * Generate OmniGart illustration SVGs + platform assets.
 *
 * SVG sources → index.json → iOS/watch PDF+PNG catalogs, Android VectorDrawables,
 * contact sheets. Original programmatic art from parts/.
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { Resvg } from '@resvg/resvg-js';
import sharp from 'sharp';
import { PDFDocument } from 'pdf-lib';
import { badgeBackground, svgDoc, COLORS } from './style.mjs';
import { regionsForMuscles, poseForPattern, tinyFigure } from './parts/body.mjs';
import { equipmentShape, equipmentForContentTag } from './parts/equipment.mjs';
import { buildIndex } from './build-index.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ILL = path.resolve(__dirname, '..');
const ROOT = path.resolve(ILL, '../..');
const GEN = path.join(ROOT, 'design/generated');
const IOS = path.join(GEN, 'ios/OmniGartIllustrations.xcassets');
const WATCH = path.join(GEN, 'watch/OmniGartIllustrations.xcassets');
const ANDROID = path.join(GEN, 'android/drawable');
const PREVIEWS = path.join(ILL, 'generated/previews');
const COMPOSE = path.join(GEN, 'compose');
const IOS_SRC = path.join(GEN, 'ios/Sources');

function ensureDir(d) {
  fs.mkdirSync(d, { recursive: true });
}

function composeEquipmentSvg(entry) {
  const shape = entry.shape || 'generic_machine';
  const parts = [...badgeBackground()];
  if (shape === 'figure') {
    parts.push(tinyFigure());
  } else {
    parts.push(equipmentShape(shape));
  }
  return svgDoc(`  ${parts.join('\n  ')}\n`, `equip:${entry.id}`);
}

function composeExerciseSvg(entry) {
  const highlights = regionsForMuscles(entry.primaryMuscles, entry.secondaryMuscles);
  const body = poseForPattern(entry.movementPattern, highlights);
  const parts = [...badgeBackground(), body];

  // Overlay a small equipment cue when useful (not bodyweight/none).
  const eq = (entry.equipment || []).find((e) => e !== 'bodyweight' && e !== 'none');
  if (eq) {
    // Scale-down overlay in corner via group translate
    const cue = equipmentForContentTag(eq);
    if (cue) {
      parts.push(
        `<g transform="translate(78 78) scale(0.32)" opacity="0.95">${cue}</g>`,
      );
    }
  }
  return svgDoc(`  ${parts.join('\n  ')}\n`, `ex:${entry.id}`);
}

function assetName(kind, id) {
  // Asset catalog / drawable safe names
  return `${kind === 'equipment' ? 'equip' : 'ex'}_${id}`.replace(/-/g, '_');
}

function androidDrawableName(kind, id) {
  return `il_og_${kind === 'equipment' ? 'equip' : 'ex'}_${id}`.replace(/-/g, '_');
}

/** Convert simple filled SVG primitives into VectorDrawable paths (best-effort). */
function svgToVectorDrawable(name, svg) {
  // Raster-backed VD is awkward; emit a layered VD with path approximations
  // by keeping the SVG as a reference and shipping PNG companion for complex art.
  // For CI / offline, we also write a tintable circular badge placeholder path
  // plus note — actual multi-colour art is the PNG in drawable-nodpi when needed.
  // Prefer true path extraction for circles/rects used by our primitives.
  const paths = [];
  // White badge
  paths.push(`    <path
        android:pathData="M64,6a58,58 0 1,0 0.1,0z"
        android:fillColor="#FFFFFFFF"/>`);
  paths.push(`    <path
        android:pathData="M64,6a58,58 0 1,0 0.1,0z"
        android:fillColor="#00000000"
        android:strokeWidth="1.5"
        android:strokeColor="#FFE2E8F0"/>`);

  // Extract circle fills
  for (const m of svg.matchAll(
    /<circle[^>]*cx="([\d.]+)"[^>]*cy="([\d.]+)"[^>]*r="([\d.]+)"[^>]*fill="(#[A-Fa-f0-9]+|none)"[^>]*\/?>/g,
  )) {
    const [, cx, cy, r, fill] = m;
    if (fill === 'none') continue;
    const d = `M${(+cx - +r).toFixed(2)},${cy}a${r},${r} 0 1,0 ${(2 * +r).toFixed(2)},0a${r},${r} 0 1,0 ${(-2 * +r).toFixed(2)},0`;
    paths.push(`    <path
        android:pathData="${d}"
        android:fillColor="${androidColor(fill)}"/>`);
  }
  // Extract rect fills
  for (const m of svg.matchAll(
    /<rect[^>]*x="([\d.]+)"[^>]*y="([\d.]+)"[^>]*width="([\d.]+)"[^>]*height="([\d.]+)"[^>]*fill="(#[A-Fa-f0-9]+)"[^>]*\/?>/g,
  )) {
    const [, x, y, w, h, fill] = m;
    const d = `M${x},${y}h${w}v${h}h${-w}z`;
    paths.push(`    <path
        android:pathData="${d}"
        android:fillColor="${androidColor(fill)}"/>`);
  }
  // Extract path fills
  for (const m of svg.matchAll(/<path[^>]*d="([^"]+)"[^>]*fill="(#[A-Fa-f0-9]+)"[^>]*\/?>/g)) {
    const [, d, fill] = m;
    paths.push(`    <path
        android:pathData="${d}"
        android:fillColor="${androidColor(fill)}"/>`);
  }
  // Stroke-only paths / lines as thin strokes
  for (const m of svg.matchAll(
    /<path[^>]*d="([^"]+)"[^>]*fill="none"[^>]*stroke="(#[A-Fa-f0-9]+)"[^>]*\/?>/g,
  )) {
    const [, d, stroke] = m;
    paths.push(`    <path
        android:pathData="${d}"
        android:fillColor="#00000000"
        android:strokeWidth="2.25"
        android:strokeColor="${androidColor(stroke)}"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"/>`);
  }
  for (const m of svg.matchAll(
    /<line[^>]*x1="([\d.]+)"[^>]*y1="([\d.]+)"[^>]*x2="([\d.]+)"[^>]*y2="([\d.]+)"[^>]*stroke="(#[A-Fa-f0-9]+)"[^>]*\/?>/g,
  )) {
    const [, x1, y1, x2, y2, stroke] = m;
    paths.push(`    <path
        android:pathData="M${x1},${y1}L${x2},${y2}"
        android:fillColor="#00000000"
        android:strokeWidth="2.25"
        android:strokeColor="${androidColor(stroke)}"
        android:strokeLineCap="round"/>`);
  }
  for (const m of svg.matchAll(
    /<ellipse[^>]*cx="([\d.]+)"[^>]*cy="([\d.]+)"[^>]*rx="([\d.]+)"[^>]*ry="([\d.]+)"[^>]*fill="(#[A-Fa-f0-9]+|none)"[^>]*\/?>/g,
  )) {
    const [, cx, cy, rx, ry, fill] = m;
    if (fill === 'none') {
      const d = `M${(+cx - +rx).toFixed(2)},${cy}a${rx},${ry} 0 1,0 ${(2 * +rx).toFixed(2)},0a${rx},${ry} 0 1,0 ${(-2 * +rx).toFixed(2)},0`;
      paths.push(`    <path
        android:pathData="${d}"
        android:fillColor="#00000000"
        android:strokeWidth="2.25"
        android:strokeColor="${androidColor(COLORS.equipment)}"/>`);
      continue;
    }
    const d = `M${(+cx - +rx).toFixed(2)},${cy}a${rx},${ry} 0 1,0 ${(2 * +rx).toFixed(2)},0a${rx},${ry} 0 1,0 ${(-2 * +rx).toFixed(2)},0`;
    paths.push(`    <path
        android:pathData="${d}"
        android:fillColor="${androidColor(fill)}"/>`);
  }

  return `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="128dp"
    android:height="128dp"
    android:viewportWidth="128"
    android:viewportHeight="128">
${paths.join('\n')}
</vector>
`;
}

function androidColor(hex) {
  if (!hex || hex === 'none') return '#00000000';
  const h = hex.replace('#', '');
  if (h.length === 6) return `#FF${h.toUpperCase()}`;
  if (h.length === 8) return `#${h.toUpperCase()}`;
  return '#FF334155';
}

async function rasterize(svg, size) {
  const resvg = new Resvg(svg, {
    fitTo: { mode: 'width', value: size },
    background: 'rgba(0,0,0,0)',
  });
  return resvg.render().asPng();
}

async function pngToPdf(pngBytes, size) {
  const pdf = await PDFDocument.create();
  const page = pdf.addPage([size, size]);
  const img = await pdf.embedPng(pngBytes);
  page.drawImage(img, { x: 0, y: 0, width: size, height: size });
  return Buffer.from(await pdf.save());
}

function writeIosImageset(catalogDir, name, pdfBuf, pngBuf) {
  const dir = path.join(catalogDir, `${name}.imageset`);
  ensureDir(dir);
  fs.writeFileSync(path.join(dir, `${name}.pdf`), pdfBuf);
  fs.writeFileSync(path.join(dir, `${name}.png`), pngBuf);
  const contents = {
    images: [
      {
        filename: `${name}.pdf`,
        idiom: 'universal',
      },
      {
        filename: `${name}.png`,
        idiom: 'universal',
        scale: '2x',
      },
    ],
    info: { author: 'omnigart', version: 1 },
    properties: {
      'preserves-vector-representation': true,
    },
  };
  fs.writeFileSync(path.join(dir, 'Contents.json'), JSON.stringify(contents, null, 2) + '\n');
}

function writeCatalogRoot(dir) {
  ensureDir(dir);
  fs.writeFileSync(
    path.join(dir, 'Contents.json'),
    JSON.stringify({ info: { author: 'omnigart', version: 1 } }, null, 2) + '\n',
  );
}

async function contactSheet(items, outPath, title) {
  const cell = 96;
  const labelH = 18;
  const pad = 24;
  const cols = Math.min(10, Math.max(4, Math.ceil(Math.sqrt(items.length))));
  const rows = Math.ceil(items.length / cols);
  const width = pad * 2 + cols * cell;
  const height = pad * 2 + 40 + rows * (cell + labelH);

  const tiles = [];
  for (let i = 0; i < items.length; i++) {
    const col = i % cols;
    const row = Math.floor(i / cols);
    const png = await rasterize(items[i].svg, 72);
    tiles.push({
      input: png,
      left: Math.round(pad + col * cell + (cell - 72) / 2),
      top: Math.round(pad + 40 + row * (cell + labelH)),
    });
  }

  let labels = '';
  for (let i = 0; i < items.length; i++) {
    const col = i % cols;
    const row = Math.floor(i / cols);
    const x = pad + col * cell + cell / 2;
    const y = pad + 40 + row * (cell + labelH) + 84;
    const label = items[i].id.length > 14 ? items[i].id.slice(0, 13) + '…' : items[i].id;
    labels += `<text x="${x}" y="${y}" text-anchor="middle" font-family="ui-sans-serif,system-ui,sans-serif" font-size="9" fill="#475569">${escapeXml(label)}</text>\n`;
  }

  const header = `<text x="${pad}" y="28" font-family="ui-sans-serif,system-ui,sans-serif" font-size="16" font-weight="600" fill="#0F172A">${escapeXml(title)} (${items.length})</text>`;
  const sheetSvg = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}">
  <rect width="100%" height="100%" fill="#F8FAFC"/>
  ${header}
  ${labels}
</svg>`;

  const base = await sharp(Buffer.from(sheetSvg)).png().toBuffer();
  const out = await sharp(base).composite(tiles).png().toBuffer();
  ensureDir(path.dirname(outPath));
  fs.writeFileSync(outPath, out);
  return outPath;
}

function escapeXml(s) {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function writeSwiftHelper(index) {
  ensureDir(IOS_SRC);
  const src = `// Generated by design/illustrations/tools/generate.mjs — do not edit.
import Foundation

/// Resolves illustration asset names for equipment and exercise ids.
public enum IllustrationAsset: Sendable {
    public static let catalogName = "OmniGartIllustrations"

    public static func equipment(id: String) -> String {
        "equip_" + sanitize(id)
    }

    public static func exercise(id: String) -> String {
        "ex_" + sanitize(id)
    }

    /// Asset name from a content \`illustrationKey\` (\`media/illustrations/<id>\`).
    public static func fromIllustrationKey(_ key: String) -> String? {
        guard let id = key.split(separator: "/").last else { return nil }
        return exercise(id: String(id))
    }

    public static let equipmentCount = ${index.counts.equipment}
    public static let exerciseCount = ${index.counts.exercises}

    private static func sanitize(_ id: String) -> String {
        id.replacingOccurrences(of: "-", with: "_")
    }
}
`;
  fs.writeFileSync(path.join(IOS_SRC, 'IllustrationAsset.swift'), src);
}

function writeKotlinHelper(index) {
  ensureDir(COMPOSE);
  const src = `// Generated by design/illustrations/tools/generate.mjs — do not edit.
package com.omnigart.design.illustrations

/**
 * Resolves Android drawable / Compose painter resource names for illustrations.
 * Drawables are generated as \`il_og_equip_*\` / \`il_og_ex_*\` under design/generated/android/drawable.
 */
object OmniGartIllustrations {
    const val EQUIPMENT_COUNT: Int = ${index.counts.equipment}
    const val EXERCISE_COUNT: Int = ${index.counts.exercises}

    fun equipmentDrawableName(id: String): String =
        "il_og_equip_" + id.replace('-', '_')

    fun exerciseDrawableName(id: String): String =
        "il_og_ex_" + id.replace('-', '_')

    fun fromIllustrationKey(key: String): String? {
        val id = key.substringAfterLast('/')
        if (id.isEmpty()) return null
        return exerciseDrawableName(id)
    }
}
`;
  fs.writeFileSync(path.join(COMPOSE, 'OmniGartIllustrations.kt'), src);
}

async function main() {
  console.log('Building illustration index…');
  const index = buildIndex();

  ensureDir(path.join(ILL, 'equipment'));
  ensureDir(path.join(ILL, 'exercises'));
  writeCatalogRoot(IOS);
  writeCatalogRoot(WATCH);
  ensureDir(ANDROID);
  ensureDir(PREVIEWS);

  const equipSheets = [];
  const exSheets = [];

  console.log(`Writing ${index.equipment.length} equipment SVGs…`);
  for (const entry of index.equipment) {
    const svg = composeEquipmentSvg(entry);
    const file = path.join(ILL, entry.asset);
    fs.writeFileSync(file, svg);

    const name = assetName('equipment', entry.id);
    const png = await rasterize(svg, 256);
    const pdf = await pngToPdf(png, 256);
    writeIosImageset(IOS, name, pdf, png);
    writeIosImageset(WATCH, name, pdf, await rasterize(svg, 128));

    const vd = svgToVectorDrawable(name, svg);
    fs.writeFileSync(path.join(ANDROID, `${androidDrawableName('equipment', entry.id)}.xml`), vd);

    equipSheets.push({ id: entry.id, svg });
  }

  console.log(`Writing ${index.exercises.length} exercise SVGs…`);
  for (const entry of index.exercises) {
    const svg = composeExerciseSvg(entry);
    const file = path.join(ILL, entry.asset);
    fs.writeFileSync(file, svg);

    const name = assetName('exercise', entry.id);
    const png = await rasterize(svg, 256);
    const pdf = await pngToPdf(png, 256);
    writeIosImageset(IOS, name, pdf, png);
    writeIosImageset(WATCH, name, pdf, await rasterize(svg, 128));

    const vd = svgToVectorDrawable(name, svg);
    fs.writeFileSync(path.join(ANDROID, `${androidDrawableName('exercise', entry.id)}.xml`), vd);

    exSheets.push({ id: entry.id, svg });
  }

  writeSwiftHelper(index);
  writeKotlinHelper(index);

  console.log('Rendering contact sheets…');
  await contactSheet(
    equipSheets,
    path.join(PREVIEWS, 'contact-sheet-equipment.png'),
    'OmniGart Equipment',
  );
  await contactSheet(
    exSheets,
    path.join(PREVIEWS, 'contact-sheet-exercises.png'),
    'OmniGart Exercises',
  );
  // Combined sheet (sample grid for PR) — all equipment + first/last exercises would be huge;
  // build a mixed overview: all equipment + every exercise.
  await contactSheet(
    [...equipSheets, ...exSheets],
    path.join(PREVIEWS, 'contact-sheet-all.png'),
    'OmniGart Illustrations',
  );

  // Copy combined sheet to artifacts-friendly path under design/illustrations
  fs.copyFileSync(
    path.join(PREVIEWS, 'contact-sheet-all.png'),
    path.join(ILL, 'contact-sheet.png'),
  );

  console.log(
    `Done: ${index.counts.equipment} equipment + ${index.counts.exercises} exercises = ${index.counts.total}`,
  );
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});

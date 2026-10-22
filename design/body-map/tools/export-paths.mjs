#!/usr/bin/env node
/**
 * Export per-muscle SVG path `d` attributes to JSON for SwiftUI Path / Compose PathParser.
 * SVG remains the source of truth; apps tint regions dynamically without shipping 129 rasters.
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createHash } from 'node:crypto';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..');
const SVG_DIR = path.join(ROOT, 'svg');
const OUT_DIR = path.join(ROOT, 'generated', 'paths');
const FIGURES = ['male-front', 'male-back', 'female-front', 'female-back'];

function extractPaths(svg) {
  const regions = {};
  const re =
    /<path\b([^>]*?)\bid="([^"]+)"([^>]*?)\/?>/gis;
  let m;
  while ((m = re.exec(svg))) {
    const before = m[1] || '';
    const id = m[2];
    const after = m[3] || '';
    const attrs = before + after;
    if (id === 'body_outline' || id === 'canvas') continue;
    const dMatch = /\bd="([^"]+)"/i.exec(attrs);
    const cls = /\bclass="([^"]+)"/i.exec(attrs);
    if (!dMatch) continue;
    if (cls && cls[1] === 'outline') continue;
    regions[id] = { d: dMatch[1], class: (cls && cls[1]) || 'muscle' };
  }
  return regions;
}

function main() {
  fs.mkdirSync(OUT_DIR, { recursive: true });
  const index = {
    version: 1,
    viewBox: [0, 0, 1024, 1536],
    generatedAt: new Date().toISOString(),
    figures: {},
    renderingNotes: {
      approach:
        'Ship path JSON + tint at runtime (SwiftUI Path / Compose PathParser). Do not bake 129 exercise PNGs into the app binary.',
      sourceOfTruth: 'design/body-map/svg/*.svg',
    },
  };

  for (const fig of FIGURES) {
    const svgPath = path.join(SVG_DIR, `${fig}.svg`);
    const svg = fs.readFileSync(svgPath, 'utf8');
    const regions = extractPaths(svg);
    const payload = {
      id: fig,
      sex: fig.startsWith('female') ? 'female' : 'male',
      view: fig.endsWith('front') ? 'front' : 'back',
      viewBox: { x: 0, y: 0, width: 1024, height: 1536 },
      regionCount: Object.keys(regions).length,
      regions,
    };
    const outFile = path.join(OUT_DIR, `${fig}.json`);
    fs.writeFileSync(outFile, JSON.stringify(payload, null, 2) + '\n');
    const hash = createHash('sha256').update(JSON.stringify(regions)).digest('hex');
    index.figures[fig] = {
      file: `${fig}.json`,
      regionCount: payload.regionCount,
      regionIds: Object.keys(regions).sort(),
      contentSha256: hash,
    };
    console.log(`Exported ${fig}: ${payload.regionCount} regions → ${outFile}`);
  }

  fs.writeFileSync(path.join(OUT_DIR, 'index.json'), JSON.stringify(index, null, 2) + '\n');
  console.log('Wrote index.json');
}

main();

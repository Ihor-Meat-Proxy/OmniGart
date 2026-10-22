#!/usr/bin/env node
/**
 * OmniGart icon pipeline:
 *   SVG sources → Android VectorDrawable, Compose ImageVector, iOS asset catalog
 *   (SVG with Preserve Vector Data) + PNG contact sheet.
 *
 * Justification (iOS): Single-colour line icons ship as SVG in an asset catalog
 * with "Preserve Vector Data" + "Template Rendering". This matches Apple's
 * current guidance for custom vector symbols without maintaining PDF or the
 * heavier SF Symbols template workflow, and keeps one source format for all
 * platforms. (Custom SF Symbol templates remain optional later if variable
 * colour / hierarchical rendering is needed.)
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { Resvg } from '@resvg/resvg-js';
import sharp from 'sharp';
import { ICONS, ICON_NAMES, STROKE } from './iconsource.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..');
const SVG_DIR = path.join(ROOT, 'icons', 'svg');
const GEN = path.join(ROOT, 'generated');
const ANDROID_DIR = path.join(GEN, 'android', 'drawable');
const COMPOSE_DIR = path.join(GEN, 'compose');
const IOS_DIR = path.join(GEN, 'ios', 'OmniGartIcons.xcassets');

function attrs(obj) {
  return Object.entries(obj)
    .map(([k, v]) => `${k}="${v}"`)
    .join(' ');
}

function toSvg(name, def) {
  const parts = [];
  for (const d of def.paths || []) {
    parts.push(`  <path d="${d}" ${attrs(STROKE)} />`);
  }
  for (const c of def.circles || []) {
    if (c.fill) {
      parts.push(
        `  <circle cx="${c.cx}" cy="${c.cy}" r="${c.r}" fill="currentColor" stroke="none" />`,
      );
    } else {
      parts.push(`  <circle cx="${c.cx}" cy="${c.cy}" r="${c.r}" ${attrs(STROKE)} />`);
    }
  }
  for (const l of def.lines || []) {
    parts.push(
      `  <line x1="${l.x1}" y1="${l.y1}" x2="${l.x2}" y2="${l.y2}" ${attrs(STROKE)} />`,
    );
  }
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none" aria-hidden="true" data-omnigart-icon="${name}">
${parts.join('\n')}
</svg>
`;
}

function androidName(name) {
  return `ic_og_${name.replace(/-/g, '_')}`;
}

function pathToAndroid(d) {
  // Android VectorDrawable accepts the same path data subset we use.
  return d;
}

function toVectorDrawable(name, def) {
  const nodes = [];
  for (const d of def.paths || []) {
    nodes.push(
      `    <path
        android:pathData="${pathToAndroid(d)}"
        android:strokeWidth="1.75"
        android:strokeColor="#FF000000"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:fillColor="#00000000" />`,
    );
  }
  for (const c of def.circles || []) {
    if (c.fill) {
      // Approximate filled circle as path
      const r = c.r;
      const cx = c.cx;
      const cy = c.cy;
      const d = `M${cx - r},${cy}a${r},${r} 0 1,0 ${r * 2},0a${r},${r} 0 1,0 -${r * 2},0`;
      nodes.push(
        `    <path
        android:pathData="${d}"
        android:fillColor="#FF000000" />`,
      );
    } else {
      const r = c.r;
      const cx = c.cx;
      const cy = c.cy;
      const d = `M${cx - r},${cy}a${r},${r} 0 1,0 ${r * 2},0a${r},${r} 0 1,0 -${r * 2},0`;
      nodes.push(
        `    <path
        android:pathData="${d}"
        android:strokeWidth="1.75"
        android:strokeColor="#FF000000"
        android:strokeLineCap="round"
        android:fillColor="#00000000" />`,
      );
    }
  }
  for (const l of def.lines || []) {
    nodes.push(
      `    <path
        android:pathData="M${l.x1},${l.y1}L${l.x2},${l.y2}"
        android:strokeWidth="1.75"
        android:strokeColor="#FF000000"
        android:strokeLineCap="round"
        android:fillColor="#00000000" />`,
    );
  }
  return `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
${nodes.join('\n')}
</vector>
`;
}

function parsePathCommands(d) {
  // Lightweight tokenizer for ImageVector path builders (M/L/C/Q/A/Z/H/V and relatives).
  const tokens = [];
  const re = /([MmLlHhVvCcSsQqTtAaZz])|([+-]?(?:\d*\.\d+|\d+)(?:[eE][+-]?\d+)?)/g;
  let m;
  while ((m = re.exec(d))) {
    if (m[1]) tokens.push({ type: 'cmd', v: m[1] });
    else tokens.push({ type: 'num', v: parseFloat(m[2]) });
  }
  return tokens;
}

function pathToComposeBuilder(d, indent = '                ') {
  const tokens = parsePathCommands(d);
  let i = 0;
  const lines = [];
  let cmd = null;
  const take = (n) => {
    const out = [];
    for (let k = 0; k < n; k++) out.push(tokens[i++].v);
    return out;
  };
  while (i < tokens.length) {
    if (tokens[i].type === 'cmd') {
      cmd = tokens[i++].v;
      if (cmd === 'Z' || cmd === 'z') {
        lines.push(`${indent}close()`);
        continue;
      }
    }
    if (!cmd) break;
    switch (cmd) {
      case 'M': {
        const [x, y] = take(2);
        lines.push(`${indent}moveTo(${x}f, ${y}f)`);
        cmd = 'L';
        break;
      }
      case 'm': {
        const [x, y] = take(2);
        lines.push(`${indent}moveToRelative(${x}f, ${y}f)`);
        cmd = 'l';
        break;
      }
      case 'L': {
        const [x, y] = take(2);
        lines.push(`${indent}lineTo(${x}f, ${y}f)`);
        break;
      }
      case 'l': {
        const [x, y] = take(2);
        lines.push(`${indent}lineToRelative(${x}f, ${y}f)`);
        break;
      }
      case 'H': {
        const [x] = take(1);
        lines.push(`${indent}horizontalLineTo(${x}f)`);
        break;
      }
      case 'h': {
        const [x] = take(1);
        lines.push(`${indent}horizontalLineToRelative(${x}f)`);
        break;
      }
      case 'V': {
        const [y] = take(1);
        lines.push(`${indent}verticalLineTo(${y}f)`);
        break;
      }
      case 'v': {
        const [y] = take(1);
        lines.push(`${indent}verticalLineToRelative(${y}f)`);
        break;
      }
      case 'C': {
        const [x1, y1, x2, y2, x, y] = take(6);
        lines.push(`${indent}curveTo(${x1}f, ${y1}f, ${x2}f, ${y2}f, ${x}f, ${y}f)`);
        break;
      }
      case 'c': {
        const [x1, y1, x2, y2, x, y] = take(6);
        lines.push(
          `${indent}curveToRelative(${x1}f, ${y1}f, ${x2}f, ${y2}f, ${x}f, ${y}f)`,
        );
        break;
      }
      case 'Q': {
        const [x1, y1, x, y] = take(4);
        lines.push(`${indent}quadTo(${x1}f, ${y1}f, ${x}f, ${y}f)`);
        break;
      }
      case 'q': {
        const [x1, y1, x, y] = take(4);
        lines.push(`${indent}quadToRelative(${x1}f, ${y1}f, ${x}f, ${y}f)`);
        break;
      }
      case 'A':
      case 'a': {
        const [rx, ry, rot, large, sweep, x, y] = take(7);
        const rel = cmd === 'a';
        const fn = rel ? 'arcToRelative' : 'arcTo';
        lines.push(
          `${indent}${fn}(${rx}f, ${ry}f, ${rot}f, ${large === 1}, ${sweep === 1}, ${x}f, ${y}f)`,
        );
        break;
      }
      case 'S': {
        const [x2, y2, x, y] = take(4);
        lines.push(`${indent}reflectiveCurveTo(${x2}f, ${y2}f, ${x}f, ${y}f)`);
        break;
      }
      case 's': {
        const [x2, y2, x, y] = take(4);
        lines.push(`${indent}reflectiveCurveToRelative(${x2}f, ${y2}f, ${x}f, ${y}f)`);
        break;
      }
      default:
        // Skip unknown leftover numbers
        if (tokens[i]?.type === 'num') i++;
        else return lines.join('\n');
    }
  }
  return lines.join('\n');
}

function kotlinName(name) {
  return name
    .split('-')
    .map((p, i) => (i === 0 ? p : p[0].toUpperCase() + p.slice(1)))
    .join('');
}

function toComposeFile() {
  const props = [];
  for (const name of ICON_NAMES) {
    const def = ICONS[name];
    const prop = kotlinName(name);
    const builders = [];
    for (const d of def.paths || []) {
      builders.push(`            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent),
            ) {
${pathToComposeBuilder(d)}
            }`);
    }
    for (const c of def.circles || []) {
      const r = c.r;
      const d = `M${c.cx - r},${c.cy}a${r},${r} 0 1,0 ${r * 2},0a${r},${r} 0 1,0 -${r * 2},0`;
      if (c.fill) {
        builders.push(`            path(fill = SolidColor(Color.Black)) {
${pathToComposeBuilder(d)}
            }`);
      } else {
        builders.push(`            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                fill = SolidColor(Color.Transparent),
            ) {
${pathToComposeBuilder(d)}
            }`);
      }
    }
    for (const l of def.lines || []) {
      const d = `M${l.x1},${l.y1}L${l.x2},${l.y2}`;
      builders.push(`            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                fill = SolidColor(Color.Transparent),
            ) {
${pathToComposeBuilder(d)}
            }`);
    }
    props.push(`    public val ${prop}: ImageVector
        get() {
            if (_${prop} != null) return _${prop}!!
            _${prop} = ImageVector.Builder(
                name = "OmniGart.${prop}",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
${builders.join('\n')}
            }.build()
            return _${prop}!!
        }
    private var _${prop}: ImageVector? = null
`);
  }

  return `// Generated by design/tools/generate-icons.mjs — do not edit.
package com.omnigart.design.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public object OmniGartIcons {
${props.join('\n')}
}
`;
}

function writeIosImageset(name, svg) {
  const dir = path.join(IOS_DIR, `${name}.imageset`);
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, `${name}.svg`), svg);
  const contents = {
    images: [
      {
        filename: `${name}.svg`,
        idiom: 'universal',
      },
    ],
    info: { author: 'omnigart', version: 1 },
    properties: {
      'preserves-vector-representation': true,
      'template-rendering-intent': 'template',
    },
  };
  fs.writeFileSync(path.join(dir, 'Contents.json'), JSON.stringify(contents, null, 2) + '\n');
}

function validateSvg(svg, name) {
  if (!svg.includes('viewBox="0 0 24 24"')) {
    throw new Error(`${name}: missing 24x24 viewBox`);
  }
  if (!svg.includes('<svg')) throw new Error(`${name}: not an SVG`);
  // Disallow external refs / scripts
  if (/<script|xlink:href|foreignObject/i.test(svg)) {
    throw new Error(`${name}: disallowed SVG content`);
  }
}

async function renderContactSheet() {
  const cols = 8;
  const cell = 96;
  const pad = 16;
  const labelH = 22;
  const rows = Math.ceil(ICON_NAMES.length / cols);
  const width = cols * cell + pad * 2;
  const height = rows * (cell + labelH) + pad * 2 + 40;

  const tiles = [];
  for (let i = 0; i < ICON_NAMES.length; i++) {
    const name = ICON_NAMES[i];
    const col = i % cols;
    const row = Math.floor(i / cols);
    const svgPath = path.join(SVG_DIR, `${name}.svg`);
    let svg = fs.readFileSync(svgPath, 'utf8');
    // resvg needs concrete colour
    svg = svg.replaceAll('currentColor', '#0F172A');
    const resvg = new Resvg(svg, {
      fitTo: { mode: 'width', value: 56 },
      background: 'transparent',
    });
    const png = resvg.render().asPng();
    const x = pad + col * cell + (cell - 56) / 2;
    const y = pad + 36 + row * (cell + labelH) + 8;
    tiles.push({ input: png, left: Math.round(x), top: Math.round(y) });
  }

  // Labels as SVG overlay
  let labels = '';
  for (let i = 0; i < ICON_NAMES.length; i++) {
    const name = ICON_NAMES[i];
    const col = i % cols;
    const row = Math.floor(i / cols);
    const x = pad + col * cell + cell / 2;
    const y = pad + 36 + row * (cell + labelH) + 72;
    labels += `<text x="${x}" y="${y}" text-anchor="middle" font-family="ui-sans-serif, system-ui, sans-serif" font-size="10" fill="#475569">${name}</text>\n`;
  }
  const header = `<text x="${pad}" y="28" font-family="ui-sans-serif, system-ui, sans-serif" font-size="18" font-weight="600" fill="#0F172A">OmniGart Icons (${ICON_NAMES.length})</text>`;
  const sheetSvg = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}">
  <rect width="100%" height="100%" fill="#F8FAFC"/>
  ${header}
  ${labels}
</svg>`;

  const base = await sharp(Buffer.from(sheetSvg)).png().toBuffer();
  const out = await sharp(base).composite(tiles).png().toBuffer();
  const previewPath = path.join(ROOT, 'icons', 'preview.png');
  fs.writeFileSync(previewPath, out);
  return previewPath;
}

function writeManifest() {
  const icons = ICON_NAMES.map((name) => ({
    name,
    file: `svg/${name}.svg`,
    category: ICONS[name].category,
    keywords: ICONS[name].keywords,
  }));
  const manifest = {
    version: 1,
    grid: '24x24',
    strokeWidthPx: 1.75,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
    style: 'minimal-line',
    count: icons.length,
    icons,
  };
  fs.writeFileSync(
    path.join(ROOT, 'icons', 'manifest.json'),
    JSON.stringify(manifest, null, 2) + '\n',
  );
}

async function main() {
  fs.mkdirSync(SVG_DIR, { recursive: true });
  fs.mkdirSync(ANDROID_DIR, { recursive: true });
  fs.mkdirSync(COMPOSE_DIR, { recursive: true });
  fs.mkdirSync(IOS_DIR, { recursive: true });

  for (const name of ICON_NAMES) {
    const def = ICONS[name];
    const svg = toSvg(name, def);
    validateSvg(svg, name);
    fs.writeFileSync(path.join(SVG_DIR, `${name}.svg`), svg);
    fs.writeFileSync(path.join(ANDROID_DIR, `${androidName(name)}.xml`), toVectorDrawable(name, def));
    writeIosImageset(name, svg);
  }

  fs.writeFileSync(
    path.join(IOS_DIR, 'Contents.json'),
    JSON.stringify({ info: { author: 'omnigart', version: 1 } }, null, 2) + '\n',
  );
  fs.writeFileSync(path.join(COMPOSE_DIR, 'OmniGartIcons.kt'), toComposeFile());
  writeManifest();
  const preview = await renderContactSheet();
  console.log(`Generated ${ICON_NAMES.length} icons`);
  console.log(`Preview: ${preview}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});

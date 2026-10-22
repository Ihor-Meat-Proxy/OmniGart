#!/usr/bin/env node
/**
 * Render exercise body-map previews and a contact sheet of all exercises.
 * Primary muscles: strong accent fill; secondary: light accent; idle: neutral.
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { Resvg } from '@resvg/resvg-js';
import sharp from 'sharp';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..');
const REPO = path.resolve(ROOT, '../..');
const SVG_DIR = path.join(ROOT, 'svg');
const OUT = path.join(ROOT, 'generated', 'previews');
const SAMPLES = path.join(OUT, 'samples');
const EXERCISES_OUT = path.join(OUT, 'exercises');
const FIGURES_OUT = path.join(OUT, 'figures');
const MAPPING = JSON.parse(
  fs.readFileSync(path.join(ROOT, 'mapping', 'taxonomy-to-regions.json'), 'utf8'),
);
const SCALE = JSON.parse(
  fs.readFileSync(path.join(ROOT, 'mapping', 'intensity-scale.json'), 'utf8'),
);
const EXERCISE_DIR = path.join(REPO, 'content', 'exercises', 'data');

const SAMPLE_IDS = [
  'back-squat',
  'barbell-bench-press',
  'conventional-deadlift',
  'pull-up',
];

function loadExercises() {
  return fs
    .readdirSync(EXERCISE_DIR)
    .filter((f) => f.endsWith('.json'))
    .map((f) => JSON.parse(fs.readFileSync(path.join(EXERCISE_DIR, f), 'utf8')))
    .sort((a, b) => a.id.localeCompare(b.id));
}

function expandRegions(muscleIds) {
  const regions = new Set();
  const views = new Set();
  for (const m of muscleIds) {
    const entry = MAPPING.mappings[m];
    if (!entry) continue;
    for (const v of entry.views || []) views.add(v);
    if (entry.regions.includes('*')) {
      regions.add('*');
    } else {
      for (const r of entry.regions) regions.add(r);
    }
  }
  return { regions, views };
}

function chooseViews(primary, secondary) {
  const { views: pv } = expandRegions(primary);
  const { views: sv } = expandRegions(secondary);
  const all = new Set([...pv, ...sv]);
  if (all.has('front') && all.has('back')) return ['front', 'back'];
  if (all.has('back')) return ['back'];
  return ['front'];
}

function tintSvg(baseSvg, primaryRegions, secondaryRegions, theme = 'light') {
  const idle = SCALE.idle.hex[theme];
  const outline = SCALE.outline.hex[theme];
  const primary = SCALE.primaryHighlight.hex[theme];
  const secondary = SCALE.secondaryHighlight.hex[theme];
  const secondaryOpacity = SCALE.secondaryHighlight.opacity;
  const canvas = theme === 'light' ? '#F8FAFC' : '#0B1220';

  let svg = baseSvg.replace(
    /fill="[^"]+"(\s+stroke="[^"]+"\s+stroke-width="[^"]+"\s+stroke-linejoin="round"\/>)/g,
    (full, rest, offset, whole) => full,
  );

  // Replace canvas
  svg = svg.replace(/id="canvas"[^>]*>/, (m) =>
    m.replace(/fill="[^"]+"/, `fill="${canvas}"`),
  );

  // Set all muscle fills to idle first via attribute rewrite per path id
  svg = svg.replace(
    /<path\b([^>]*?)id="([^"]+)"([^>]*?)\/>/g,
    (full, before, id, after) => {
      const attrs = before + after;
      if (id === 'canvas') return full;
      let fill = idle;
      let opacity = '1';
      if (id === 'body_outline') {
        fill = idle;
        opacity = '0.35';
      } else if (primaryRegions.has('*') || primaryRegions.has(id)) {
        fill = primary;
        opacity = '1';
      } else if (secondaryRegions.has('*') || secondaryRegions.has(id)) {
        fill = secondary;
        opacity = String(secondaryOpacity);
      }
      let next = full
        .replace(/\sfill="[^"]*"/, '')
        .replace(/\sopacity="[^"]*"/, '')
        .replace(/\sstroke="[^"]*"/, '');
      next = next.replace(
        /\/>$/,
        ` fill="${fill}" stroke="${outline}" opacity="${opacity}"/>`,
      );
      return next;
    },
  );
  return svg;
}

function renderPng(svg, outPath, width = 400) {
  const resvg = new Resvg(svg, {
    fitTo: { mode: 'width', value: width },
    background: 'transparent',
  });
  const png = resvg.render().asPng();
  fs.mkdirSync(path.dirname(outPath), { recursive: true });
  fs.writeFileSync(outPath, png);
}

async function composeDual(frontPng, backPng, outPath, label) {
  // Side-by-side front | back with label bar
  const w = 360;
  const h = 432;
  const canvasW = w * 2 + 24;
  const canvasH = h + 48;
  const frontBuf = await sharp(frontPng)
    .resize(w, h, { fit: 'contain', background: { r: 248, g: 250, b: 252, alpha: 255 } })
    .png()
    .toBuffer();
  const backBuf = await sharp(backPng)
    .resize(w, h, { fit: 'contain', background: { r: 248, g: 250, b: 252, alpha: 255 } })
    .png()
    .toBuffer();
  const labelBuf = Buffer.from(
    `<svg width="${canvasW}" height="36" xmlns="http://www.w3.org/2000/svg">
      <rect width="100%" height="100%" fill="#F8FAFC"/>
      <text x="12" y="24" font-family="sans-serif" font-size="16" fill="#0F172A">${escapeXml(label)}</text>
      <text x="${w + 20}" y="24" font-family="sans-serif" font-size="12" fill="#64748B">front</text>
      <text x="${canvasW - 48}" y="24" font-family="sans-serif" font-size="12" fill="#64748B">back</text>
    </svg>`,
  );
  await sharp({
    create: {
      width: canvasW,
      height: canvasH,
      channels: 4,
      background: { r: 248, g: 250, b: 252, alpha: 255 },
    },
  })
    .composite([
      { input: labelBuf, left: 0, top: 0 },
      { input: frontBuf, left: 8, top: 40 },
      { input: backBuf, left: w + 16, top: 40 },
    ])
    .png()
    .toFile(outPath);
}

function escapeXml(s) {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

async function renderExercise(ex, sex = 'male', theme = 'light') {
  const views = chooseViews(ex.primaryMuscles, ex.secondaryMuscles);
  const { regions: primary } = expandRegions(ex.primaryMuscles);
  const { regions: secondary } = expandRegions(ex.secondaryMuscles);
  // Secondary should not override primary
  for (const r of primary) secondary.delete(r);

  const pngs = [];
  for (const view of ['front', 'back']) {
    const base = fs.readFileSync(path.join(SVG_DIR, `${sex}-${view}.svg`), 'utf8');
    const svg = tintSvg(
      base,
      views.includes(view) ? primary : new Set(),
      views.includes(view) ? secondary : new Set(),
      theme,
    );
    const tmp = path.join(OUT, '_tmp', `${ex.id}-${sex}-${view}.png`);
    renderPng(svg, tmp, 400);
    pngs.push({ view, path: tmp, active: views.includes(view) });
  }

  // Prefer dual when both views; else single
  const outSingle = path.join(EXERCISES_OUT, `${ex.id}.png`);
  if (views.length === 2) {
    await composeDual(
      pngs.find((p) => p.view === 'front').path,
      pngs.find((p) => p.view === 'back').path,
      outSingle,
      ex.id,
    );
  } else {
    const chosen = pngs.find((p) => p.active) || pngs[0];
    await sharp(chosen.path)
      .resize(400, 480, { fit: 'contain', background: { r: 248, g: 250, b: 252, alpha: 255 } })
      .png()
      .toFile(outSingle);
  }
  return { outSingle, views };
}

async function contactSheet(exercisePngs) {
  const cols = 10;
  const thumbW = 160;
  const thumbH = 200;
  const rows = Math.ceil(exercisePngs.length / cols);
  const sheetW = cols * thumbW;
  const sheetH = rows * thumbH;
  const composites = [];
  for (let i = 0; i < exercisePngs.length; i++) {
    const col = i % cols;
    const row = Math.floor(i / cols);
    const buf = await sharp(exercisePngs[i])
      .resize(thumbW, thumbH, { fit: 'contain', background: '#F8FAFC' })
      .png()
      .toBuffer();
    composites.push({ input: buf, left: col * thumbW, top: row * thumbH });
  }
  const out = path.join(OUT, 'contact-sheet-all-exercises.png');
  await sharp({
    create: {
      width: sheetW,
      height: sheetH,
      channels: 4,
      background: { r: 248, g: 250, b: 252, alpha: 255 },
    },
  })
    .composite(composites)
    .png()
    .toFile(out);
  console.log(`Contact sheet → ${out}`);
  return out;
}

async function main() {
  fs.mkdirSync(SAMPLES, { recursive: true });
  fs.mkdirSync(EXERCISES_OUT, { recursive: true });
  fs.mkdirSync(FIGURES_OUT, { recursive: true });
  fs.mkdirSync(path.join(OUT, '_tmp'), { recursive: true });

  // Base figures (individual + large 2×2 untinted sheet)
  const baseBufs = {};
  for (const sex of ['male', 'female']) {
    for (const view of ['front', 'back']) {
      for (const theme of ['light', 'dark']) {
        const themed = path.join(SVG_DIR, 'themed', `${sex}-${view}-${theme}.svg`);
        const svg = fs.readFileSync(themed, 'utf8');
        const outPng = path.join(FIGURES_OUT, `${sex}-${view}-${theme}.png`);
        renderPng(svg, outPng, theme === 'light' ? 480 : 360);
        if (theme === 'light') {
          baseBufs[`${sex}-${view}`] = await sharp(outPng)
            .resize(420, 630, { fit: 'contain', background: { r: 248, g: 250, b: 252, alpha: 255 } })
            .png()
            .toBuffer();
        }
      }
    }
  }
  const sheetW = 420 * 2 + 48;
  const sheetH = 630 * 2 + 72;
  const label = (t) =>
    Buffer.from(
      `<svg width="420" height="28" xmlns="http://www.w3.org/2000/svg"><text x="8" y="20" font-family="sans-serif" font-size="14" fill="#334155">${t}</text></svg>`,
    );
  await sharp({
    create: {
      width: sheetW,
      height: sheetH,
      channels: 4,
      background: { r: 248, g: 250, b: 252, alpha: 255 },
    },
  })
    .composite([
      { input: label('male · front'), left: 12, top: 8 },
      { input: label('male · back'), left: 420 + 36, top: 8 },
      { input: label('female · front'), left: 12, top: 630 + 40 },
      { input: label('female · back'), left: 420 + 36, top: 630 + 40 },
      { input: baseBufs['male-front'], left: 12, top: 36 },
      { input: baseBufs['male-back'], left: 420 + 36, top: 36 },
      { input: baseBufs['female-front'], left: 12, top: 630 + 68 },
      { input: baseBufs['female-back'], left: 420 + 36, top: 630 + 68 },
    ])
    .png()
    .toFile(path.join(FIGURES_OUT, 'base-figures-sheet.png'));
  console.log('Base figures sheet → figures/base-figures-sheet.png');

  const exercises = loadExercises();
  const paths = [];
  for (const ex of exercises) {
    const { outSingle } = await renderExercise(ex, 'male', 'light');
    paths.push(outSingle);
    if (SAMPLE_IDS.includes(ex.id)) {
      const sampleOut = path.join(SAMPLES, `${ex.id}.png`);
      fs.copyFileSync(outSingle, sampleOut);
      // Also dual-theme sample
      await renderExercise(ex, 'female', 'light');
      console.log(`Sample → ${sampleOut}`);
    }
  }

  await contactSheet(paths);

  // Manifest
  const manifest = {
    exerciseCount: exercises.length,
    samples: SAMPLE_IDS.map((id) => `samples/${id}.png`),
    contactSheet: 'contact-sheet-all-exercises.png',
    figures: fs.readdirSync(FIGURES_OUT).map((f) => `figures/${f}`),
  };
  fs.writeFileSync(path.join(OUT, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
  console.log(`Rendered ${exercises.length} exercise previews`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});

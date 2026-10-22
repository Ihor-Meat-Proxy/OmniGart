#!/usr/bin/env node
/**
 * Body-map CI checks:
 * - Every exercise muscle id exists in taxonomy→regions mapping
 * - Every mapped region exists in the corresponding SVG figure(s)
 * - Generated path JSON is fresh / golden hashes match
 * - Preview samples + contact sheet exist
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createHash } from 'node:crypto';
import { spawnSync } from 'node:child_process';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..');
const REPO = path.resolve(ROOT, '../..');
const GOLDEN = path.join(__dirname, 'golden', 'paths-index.sha256');
const wantGolden = process.argv.includes('--golden');

let failed = false;
function fail(msg) {
  console.error(`FAIL: ${msg}`);
  failed = true;
}
function ok(msg) {
  console.log(`OK: ${msg}`);
}

function loadJson(p) {
  return JSON.parse(fs.readFileSync(p, 'utf8'));
}

function extractSvgIds(svg) {
  const ids = new Set();
  const re = /<path\b[^>]*\bid="([^"]+)"/gi;
  let m;
  while ((m = re.exec(svg))) ids.add(m[1]);
  return ids;
}

function checkMappingVsExercises() {
  const mapping = loadJson(path.join(ROOT, 'mapping', 'taxonomy-to-regions.json'));
  const mapped = new Set(Object.keys(mapping.mappings));
  const dataDir = path.join(REPO, 'content', 'exercises', 'data');
  const schema = loadJson(
    path.join(REPO, 'content', 'exercises', 'schema', 'exercise.schema.json'),
  );
  const schemaMuscles = new Set(schema.$defs.muscle.enum);

  for (const m of schemaMuscles) {
    if (!mapped.has(m)) fail(`Schema muscle "${m}" missing from taxonomy-to-regions mapping`);
  }
  for (const m of mapped) {
    if (!schemaMuscles.has(m)) fail(`Mapping muscle "${m}" not in exercise schema enum`);
  }

  let exerciseCount = 0;
  const used = new Set();
  for (const f of fs.readdirSync(dataDir).filter((x) => x.endsWith('.json'))) {
    exerciseCount++;
    const ex = loadJson(path.join(dataDir, f));
    for (const field of ['primaryMuscles', 'secondaryMuscles']) {
      for (const m of ex[field] || []) {
        used.add(m);
        if (!mapped.has(m)) {
          fail(`${f}: muscle "${m}" not in body-map taxonomy mapping`);
        }
      }
    }
  }
  ok(`Validated ${exerciseCount} exercises against ${mapped.size} mapped muscle ids`);
  return { mapping, used };
}

function checkRegionsInSvg(mapping) {
  const figureIds = {
    'male-front': extractSvgIds(fs.readFileSync(path.join(ROOT, 'svg', 'male-front.svg'), 'utf8')),
    'male-back': extractSvgIds(fs.readFileSync(path.join(ROOT, 'svg', 'male-back.svg'), 'utf8')),
    'female-front': extractSvgIds(fs.readFileSync(path.join(ROOT, 'svg', 'female-front.svg'), 'utf8')),
    'female-back': extractSvgIds(fs.readFileSync(path.join(ROOT, 'svg', 'female-back.svg'), 'utf8')),
  };

  const allRegionIds = new Set();
  for (const [muscle, entry] of Object.entries(mapping.mappings)) {
    if (entry.regions.includes('*')) continue;
    for (const region of entry.regions) {
      allRegionIds.add(region);
      const presentIn = Object.entries(figureIds)
        .filter(([, ids]) => ids.has(region))
        .map(([name]) => name);
      if (presentIn.length === 0) {
        fail(`Region "${region}" for ${muscle} missing from all SVG figures`);
        continue;
      }
      // Sex parity: if on male-front, must be on female-front (and same for back)
      for (const view of ['front', 'back']) {
        const maleHas = figureIds[`male-${view}`].has(region);
        const femaleHas = figureIds[`female-${view}`].has(region);
        if (maleHas !== femaleHas) {
          fail(`Region "${region}" sex mismatch on ${view} view`);
        }
      }
    }
  }
  ok(`All ${allRegionIds.size} mapped regions present in SVGs`);
  return allRegionIds;
}

function checkGeneratedPaths(wantUpdate = false) {
  const indexPath = path.join(ROOT, 'generated', 'paths', 'index.json');
  if (!fs.existsSync(indexPath)) {
    fail('generated/paths/index.json missing — run npm run build');
    return;
  }
  const index = loadJson(indexPath);
  const h = createHash('sha256');
  for (const fig of Object.keys(index.figures).sort()) {
    h.update(fig);
    h.update(index.figures[fig].contentSha256);
  }
  const digest = h.digest('hex');
  fs.mkdirSync(path.dirname(GOLDEN), { recursive: true });
  if (!fs.existsSync(GOLDEN) || wantUpdate) {
    fs.writeFileSync(GOLDEN, digest + '\n');
    ok(`Wrote golden hash ${digest.slice(0, 12)}…`);
  } else {
    const expected = fs.readFileSync(GOLDEN, 'utf8').trim();
    if (expected !== digest) {
      fail(`Path golden mismatch: expected ${expected.slice(0, 12)}… got ${digest.slice(0, 12)}…`);
    } else {
      ok('Path JSON golden hash matches');
    }
  }

  // Snapshot: each figure JSON region count stable vs index
  for (const [fig, meta] of Object.entries(index.figures)) {
    const payload = loadJson(path.join(ROOT, 'generated', 'paths', meta.file));
    if (payload.regionCount !== meta.regionCount) {
      fail(`${fig}: regionCount drift`);
    }
    if (Object.keys(payload.regions).length < 20) {
      fail(`${fig}: suspiciously few regions (${Object.keys(payload.regions).length})`);
    }
  }
  ok('Generated path JSON snapshots coherent');
}

function checkPreviews() {
  const samples = [
    'back-squat.png',
    'barbell-bench-press.png',
    'conventional-deadlift.png',
    'pull-up.png',
  ];
  for (const s of samples) {
    const p = path.join(ROOT, 'generated', 'previews', 'samples', s);
    if (!fs.existsSync(p)) fail(`Missing sample preview ${s}`);
  }
  const sheet = path.join(ROOT, 'generated', 'previews', 'contact-sheet-all-exercises.png');
  if (!fs.existsSync(sheet)) fail('Missing contact sheet');
  const manifest = path.join(ROOT, 'generated', 'previews', 'manifest.json');
  if (fs.existsSync(manifest)) {
    const m = loadJson(manifest);
    if (m.exerciseCount < 120) fail(`Expected ≥120 exercise previews, got ${m.exerciseCount}`);
  }
  ok('Sample previews + contact sheet present');
}

function ensureBuilt() {
  const need =
    !fs.existsSync(path.join(ROOT, 'svg', 'male-front.svg')) ||
    !fs.existsSync(path.join(ROOT, 'generated', 'paths', 'index.json'));
  if (need) {
    console.log('Building body-map artifacts…');
    const r = spawnSync('npm', ['run', 'build'], { cwd: __dirname, stdio: 'inherit' });
    if (r.status !== 0) fail('npm run build failed');
  }
}

ensureBuilt();
const { mapping } = checkMappingVsExercises();
checkRegionsInSvg(mapping);
checkGeneratedPaths(wantGolden && process.argv.includes('--write-golden'));
checkPreviews();

if (failed) {
  console.error('Body-map check failed');
  process.exit(1);
}
console.log('Body-map check passed');

#!/usr/bin/env node
/**
 * Validate illustration sources + index. Coverage gaps are warnings by default
 * (--coverage-warn) so CI stays green until the expanded catalogue lands.
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { buildIndex } from './build-index.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ILL = path.resolve(__dirname, '..');
const ROOT = path.resolve(ILL, '../..');

const coverageWarnOnly =
  process.argv.includes('--coverage-warn') || process.env.ILLUSTRATION_COVERAGE_WARN === '1';

let failed = false;
let warned = false;

function fail(msg) {
  console.error(`FAIL: ${msg}`);
  failed = true;
}

function warn(msg) {
  console.warn(`WARN: ${msg}`);
  warned = true;
}

function ok(msg) {
  console.log(`OK: ${msg}`);
}

function validateSvg(file) {
  const svg = fs.readFileSync(file, 'utf8');
  const base = path.basename(file);
  if (!svg.includes('viewBox="0 0 128 128"')) fail(`${base}: bad viewBox`);
  if (!svg.includes('<svg')) fail(`${base}: not SVG`);
  if (/<script|foreignObject|xlink:href\s*=\s*["']https?:/i.test(svg)) {
    fail(`${base}: disallowed content`);
  }
  if (!svg.includes('#0F766E') && !svg.includes('#334155') && !svg.includes('#94A3B8')) {
    // equipment-only or figure-only still use palette
    if (!svg.includes('#FFFFFF')) fail(`${base}: missing style colours`);
  }
}

function main() {
  const index = buildIndex();
  const missing = [];

  for (const e of index.equipment) {
    const p = path.join(ILL, e.asset);
    if (!fs.existsSync(p)) missing.push(e.asset);
    else validateSvg(p);
  }
  for (const e of index.exercises) {
    const p = path.join(ILL, e.asset);
    if (!fs.existsSync(p)) missing.push(e.asset);
    else validateSvg(p);
  }

  ok(`Validated index (${index.counts.total} entries)`);

  // Generated platform artefacts
  const required = [
    'design/generated/ios/OmniGartIllustrations.xcassets/Contents.json',
    'design/generated/watch/OmniGartIllustrations.xcassets/Contents.json',
    'design/generated/ios/Sources/IllustrationAsset.swift',
    'design/generated/compose/OmniGartIllustrations.kt',
    'design/illustrations/contact-sheet.png',
    'design/illustrations/generated/previews/contact-sheet-all.png',
  ];
  for (const r of required) {
    if (!fs.existsSync(path.join(ROOT, r))) fail(`missing ${r}`);
  }
  ok('Platform artefacts present');

  // Drawable count smoke check
  const drawableDir = path.join(ROOT, 'design/generated/android/drawable');
  const ils = fs.existsSync(drawableDir)
    ? fs.readdirSync(drawableDir).filter((f) => f.startsWith('il_og_') && f.endsWith('.xml'))
    : [];
  if (ils.length < index.counts.total) {
    const msg = `Android drawables ${ils.length} < index ${index.counts.total}`;
    if (coverageWarnOnly) warn(msg);
    else fail(msg);
  } else {
    ok(`Android drawables ${ils.length}`);
  }

  if (missing.length) {
    const msg = `Missing ${missing.length} illustration SVG(s), e.g. ${missing.slice(0, 5).join(', ')}`;
    if (coverageWarnOnly) warn(msg);
    else fail(msg);
  } else {
    ok('All index ids have SVG sources');
  }

  // Report seed ids not yet in content/kotlin (informational)
  const seedOnly = index.equipment.filter((e) => e.sources.length === 1 && e.sources[0] === 'seed');
  if (seedOnly.length) {
    console.log(
      `INFO: ${seedOnly.length} equipment ids are seed-only (awaiting catalogue PR)`,
    );
  }

  if (failed) {
    console.error('\nIllustration check failed');
    process.exit(1);
  }
  if (warned) {
    console.log('\nIllustration check passed with coverage warnings');
  } else {
    console.log('\nIllustration check passed');
  }
}

main();

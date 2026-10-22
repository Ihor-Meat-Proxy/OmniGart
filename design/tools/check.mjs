#!/usr/bin/env node
/**
 * CI check: SVG validity, generated outputs up-to-date, icon count / manifest sync.
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createHash } from 'node:crypto';
import { spawnSync } from 'node:child_process';
import { ICON_NAMES } from './iconsource.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..');
let failed = false;

function fail(msg) {
  console.error(`FAIL: ${msg}`);
  failed = true;
}

function ok(msg) {
  console.log(`OK: ${msg}`);
}

function hashDir(dir, filter = () => true) {
  if (!fs.existsSync(dir)) return '';
  const files = [];
  function walk(d) {
    for (const ent of fs.readdirSync(d, { withFileTypes: true })) {
      const p = path.join(d, ent.name);
      if (ent.isDirectory()) walk(p);
      else if (filter(p)) files.push(p);
    }
  }
  walk(dir);
  files.sort();
  const h = createHash('sha256');
  for (const f of files) {
    h.update(path.relative(ROOT, f));
    h.update(fs.readFileSync(f));
  }
  return h.digest('hex');
}

function validateSvgs() {
  const svgDir = path.join(ROOT, 'icons', 'svg');
  const files = fs.readdirSync(svgDir).filter((f) => f.endsWith('.svg'));
  if (files.length !== ICON_NAMES.length) {
    fail(`SVG count ${files.length} != source count ${ICON_NAMES.length}`);
  }
  for (const f of files) {
    const svg = fs.readFileSync(path.join(svgDir, f), 'utf8');
    const name = f.replace(/\.svg$/, '');
    if (!ICON_NAMES.includes(name)) fail(`Unexpected SVG ${f}`);
    if (!svg.includes('viewBox="0 0 24 24"')) fail(`${f}: bad viewBox`);
    if (/<script|foreignObject|xlink:href\s*=\s*["']https?:/i.test(svg)) {
      fail(`${f}: disallowed content`);
    }
    if (!svg.includes('stroke-linecap="round"') && !svg.includes('<circle')) {
      // lines/paths should use round caps; filled-only ok
    }
  }
  ok(`Validated ${files.length} SVG sources`);
}

function checkManifest() {
  const manifest = JSON.parse(fs.readFileSync(path.join(ROOT, 'icons', 'manifest.json'), 'utf8'));
  if (manifest.count !== ICON_NAMES.length) fail('manifest count mismatch');
  const names = new Set(manifest.icons.map((i) => i.name));
  for (const n of ICON_NAMES) {
    if (!names.has(n)) fail(`manifest missing ${n}`);
  }
  ok('Manifest synced');
}

function checkGeneratedPresent() {
  const required = [
    'generated/compose/OmniGartIcons.kt',
    'generated/compose/OmniGartTheme.kt',
    'generated/ios/Sources/Color+OmniGart.swift',
    'generated/ios/Sources/Font+OmniGart.swift',
    'generated/ios/Sources/OmniGartMetrics.swift',
    'generated/android/values/og_colors.xml',
    'generated/android/values/og_dimens.xml',
    'icons/preview.png',
    'logo/omnigart-mark.svg',
    'logo/preview-variants.png',
  ];
  for (const r of required) {
    if (!fs.existsSync(path.join(ROOT, r))) fail(`missing ${r}`);
  }
  ok('Required generated artefacts present');
}

function checkFreshness() {
  // Re-run generators into a temp copy via hashing before/after regenerate
  const beforeIcons = hashDir(path.join(ROOT, 'icons', 'svg'));
  const beforeAndroid = hashDir(path.join(ROOT, 'generated', 'android', 'drawable'));
  const beforeComposeIcons = createHash('sha256')
    .update(fs.readFileSync(path.join(ROOT, 'generated', 'compose', 'OmniGartIcons.kt')))
    .digest('hex');
  const beforeTokensSwift = hashDir(path.join(ROOT, 'generated', 'ios', 'Sources'));

  const run = (script) => {
    const r = spawnSync(process.execPath, [path.join(__dirname, script)], {
      cwd: __dirname,
      encoding: 'utf8',
    });
    if (r.status !== 0) {
      fail(`${script} failed: ${r.stderr || r.stdout}`);
      return false;
    }
    return true;
  };

  if (!run('generate-icons.mjs')) return;
  if (!run('style-dictionary.config.mjs')) return;

  const afterIcons = hashDir(path.join(ROOT, 'icons', 'svg'));
  const afterAndroid = hashDir(path.join(ROOT, 'generated', 'android', 'drawable'));
  const afterComposeIcons = createHash('sha256')
    .update(fs.readFileSync(path.join(ROOT, 'generated', 'compose', 'OmniGartIcons.kt')))
    .digest('hex');
  const afterTokensSwift = hashDir(path.join(ROOT, 'generated', 'ios', 'Sources'));

  if (beforeIcons !== afterIcons) fail('icon SVGs drifted — re-run generate-icons and commit');
  if (beforeAndroid !== afterAndroid) fail('Android drawables out of date');
  if (beforeComposeIcons !== afterComposeIcons) fail('Compose OmniGartIcons.kt out of date');
  if (beforeTokensSwift !== afterTokensSwift) fail('iOS token Sources out of date');
  ok('Generated outputs are up to date');
}

validateSvgs();
checkManifest();
checkGeneratedPresent();
checkFreshness();

if (failed) {
  console.error('\nDesign check failed');
  process.exit(1);
}
console.log('\nDesign check passed');

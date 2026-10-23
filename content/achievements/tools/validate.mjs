#!/usr/bin/env node
/**
 * Achievements content validator for CI.
 * Checks: JSON Schema, unique ids, locale keys (en-GB + uk), icon refs, rule DSL.
 */
import { readFileSync, readdirSync, existsSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import Ajv2020 from 'ajv/dist/2020.js';
import addFormats from 'ajv-formats';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = join(__dirname, '..');
const REPO = join(ROOT, '..', '..');

const issues = [];

function fail(path, message) {
  issues.push({ path, message });
}

function loadJson(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}

const schema = loadJson(join(ROOT, 'schema', 'achievement.schema.json'));
const ajv = new Ajv2020({ allErrors: true, strict: false });
addFormats(ajv);
const validate = ajv.compile(schema);

const localeEn = loadJson(join(ROOT, 'locales', 'en-GB.json'));
const localeUk = loadJson(join(ROOT, 'locales', 'uk.json'));

for (const key of Object.keys(localeEn)) {
  if (!(key in localeUk)) fail('locales/uk.json', `Missing key: ${key}`);
}
for (const key of Object.keys(localeUk)) {
  if (!(key in localeEn)) fail('locales/en-GB.json', `Missing key: ${key}`);
}

// Hard fail on Russian locale artefacts
const ruPath = join(ROOT, 'locales', 'ru.json');
if (existsSync(ruPath)) {
  fail('locales/ru.json', 'Russian locale must not exist');
}

const designManifestPath = join(REPO, 'design', 'icons', 'manifest.json');
const designIcons = new Set();
if (existsSync(designManifestPath)) {
  const manifest = loadJson(designManifestPath);
  for (const icon of manifest.icons ?? []) designIcons.add(icon.name);
} else {
  fail(designManifestPath, 'design/icons/manifest.json not found');
}

const proposedPath = join(ROOT, 'proposed-icons.json');
const proposedIcons = new Set();
if (existsSync(proposedPath)) {
  const proposed = loadJson(proposedPath);
  for (const icon of proposed.icons ?? []) proposedIcons.add(icon.name);
}

const allowedIcons = new Set([...designIcons, ...proposedIcons]);

const FORBIDDEN_METRICS = new Set([
  'calories_below',
  'calorie_deficit',
  'weight_loss_kg',
  'weight_loss_rate',
  'bmi_decrease',
  'dehydration',
]);

const dataDir = join(ROOT, 'data');
const files = readdirSync(dataDir).filter((f) => f.endsWith('.json'));
const ids = new Set();
let count = 0;
const byCategory = {};

for (const file of files) {
  const path = join(dataDir, file);
  const rows = loadJson(path);
  if (!Array.isArray(rows)) {
    fail(path, 'Expected array of achievements');
    continue;
  }
  for (const row of rows) {
    count += 1;
    byCategory[row.category] = (byCategory[row.category] ?? 0) + 1;

    if (!validate(row)) {
      for (const err of validate.errors ?? []) {
        fail(path, `${row.id ?? '?'}: ${err.instancePath} ${err.message}`);
      }
    }

    if (!row.id) {
      fail(path, 'Missing id');
      continue;
    }
    if (ids.has(row.id)) fail(path, `Duplicate id: ${row.id}`);
    ids.add(row.id);

    if (row.titleKey && !(row.titleKey in localeEn)) {
      fail(path, `${row.id}: missing en-GB titleKey ${row.titleKey}`);
    }
    if (row.descriptionKey && !(row.descriptionKey in localeEn)) {
      fail(path, `${row.id}: missing en-GB descriptionKey ${row.descriptionKey}`);
    }
    if (row.titleKey && !(row.titleKey in localeUk)) {
      fail(path, `${row.id}: missing uk titleKey ${row.titleKey}`);
    }
    if (row.descriptionKey && !(row.descriptionKey in localeUk)) {
      fail(path, `${row.id}: missing uk descriptionKey ${row.descriptionKey}`);
    }

    if (row.iconId && !allowedIcons.has(row.iconId)) {
      fail(
        path,
        `${row.id}: unknown iconId "${row.iconId}" (not in design manifest or proposed-icons.json)`,
      );
    }

    // Rule DSL sanity
    const rule = row.rule;
    if (rule) {
      if (FORBIDDEN_METRICS.has(rule.metric)) {
        fail(path, `${row.id}: forbidden metric ${rule.metric}`);
      }
      if (rule.window?.kind === 'streak_days' && !rule.window.qualify) {
        fail(path, `${row.id}: streak_days requires qualify`);
      }
      if (rule.window?.kind === 'rolling_days' && !rule.window.days) {
        fail(path, `${row.id}: rolling_days requires days`);
      }
      if (rule.aggregation === 'streak' && rule.window?.kind !== 'streak_days') {
        fail(path, `${row.id}: aggregation streak requires window.kind streak_days`);
      }
      // Healthy body rules
      if (row.category === 'body') {
        if (rule.filters?.excludeWeightLossRate !== true) {
          fail(path, `${row.id}: body achievements must set filters.excludeWeightLossRate=true`);
        }
        if (['volume_kg', 'pace_sec_per_km'].includes(rule.metric)) {
          fail(path, `${row.id}: body category must not use performance metrics`);
        }
      }
      if (row.category === 'nutrition' && rule.metric !== 'food_days_logged') {
        fail(path, `${row.id}: nutrition achievements may only use food_days_logged`);
      }
      if (row.category === 'water' && rule.metric !== 'water_goal_days') {
        fail(path, `${row.id}: water achievements may only use water_goal_days (goal met)`);
      }
    }

    // Tier thresholds must be strictly increasing
    if (Array.isArray(row.tiers) && row.tiers.length > 1) {
      for (let i = 1; i < row.tiers.length; i++) {
        if (!(row.tiers[i].threshold > row.tiers[i - 1].threshold)) {
          fail(path, `${row.id}: tier thresholds must be strictly increasing`);
        }
      }
    }
  }
}

const catalogue = loadJson(join(ROOT, 'catalogue.json'));
if (catalogue.count !== count) {
  fail('catalogue.json', `count ${catalogue.count} != actual ${count}`);
}

console.log(`Validated ${count} achievements across ${files.length} files`);
console.log('Per category:', byCategory);

if (issues.length) {
  console.error(`\n${issues.length} issue(s):`);
  for (const issue of issues) {
    console.error(` - ${issue.path}: ${issue.message}`);
  }
  process.exit(1);
}

console.log('OK');

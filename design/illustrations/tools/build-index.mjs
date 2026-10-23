#!/usr/bin/env node
/**
 * Build design/illustrations/index.json from:
 *   - content/exercises/data/*.json (exercise ids + equipment tags)
 *   - EquipmentCatalogue.kt ids (when present)
 *   - equipment-catalogue-seed.json (~150-item parallel catalogue)
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ILL = path.resolve(__dirname, '..');
const ROOT = path.resolve(ILL, '../..');

function readJson(p) {
  return JSON.parse(fs.readFileSync(p, 'utf8'));
}

function exerciseIds() {
  const dir = path.join(ROOT, 'content/exercises/data');
  return fs
    .readdirSync(dir)
    .filter((f) => f.endsWith('.json'))
    .map((f) => f.replace(/\.json$/, ''))
    .sort();
}

function equipmentFromExercises() {
  const dir = path.join(ROOT, 'content/exercises/data');
  const set = new Set();
  for (const f of fs.readdirSync(dir).filter((x) => x.endsWith('.json'))) {
    const data = readJson(path.join(dir, f));
    for (const e of data.equipment || []) {
      if (typeof e === 'string') set.add(e);
      else if (e?.id) set.add(e.id);
    }
  }
  return set;
}

function equipmentFromKotlin() {
  const kt = path.join(
    ROOT,
    'shared/src/commonMain/kotlin/com/omnigart/shared/strength/EquipmentCatalogue.kt',
  );
  if (!fs.existsSync(kt)) return new Set();
  const text = fs.readFileSync(kt, 'utf8');
  const ids = new Set();
  for (const m of text.matchAll(/"([a-z][a-z0-9_]*)"/g)) {
    ids.add(m[1]);
  }
  return ids;
}

function equipmentFromSeed() {
  const seed = readJson(path.join(ILL, 'equipment-catalogue-seed.json'));
  return new Map(seed.items.map((i) => [i.id, i]));
}

export function buildIndex() {
  const exercises = exerciseIds();
  const seed = equipmentFromSeed();
  const fromContent = equipmentFromExercises();
  const fromKt = equipmentFromKotlin();

  const equipmentIds = new Set([...seed.keys(), ...fromContent, ...fromKt]);

  const equipment = [...equipmentIds].sort().map((id) => {
    const meta = seed.get(id) || {
      id,
      category: 'accessories',
      shape: 'generic_machine',
    };
    return {
      id,
      kind: 'equipment',
      asset: `equipment/equip_${id}.svg`,
      category: meta.category || 'accessories',
      shape: meta.shape || 'generic_machine',
      sources: [
        seed.has(id) ? 'seed' : null,
        fromContent.has(id) ? 'content' : null,
        fromKt.has(id) ? 'catalogue' : null,
      ].filter(Boolean),
    };
  });

  const exerciseEntries = exercises.map((id) => {
    const data = readJson(path.join(ROOT, 'content/exercises/data', `${id}.json`));
    return {
      id,
      kind: 'exercise',
      asset: `exercises/ex_${id}.svg`,
      movementPattern: data.movementPattern || 'other',
      primaryMuscles: data.primaryMuscles || [],
      secondaryMuscles: data.secondaryMuscles || [],
      equipment: data.equipment || [],
      illustrationKey: data.media?.illustrationKey || `media/illustrations/${id}`,
    };
  });

  const index = {
    version: 1,
    style: {
      viewBox: '0 0 128 128',
      accent: '#0F766E',
      figure: '#94A3B8',
      equipment: '#334155',
      badge: '#FFFFFF',
      strokeWidth: 2.25,
    },
    counts: {
      equipment: equipment.length,
      exercises: exerciseEntries.length,
      total: equipment.length + exerciseEntries.length,
    },
    equipment,
    exercises: exerciseEntries,
  };

  const out = path.join(ILL, 'index.json');
  fs.writeFileSync(out, JSON.stringify(index, null, 2) + '\n');
  return index;
}

const isMain =
  process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (isMain) {
  const index = buildIndex();
  console.log(
    `index.json: ${index.counts.equipment} equipment, ${index.counts.exercises} exercises`,
  );
}

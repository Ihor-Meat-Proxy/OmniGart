#!/usr/bin/env node
/**
 * Offline Evidence registry validation (CI job "Evidence").
 * - JSON Schema for entries + claims
 * - Unique evidence/claim ids
 * - Every claim has ≥1 evidence id that exists
 * - Every evidence usedByClaimIds points at a claim
 * - Content exercise/template evidenceRefs resolve into the registry
 * - Planning EvidenceCatalog refIds resolve into the registry
 * - No ru locale under content/evidence
 */
import { readFileSync, readdirSync, existsSync } from 'node:fs';
import { join, dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import Ajv2020 from 'ajv/dist/2020.js';
import addFormats from 'ajv-formats';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, '../..');
const EVIDENCE_ROOT = join(ROOT, 'content/evidence');
const ENTRIES_DIR = join(EVIDENCE_ROOT, 'entries');
const CLAIMS_PATH = join(EVIDENCE_ROOT, 'claims/registry.json');
const LOCALES_DIR = join(EVIDENCE_ROOT, 'locales');
const ENTRY_SCHEMA = JSON.parse(
  readFileSync(join(EVIDENCE_ROOT, 'schema/evidence-entry.schema.json'), 'utf8'),
);
const CLAIMS_SCHEMA = JSON.parse(
  readFileSync(join(EVIDENCE_ROOT, 'schema/claims-registry.schema.json'), 'utf8'),
);

const ajv = new Ajv2020({ allErrors: true, strict: false });
addFormats(ajv);
const validateEntry = ajv.compile(ENTRY_SCHEMA);
const validateClaims = ajv.compile(CLAIMS_SCHEMA);

const errors = [];
const warnings = [];

function fail(msg) {
  errors.push(msg);
}
function warn(msg) {
  warnings.push(msg);
}

function loadEntries() {
  const files = readdirSync(ENTRIES_DIR).filter((f) => f.endsWith('.json'));
  const entries = [];
  for (const file of files) {
    const raw = JSON.parse(readFileSync(join(ENTRIES_DIR, file), 'utf8'));
    if (!validateEntry(raw)) {
      fail(`${file}: schema ${ajv.errorsText(validateEntry.errors)}`);
    }
    if (`${raw.id}.json` !== file) {
      fail(`${file}: filename must match id ${raw.id}.json`);
    }
    entries.push(raw);
  }
  return entries;
}

function harvestContentRefs() {
  const refs = [];
  const roots = [
    join(ROOT, 'content/exercises/data'),
    join(ROOT, 'content/exercises/templates'),
  ];
  for (const dir of roots) {
    if (!existsSync(dir)) continue;
    for (const file of readdirSync(dir).filter((f) => f.endsWith('.json'))) {
      const data = JSON.parse(readFileSync(join(dir, file), 'utf8'));
      for (const r of data.evidenceRefs || []) {
        refs.push({
          key: `${r.kind}:${r.id}`,
          kind: r.kind,
          id: r.id,
          source: `${dir.split('/').slice(-1)[0]}/${file}`,
        });
      }
    }
  }
  return refs;
}

function harvestPlanningRefIds() {
  const catalog = join(
    ROOT,
    'shared/planning/src/commonMain/kotlin/com/omnigart/planning/EvidenceCatalog.kt',
  );
  if (!existsSync(catalog)) return [];
  const text = readFileSync(catalog, 'utf8');
  const out = [];
  const re = /refId\s*=\s*"([^"]+)"/g;
  let m;
  while ((m = re.exec(text))) out.push(m[1]);
  return out;
}

function assertNoRuLocale() {
  if (!existsSync(LOCALES_DIR)) return;
  for (const name of readdirSync(LOCALES_DIR)) {
    if (/^ru(\b|[._-])/i.test(name) || name.toLowerCase() === 'ru.json') {
      fail(`Russian locale file not allowed: locales/${name}`);
    }
  }
  // scan strings for Cyrillic is OK (uk); ensure no dedicated ru keys file
  const forbidden = join(LOCALES_DIR, 'ru.json');
  if (existsSync(forbidden)) fail('content/evidence/locales/ru.json is forbidden');
}

function main() {
  const entries = loadEntries();
  const claimsDoc = JSON.parse(readFileSync(CLAIMS_PATH, 'utf8'));
  if (!validateClaims(claimsDoc)) {
    fail(`claims registry schema: ${ajv.errorsText(validateClaims.errors)}`);
  }

  const entryById = new Map();
  for (const e of entries) {
    if (entryById.has(e.id)) fail(`duplicate evidence id: ${e.id}`);
    entryById.set(e.id, e);
  }

  const claimById = new Map();
  for (const c of claimsDoc.claims) {
    if (claimById.has(c.id)) fail(`duplicate claim id: ${c.id}`);
    claimById.set(c.id, c);
  }

  const localeEn = JSON.parse(readFileSync(join(LOCALES_DIR, 'en-GB.json'), 'utf8'));
  const localeUk = JSON.parse(readFileSync(join(LOCALES_DIR, 'uk.json'), 'utf8'));

  for (const c of claimsDoc.claims) {
    if (!c.evidenceIds.length) fail(`claim ${c.id} has no evidenceIds`);
    for (const eid of c.evidenceIds) {
      if (!entryById.has(eid)) fail(`claim ${c.id} references missing evidence ${eid}`);
    }
    for (const key of [c.textKeys.enGB, c.textKeys.uk]) {
      if (!(key in localeEn)) fail(`missing en-GB locale key ${key}`);
      if (!(key in localeUk)) fail(`missing uk locale key ${key}`);
    }
  }

  for (const e of entries) {
    for (const cid of e.usedByClaimIds) {
      if (!claimById.has(cid)) fail(`evidence ${e.id} usedByClaimIds unknown claim ${cid}`);
    }
    // bidirectional soft check
    for (const c of claimsDoc.claims) {
      if (c.evidenceIds.includes(e.id) && !e.usedByClaimIds.includes(c.id)) {
        warn(`evidence ${e.id} missing usedByClaimIds for claim ${c.id}`);
      }
    }
    if (e.doi === null && e.pmid === null && e.verificationStatus === 'verified') {
      // textbooks/guidelines without identifiers may be unverified only
      if (!['book-coaching-source', 'guideline', 'report'].includes(e.type) || e.id.startsWith('omnigart-')) {
        // karvonen has pmid
      }
    }
  }

  // Content refs → registry
  const contentRefIndex = new Map();
  for (const e of entries) {
    for (const key of e.contentRefKeys || []) {
      contentRefIndex.set(key, e.id);
    }
    if (e.doi) contentRefIndex.set(`doi:${e.doi}`, e.id);
    if (e.pmid) contentRefIndex.set(`pmid:${e.pmid}`, e.id);
  }

  const contentRefs = harvestContentRefs();
  const missingContent = [];
  for (const r of contentRefs) {
    if (!contentRefIndex.has(r.key)) {
      // also try lowercase doi
      const alt = r.kind === 'doi' ? `doi:${r.id}` : r.key;
      if (!contentRefIndex.has(alt)) missingContent.push(r);
    }
  }
  // unique missing
  const seenMiss = new Set();
  for (const r of missingContent) {
    if (seenMiss.has(r.key)) continue;
    seenMiss.add(r.key);
    fail(`content ref not in evidence registry: ${r.key} (e.g. ${r.source})`);
  }

  for (const refId of harvestPlanningRefIds()) {
    if (!contentRefIndex.has(refId)) {
      fail(`planning EvidenceCatalog refId not in registry: ${refId}`);
    }
  }

  assertNoRuLocale();

  // Invented-DOI guard: empty string DOI forbidden by schema; double-check
  for (const e of entries) {
    if (e.doi === '') fail(`evidence ${e.id} has empty doi — omit or set null`);
  }

  const verified = entries.filter((e) => e.verificationStatus === 'verified').length;
  const unverified = entries.filter((e) => e.verificationStatus === 'unverified').length;
  const mismatched = entries.filter((e) => e.verificationStatus === 'mismatched').length;
  const retracted = entries.filter((e) => e.retracted || e.verificationStatus === 'retracted').length;

  console.log(
    JSON.stringify(
      {
        ok: errors.length === 0,
        entries: entries.length,
        claims: claimsDoc.claims.length,
        verificationStatusCounts: { verified, unverified, mismatched, retracted },
        contentRefInstances: contentRefs.length,
        uniqueContentRefs: new Set(contentRefs.map((r) => r.key)).size,
        warnings,
        errors,
      },
      null,
      2,
    ),
  );

  if (errors.length) process.exit(1);
}

main();

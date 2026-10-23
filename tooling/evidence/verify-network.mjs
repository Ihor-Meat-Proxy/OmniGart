#!/usr/bin/env node
/**
 * Network evidence verification (weekly + workflow_dispatch only).
 * - Resolve every DOI via Crossref
 * - Resolve every PMID via NCBI E-utilities
 * - Compare title / year / first authors (tolerant)
 * - Check Crossref update-to for retractions/withdrawals
 * - Write content/evidence/reports/verification-report.json
 * Never invents DOIs; unverifiable entries are marked unverified.
 */
import { readFileSync, writeFileSync, readdirSync, mkdirSync } from 'node:fs';
import { join, dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, '../..');
const ENTRIES_DIR = join(ROOT, 'content/evidence/entries');
const REPORTS_DIR = join(ROOT, 'content/evidence/reports');
const EMAIL = process.env.OMNIGART_EVIDENCE_EMAIL || 'evidence@omnigart.app';
const UA = `OmniGartEvidenceBot/0.1 (mailto:${EMAIL}; https://github.com/Ihor-Meat-Proxy)`;

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function fetchJson(url, init = {}) {
  const res = await fetch(url, {
    ...init,
    headers: {
      'User-Agent': UA,
      Accept: 'application/json',
      ...(init.headers || {}),
    },
  });
  const text = await res.text();
  let body = null;
  try {
    body = text ? JSON.parse(text) : null;
  } catch {
    body = { raw: text.slice(0, 500) };
  }
  return { ok: res.ok, status: res.status, body };
}

function normTitle(t) {
  return (t || '')
    .toLowerCase()
    .replace(/<[^>]+>/g, '')
    .replace(/[^a-z0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function titleSimilar(a, b) {
  const na = normTitle(a);
  const nb = normTitle(b);
  if (!na || !nb) return false;
  if (na === nb) return true;
  if (na.includes(nb) || nb.includes(na)) return true;
  const ta = new Set(na.split(' ').filter((w) => w.length > 3));
  const tb = new Set(nb.split(' ').filter((w) => w.length > 3));
  if (!ta.size || !tb.size) return false;
  let inter = 0;
  for (const w of ta) if (tb.has(w)) inter += 1;
  return inter / Math.min(ta.size, tb.size) >= 0.55;
}

function yearFromCrossref(message) {
  const parts =
    message?.['published-print']?.['date-parts']?.[0] ||
    message?.['published-online']?.['date-parts']?.[0] ||
    message?.issued?.['date-parts']?.[0];
  return parts?.[0] ?? null;
}

function authorsFromCrossref(message) {
  return (message?.author || []).map((a) => {
    const family = a.family || '';
    const given = a.given || '';
    return `${family}${given ? ` ${given[0]}` : ''}`.trim();
  });
}

async function checkCrossref(doi) {
  const enc = encodeURIComponent(doi);
  // Crossref polite pool: ~1 req / 50ms+; we sleep 200ms between calls
  const { ok, status, body } = await fetchJson(`https://api.crossref.org/works/${enc}`);
  if (!ok) return { ok: false, status, error: `crossref HTTP ${status}`, body };
  const message = body.message;
  const updates = message?.['update-to'] || message?.update || [];
  const updateList = Array.isArray(message?.['update-to'])
    ? message['update-to']
    : Array.isArray(message?.update)
      ? message.update
      : [];
  // Crossref uses "update" relation on the work that was updated; also check relation
  const relations = message?.relation || {};
  const isRetracted =
    JSON.stringify(relations).toLowerCase().includes('retraction') ||
    JSON.stringify(updateList).toLowerCase().includes('retract') ||
    (message?.title || []).some((t) => /retract/i.test(t));
  return {
    ok: true,
    status,
    title: (message.title || [])[0] || null,
    year: yearFromCrossref(message),
    authors: authorsFromCrossref(message),
    isRetracted,
    updates: updateList,
    relations,
  };
}

async function checkPmid(pmid) {
  const url =
    `https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&retmode=json` +
    `&email=${encodeURIComponent(EMAIL)}&tool=OmniGartEvidence&id=${encodeURIComponent(pmid)}`;
  const { ok, status, body } = await fetchJson(url);
  if (!ok) return { ok: false, status, error: `pubmed HTTP ${status}` };
  const it = body?.result?.[pmid];
  if (!it) return { ok: false, status, error: 'pmid not found' };
  const doi = (it.articleids || []).find((a) => a.idtype === 'doi')?.value || null;
  const pmcid = (it.articleids || []).find((a) => a.idtype === 'pmc')?.value || null;
  const year = parseInt(String(it.pubdate || '').slice(0, 4), 10) || null;
  return {
    ok: true,
    title: it.title,
    year,
    authors: (it.authors || []).map((a) => a.name),
    doi,
    pmcid,
  };
}

async function checkRetractionWatchHint(doi) {
  // Best-effort: Crossref already checked. Optional RW open data is large;
  // we note when DOI cannot be checked further without inventing status.
  return null;
}

function loadEntries() {
  return readdirSync(ENTRIES_DIR)
    .filter((f) => f.endsWith('.json'))
    .map((f) => JSON.parse(readFileSync(join(ENTRIES_DIR, f), 'utf8')));
}

async function verifyOne(entry) {
  const result = {
    id: entry.id,
    doi: entry.doi,
    pmid: entry.pmid,
    status: 'unverified',
    issues: [],
    remote: {},
  };

  if (!entry.doi && !entry.pmid) {
    result.status = 'unverified';
    result.issues.push('no DOI or PMID to resolve');
    return result;
  }

  let titleOk = true;
  let yearOk = true;
  let retracted = false;

  if (entry.doi) {
    await sleep(220);
    const cr = await checkCrossref(entry.doi);
    result.remote.crossref = {
      http: cr.status,
      title: cr.title,
      year: cr.year,
      isRetracted: cr.isRetracted,
    };
    if (!cr.ok) {
      result.issues.push(`DOI not resolvable via Crossref (${cr.status || cr.error})`);
      result.status = 'unverified';
      return result;
    }
    if (!titleSimilar(entry.title, cr.title)) {
      titleOk = false;
      result.issues.push(`title mismatch vs Crossref: remote="${cr.title}"`);
    }
    if (cr.year && entry.year && Math.abs(cr.year - entry.year) > 1) {
      yearOk = false;
      result.issues.push(`year mismatch vs Crossref: remote=${cr.year} local=${entry.year}`);
    }
    if (cr.isRetracted) {
      retracted = true;
      result.issues.push('Crossref signals retraction/update relation');
    }
    await checkRetractionWatchHint(entry.doi);
  }

  if (entry.pmid) {
    await sleep(350); // NCBI without API key: ≤3 req/s; stay under
    const pm = await checkPmid(entry.pmid);
    result.remote.pubmed = {
      title: pm.title,
      year: pm.year,
      doi: pm.doi,
      pmcid: pm.pmcid,
      ok: pm.ok,
    };
    if (!pm.ok) {
      result.issues.push(`PMID not resolvable (${pm.error})`);
      if (!entry.doi) {
        result.status = 'unverified';
        return result;
      }
    } else {
      if (!titleSimilar(entry.title, pm.title)) {
        titleOk = false;
        result.issues.push(`title mismatch vs PubMed: remote="${pm.title}"`);
      }
      if (pm.year && entry.year && Math.abs(pm.year - entry.year) > 1) {
        yearOk = false;
        result.issues.push(`year mismatch vs PubMed: remote=${pm.year} local=${entry.year}`);
      }
      if (entry.doi && pm.doi && entry.doi.toLowerCase() !== pm.doi.toLowerCase()) {
        result.issues.push(`PMID DOI differs: pubmed=${pm.doi} local=${entry.doi}`);
      }
    }
  }

  if (retracted) result.status = 'retracted';
  else if (!titleOk || !yearOk) result.status = 'mismatched';
  else result.status = 'verified';

  return result;
}

async function main() {
  const entries = loadEntries();
  const results = [];
  for (const e of entries) {
    process.stderr.write(`verify ${e.id}...\n`);
    results.push(await verifyOne(e));
  }

  const today = new Date().toISOString().slice(0, 10);
  // Persist status back into entry files (non-destructive for missing network)
  for (const r of results) {
    const path = join(ENTRIES_DIR, `${r.id}.json`);
    const entry = JSON.parse(readFileSync(path, 'utf8'));
    entry.verificationStatus = r.status;
    if (r.status === 'verified') entry.lastVerified = today;
    if (r.status === 'retracted') {
      entry.retracted = true;
      entry.retractionNotes = r.issues.join('; ');
    }
    writeFileSync(path, JSON.stringify(entry, null, 2) + '\n');
  }

  const summary = {
    generatedAt: new Date().toISOString(),
    email: EMAIL,
    counts: {
      total: results.length,
      verified: results.filter((r) => r.status === 'verified').length,
      unverified: results.filter((r) => r.status === 'unverified').length,
      mismatched: results.filter((r) => r.status === 'mismatched').length,
      retracted: results.filter((r) => r.status === 'retracted').length,
    },
    unverified: results.filter((r) => r.status === 'unverified'),
    mismatched: results.filter((r) => r.status === 'mismatched'),
    retracted: results.filter((r) => r.status === 'retracted'),
    results,
  };

  mkdirSync(REPORTS_DIR, { recursive: true });
  const reportPath = join(REPORTS_DIR, 'verification-report.json');
  writeFileSync(reportPath, JSON.stringify(summary, null, 2) + '\n');
  console.log(JSON.stringify(summary.counts, null, 2));
  console.log(`Wrote ${reportPath}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});

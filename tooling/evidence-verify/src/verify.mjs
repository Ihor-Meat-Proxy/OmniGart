#!/usr/bin/env node
/**
 * OmniGart evidence verifier.
 *
 * Resolves every DOI via Crossref and every PMID via NCBI E-utilities.
 * Distinguishes resolve failures from network errors; uses on-disk cache so
 * transient network issues do not fail PR checks when --offline-ok is set
 * (or when a warm cache entry exists after retries).
 *
 * Exit codes:
 *   0 — ok (or network soft-pass with cache)
 *   2 — resolve failure (unresolvable id, title/year mismatch, unexpected retraction)
 *   3 — network failure without usable cache
 */
import { createHash } from "node:crypto";
import {
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  writeFileSync,
} from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, "../../..");
const EVIDENCE_ROOT = join(ROOT, "content/evidence");
const CACHE_DIR = join(__dirname, "../.cache");
const CONTACT = process.env.EVIDENCE_CONTACT_EMAIL || "evidence@omnigart.dev";
const USER_AGENT = `OmniGartEvidenceVerify/1.0 (mailto:${CONTACT}; +https://github.com/Ihor-Meat-Proxy/OmniGart)`;
const CROSSREF = "https://api.crossref.org";
const NCBI = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils";

const args = new Set(process.argv.slice(2));
const OFFLINE_OK = args.has("--offline-ok") || process.env.EVIDENCE_OFFLINE_OK === "1";
const WRITE_REPORT = args.has("--report") || process.env.EVIDENCE_REPORT_PATH;
const REPORT_PATH =
  process.env.EVIDENCE_REPORT_PATH || join(ROOT, "tooling/evidence-verify/last-report.json");

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function normalizeTitle(t) {
  return String(t || "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .trim()
    .replace(/\s+/g, " ");
}

function titleSimilar(a, b) {
  const na = normalizeTitle(a);
  const nb = normalizeTitle(b);
  if (!na || !nb) return false;
  if (na === nb) return true;
  if (na.includes(nb) || nb.includes(na)) return true;
  const ta = new Set(na.split(" ").filter((w) => w.length > 3));
  const tb = new Set(nb.split(" ").filter((w) => w.length > 3));
  if (ta.size === 0 || tb.size === 0) return false;
  let inter = 0;
  for (const w of ta) if (tb.has(w)) inter += 1;
  const union = new Set([...ta, ...tb]).size;
  return inter / union >= 0.55;
}

function cacheKey(kind, id) {
  return createHash("sha256").update(`${kind}:${id}`).digest("hex").slice(0, 24);
}

function readCache(kind, id) {
  const path = join(CACHE_DIR, `${kind}-${cacheKey(kind, id)}.json`);
  if (!existsSync(path)) return null;
  try {
    return JSON.parse(readFileSync(path, "utf8"));
  } catch {
    return null;
  }
}

function writeCache(kind, id, payload) {
  mkdirSync(CACHE_DIR, { recursive: true });
  const path = join(CACHE_DIR, `${kind}-${cacheKey(kind, id)}.json`);
  writeFileSync(
    path,
    JSON.stringify({ fetchedAt: new Date().toISOString(), kind, id, ...payload }, null, 2) + "\n",
  );
}

class NetworkError extends Error {
  constructor(message, cause) {
    super(message);
    this.name = "NetworkError";
    this.cause = cause;
  }
}

class ResolveError extends Error {
  constructor(message) {
    super(message);
    this.name = "ResolveError";
  }
}

async function fetchJson(url, { retries = 3, minIntervalMs = 1100 } = {}) {
  let lastNet = null;
  for (let attempt = 1; attempt <= retries; attempt++) {
    if (attempt > 1) await sleep(minIntervalMs * attempt);
    else await sleep(minIntervalMs);
    try {
      const res = await fetch(url, {
        headers: {
          "User-Agent": USER_AGENT,
          Accept: "application/json",
        },
        signal: AbortSignal.timeout(25000),
      });
      if (res.status === 404) {
        throw new ResolveError(`HTTP 404 for ${url}`);
      }
      if (res.status === 429 || res.status >= 500) {
        lastNet = new NetworkError(`HTTP ${res.status} for ${url}`);
        continue;
      }
      if (!res.ok) {
        throw new ResolveError(`HTTP ${res.status} for ${url}`);
      }
      return await res.json();
    } catch (err) {
      if (err instanceof ResolveError) throw err;
      lastNet = new NetworkError(err.message || String(err), err);
    }
  }
  throw lastNet || new NetworkError(`Failed to fetch ${url}`);
}

function yearFromCrossref(msg) {
  for (const key of ["published-print", "published-online", "created", "issued"]) {
    const parts = msg?.[key]?.["date-parts"]?.[0];
    if (parts?.[0]) return parts[0];
  }
  return null;
}

function isRetractionUpdate(msg) {
  const updates = [...(msg["update-to"] || []), ...(msg["updated-by"] || [])];
  const types = updates.map((u) => String(u.type || u.label || "").toLowerCase());
  return types.some(
    (t) =>
      t.includes("retract") ||
      t.includes("withdrawal") ||
      t.includes("remove") ||
      t === "expression of concern",
  );
}

async function resolveDoi(doi) {
  const cached = readCache("doi", doi);
  try {
    const data = await fetchJson(`${CROSSREF}/works/${encodeURIComponent(doi)}`);
    const msg = data.message;
    const payload = {
      title: (msg.title || [""])[0],
      year: yearFromCrossref(msg),
      retractedSignal: isRetractionUpdate(msg),
      updateTypes: [...(msg["update-to"] || []), ...(msg["updated-by"] || [])].map(
        (u) => u.type || u.label,
      ),
      container: (msg["container-title"] || [""])[0] || null,
    };
    writeCache("doi", doi, { ok: true, payload });
    return { source: "network", ...payload };
  } catch (err) {
    if (err instanceof ResolveError) throw err;
    if (cached?.ok && cached.payload) {
      return { source: "cache", networkError: err.message, ...cached.payload };
    }
    throw err;
  }
}

async function resolvePmid(pmid) {
  const cached = readCache("pmid", pmid);
  try {
    const summary = await fetchJson(
      `${NCBI}/esummary.fcgi?db=pubmed&retmode=json&id=${encodeURIComponent(pmid)}`,
    );
    const rec = summary?.result?.[pmid];
    if (!rec || rec.error) {
      throw new ResolveError(`PMID ${pmid} not found in NCBI`);
    }
    const year = Number.parseInt(String(rec.pubdate || "").slice(0, 4), 10) || null;
    const payload = {
      title: rec.title || "",
      year,
      retractedSignal: String(rec.pubstatus || "").toLowerCase().includes("retract"),
      sourceName: rec.fulljournalname || rec.source || null,
    };
    writeCache("pmid", pmid, { ok: true, payload });
    return { source: "network", ...payload };
  } catch (err) {
    if (err instanceof ResolveError) throw err;
    if (cached?.ok && cached.payload) {
      return { source: "cache", networkError: err.message, ...cached.payload };
    }
    throw err;
  }
}

function loadEntries() {
  const dir = join(EVIDENCE_ROOT, "entries");
  return readdirSync(dir)
    .filter((f) => f.endsWith(".json"))
    .map((f) => JSON.parse(readFileSync(join(dir, f), "utf8")));
}

function checkMatch(entry, remote, field) {
  const issues = [];
  if (remote.title && !titleSimilar(entry.title, remote.title)) {
    issues.push(
      `${field} title mismatch for ${entry.id}: local="${entry.title.slice(0, 80)}" remote="${String(remote.title).slice(0, 80)}"`,
    );
  }
  if (remote.year && entry.year && Math.abs(remote.year - entry.year) > 1) {
    issues.push(
      `${field} year mismatch for ${entry.id}: local=${entry.year} remote=${remote.year}`,
    );
  }
  return issues;
}

async function main() {
  mkdirSync(CACHE_DIR, { recursive: true });
  const entries = loadEntries();
  const resolveFailures = [];
  const networkFailures = [];
  const warnings = [];
  const usedCache = [];
  let doiChecked = 0;
  let pmidChecked = 0;

  for (const entry of entries) {
    if (entry.doi) {
      doiChecked += 1;
      try {
        const remote = await resolveDoi(entry.doi);
        if (remote.source === "cache") {
          usedCache.push(`doi:${entry.doi}`);
          if (remote.networkError) {
            warnings.push(`Network fallback to cache for doi:${entry.doi} (${remote.networkError})`);
          }
        }
        resolveFailures.push(...checkMatch(entry, remote, "DOI"));
        if (remote.retractedSignal && !entry.retracted) {
          resolveFailures.push(
            `DOI ${entry.doi} (${entry.id}) has Crossref retraction/withdrawal signal but retracted=false`,
          );
        }
        if (!remote.retractedSignal && entry.retracted) {
          warnings.push(
            `${entry.id} marked retracted locally but Crossref has no retraction update-to/updated-by`,
          );
        }
      } catch (err) {
        if (err instanceof ResolveError) {
          resolveFailures.push(`DOI ${entry.doi} (${entry.id}): ${err.message}`);
        } else if (err instanceof NetworkError) {
          networkFailures.push(`DOI ${entry.doi} (${entry.id}): ${err.message}`);
        } else {
          networkFailures.push(`DOI ${entry.doi} (${entry.id}): ${err.message || err}`);
        }
      }
    }

    if (entry.pmid) {
      pmidChecked += 1;
      try {
        const remote = await resolvePmid(entry.pmid);
        if (remote.source === "cache") {
          usedCache.push(`pmid:${entry.pmid}`);
          if (remote.networkError) {
            warnings.push(`Network fallback to cache for pmid:${entry.pmid} (${remote.networkError})`);
          }
        }
        resolveFailures.push(...checkMatch(entry, remote, "PMID"));
        if (remote.retractedSignal && !entry.retracted) {
          resolveFailures.push(
            `PMID ${entry.pmid} (${entry.id}) flagged retracted remotely but retracted=false`,
          );
        }
      } catch (err) {
        if (err instanceof ResolveError) {
          resolveFailures.push(`PMID ${entry.pmid} (${entry.id}): ${err.message}`);
        } else if (err instanceof NetworkError) {
          networkFailures.push(`PMID ${entry.pmid} (${entry.id}): ${err.message}`);
        } else {
          networkFailures.push(`PMID ${entry.pmid} (${entry.id}): ${err.message || err}`);
        }
      }
    }

    if (!entry.doi && !entry.pmid && !entry.url) {
      warnings.push(`${entry.id} has no DOI/PMID/URL to resolve (textbook/consensus ok)`);
    }
  }

  const byType = {};
  for (const e of entries) {
    byType[e.type] = (byType[e.type] || 0) + 1;
  }

  const report = {
    generatedAt: new Date().toISOString(),
    entryCount: entries.length,
    byType,
    doiChecked,
    pmidChecked,
    resolveFailures,
    networkFailures,
    warnings,
    usedCache,
    retracted: entries.filter((e) => e.retracted).map((e) => e.id),
    offlineOk: OFFLINE_OK,
  };

  if (WRITE_REPORT || process.env.EVIDENCE_REPORT_PATH) {
    writeFileSync(REPORT_PATH, JSON.stringify(report, null, 2) + "\n");
    console.log(`Wrote report ${REPORT_PATH}`);
  }

  console.log(`Evidence entries: ${entries.length}`);
  console.log(`By type: ${JSON.stringify(byType)}`);
  console.log(`Checked DOI=${doiChecked} PMID=${pmidChecked}`);
  if (warnings.length) {
    console.log("Warnings:");
    for (const w of warnings) console.log(`  - ${w}`);
  }
  if (resolveFailures.length) {
    console.error("Resolve failures:");
    for (const f of resolveFailures) console.error(`  - ${f}`);
  }
  if (networkFailures.length) {
    console.error("Network failures:");
    for (const f of networkFailures) console.error(`  - ${f}`);
  }

  if (resolveFailures.length) {
    process.exitCode = 2;
    return;
  }
  if (networkFailures.length) {
    if (OFFLINE_OK) {
      console.warn(
        "Network failures present but --offline-ok set; treating as soft-pass for PR flakiness.",
      );
      process.exitCode = 0;
      return;
    }
    process.exitCode = 3;
    return;
  }
  console.log("Evidence verification OK");
  process.exitCode = 0;
}

main().catch((err) => {
  console.error(err);
  process.exitCode = 3;
});

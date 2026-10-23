#!/usr/bin/env node
/**
 * Verify a built content pack: manifest shape, per-file hashes, archive hash,
 * channel rules, optional size budget, optional minisign signatures.
 */
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawnSync } from 'node:child_process';

function parseArgs(argv) {
  const out = { dir: null, pubkey: null, requireSig: false, catalogDist: null };
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--dir') out.dir = argv[++i];
    else if (a === '--pubkey') out.pubkey = argv[++i];
    else if (a === '--require-sig') out.requireSig = true;
    else if (a === '--catalog-dist') out.catalogDist = argv[++i];
  }
  if (!out.dir && !out.catalogDist) {
    console.error(
      'Usage: verify.mjs --dir <builtPackDir> [--pubkey minisign.pub] [--require-sig]\n' +
        '   or: verify.mjs --catalog-dist <distDir> [--pubkey ...] [--require-sig]',
    );
    process.exit(2);
  }
  return out;
}

function sha256File(filePath) {
  const h = crypto.createHash('sha256');
  h.update(fs.readFileSync(filePath));
  return h.digest('hex');
}

function fail(msg) {
  console.error(`FAIL: ${msg}`);
  process.exit(1);
}

function minisignVerify(filePath, pubkey) {
  const sig = `${filePath}.minisig`;
  if (!fs.existsSync(sig)) return { ok: false, missing: true };
  const r = spawnSync('minisign', ['-Vm', filePath, '-p', pubkey], { encoding: 'utf8' });
  return { ok: r.status === 0, missing: false, stderr: r.stderr, stdout: r.stdout };
}

function extractTarList(archivePath) {
  const r = spawnSync('tar', ['-tf', archivePath], { encoding: 'utf8' });
  if (r.status !== 0) fail(`tar list failed: ${r.stderr}`);
  return r.stdout
    .split('\n')
    .map((s) => s.replace(/\/$/, ''))
    .filter(Boolean)
    .filter((p) => !p.endsWith('/'));
}

function verifyOne(dir, args) {
  const manifestPath = path.join(dir, 'manifest.json');
  const archivePath = path.join(dir, 'pack.tar');

  if (!fs.existsSync(manifestPath)) fail(`${dir}: manifest.json missing`);
  if (!fs.existsSync(archivePath)) fail(`${dir}: pack.tar missing`);

  const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
  if (manifest.schema !== 'omnigart.content-pack/1') fail(`${dir}: unexpected schema`);
  if (!manifest.files?.length) fail(`${dir}: empty files list`);
  if (manifest.channel === 'update' && !manifest.baselineVersion) {
    fail(`${dir}: update missing baselineVersion`);
  }

  if (sha256File(archivePath) !== manifest.archive.sha256) {
    fail(`${dir}: archive sha256 mismatch`);
  }
  if (fs.statSync(archivePath).size !== manifest.archive.size) {
    fail(`${dir}: archive size mismatch`);
  }

  if (
    manifest.channel === 'baseline' &&
    typeof manifest.sizeBudgetBytes === 'number' &&
    manifest.archive.size > manifest.sizeBudgetBytes
  ) {
    fail(
      `${dir}: baseline ${manifest.archive.size} exceeds sizeBudgetBytes ${manifest.sizeBudgetBytes}`,
    );
  }

  const tmp = fs.mkdtempSync(path.join(dir, '.verify-'));
  try {
    const x = spawnSync('tar', ['-xf', archivePath, '-C', tmp], { encoding: 'utf8' });
    if (x.status !== 0) fail(`extract failed: ${x.stderr}`);

    const listed = new Set(extractTarList(archivePath));
    for (const f of manifest.files) {
      const abs = path.join(tmp, f.path);
      if (!fs.existsSync(abs)) fail(`missing file in archive: ${f.path}`);
      if (sha256File(abs) !== f.sha256) fail(`sha256 mismatch: ${f.path}`);
      if (fs.statSync(abs).size !== f.size) fail(`size mismatch: ${f.path}`);
      listed.delete(f.path);
    }
    if (listed.size) fail(`archive has unexpected paths: ${[...listed].join(', ')}`);
  } finally {
    fs.rmSync(tmp, { recursive: true, force: true });
  }

  if (args.pubkey) {
    const pubkey = path.resolve(args.pubkey);
    if (!fs.existsSync(pubkey)) fail(`pubkey not found: ${pubkey}`);
    for (const target of [manifestPath, archivePath]) {
      const res = minisignVerify(target, pubkey);
      if (res.missing) {
        if (args.requireSig) fail(`missing signature for ${path.basename(target)}`);
        console.warn(`WARN: no .minisig for ${path.basename(target)} (unsigned build)`);
        continue;
      }
      if (!res.ok) fail(`minisign verify failed for ${path.basename(target)}: ${res.stderr}`);
      console.log(`OK: minisign ${path.basename(target)}`);
    }
  } else if (args.requireSig) {
    fail('--require-sig needs --pubkey');
  }

  console.log(`OK: verified ${manifest.channel} ${manifest.id}@${manifest.version}`);
}

function collectPackDirs(distRoot) {
  const indexPath = path.join(distRoot, 'build-index.json');
  if (fs.existsSync(indexPath)) {
    const index = JSON.parse(fs.readFileSync(indexPath, 'utf8'));
    return index.packs.map((p) => path.join(distRoot, p.out));
  }
  // Fallback walk
  const found = [];
  function walk(d) {
    for (const ent of fs.readdirSync(d, { withFileTypes: true })) {
      const p = path.join(d, ent.name);
      if (ent.isDirectory()) {
        if (fs.existsSync(path.join(p, 'manifest.json'))) found.push(p);
        else walk(p);
      }
    }
  }
  walk(distRoot);
  return found;
}

function main() {
  const args = parseArgs(process.argv);
  if (args.catalogDist) {
    const root = path.resolve(args.catalogDist);
    const dirs = collectPackDirs(root);
    if (!dirs.length) fail(`no packs under ${root}`);
    for (const d of dirs) verifyOne(d, args);
    console.log(`OK: catalog-dist verified (${dirs.length} packs)`);
    return;
  }
  verifyOne(path.resolve(args.dir), args);
}

main();

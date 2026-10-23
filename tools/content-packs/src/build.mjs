#!/usr/bin/env node
/**
 * Deterministic content-pack builder (bundle-first).
 *
 * Same content sources produce:
 *   - channel=baseline  → embedded in app binaries (always-working offline baseline)
 *   - channel=update    → signed CDN pack; app applies only if signature + schema +
 *                         minAppVersion pass and version > installed; rollback on failure
 *   - channel=media|off|config → CDN (config also bundles safe defaults)
 *
 * Determinism: sorted paths, tar mtime=epoch, uid/gid 0, stable JSON.
 */
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..');

function parseArgs(argv) {
  const out = {
    in: null,
    out: null,
    id: null,
    version: null,
    minAppVersion: '0.1.0',
    channel: 'baseline',
    baselineVersion: null,
    contentEpoch: null,
    sizeBudgetBytes: null,
    bundlePath: null,
    catalog: null,
  };
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--in') out.in = argv[++i];
    else if (a === '--out') out.out = argv[++i];
    else if (a === '--id') out.id = argv[++i];
    else if (a === '--version') out.version = argv[++i];
    else if (a === '--min-app-version') out.minAppVersion = argv[++i];
    else if (a === '--channel') out.channel = argv[++i];
    else if (a === '--baseline-version') out.baselineVersion = argv[++i];
    else if (a === '--content-epoch') out.contentEpoch = argv[++i];
    else if (a === '--size-budget-bytes') out.sizeBudgetBytes = Number(argv[++i]);
    else if (a === '--bundle-path') out.bundlePath = argv[++i];
    else if (a === '--catalog') out.catalog = argv[++i];
    else if (a === '--help') {
      console.log(`Usage:
  build.mjs --catalog catalog.json [--out dist]
  build.mjs --in <packDir> --out <dir> --id ... --version ... [--channel baseline|update|media|off|config]`);
      process.exit(0);
    }
  }
  return out;
}

function sha256File(filePath) {
  const h = crypto.createHash('sha256');
  h.update(fs.readFileSync(filePath));
  return h.digest('hex');
}

function sha256Buffer(buf) {
  return crypto.createHash('sha256').update(buf).digest('hex');
}

function walkFiles(dir, base = dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  const files = [];
  for (const ent of entries.sort((a, b) => a.name.localeCompare(b.name))) {
    if (ent.name === 'pack.json' || ent.name.startsWith('.')) continue;
    const abs = path.join(dir, ent.name);
    if (ent.isDirectory()) files.push(...walkFiles(abs, base));
    else if (ent.isFile()) files.push(path.relative(base, abs).split(path.sep).join('/'));
  }
  return files;
}

function readPackMeta(packDir) {
  const metaPath = path.join(packDir, 'pack.json');
  if (!fs.existsSync(metaPath)) return {};
  return JSON.parse(fs.readFileSync(metaPath, 'utf8'));
}

function validateManifestShape(m) {
  if (m.schema !== 'omnigart.content-pack/1') throw new Error('bad schema');
  if (!/^[a-z][a-z0-9-]{1,63}$/.test(m.id)) throw new Error(`bad id: ${m.id}`);
  const channels = new Set(['baseline', 'update', 'media', 'off', 'config']);
  if (!channels.has(m.channel)) throw new Error(`bad channel: ${m.channel}`);
  if (m.channel === 'update' && !m.baselineVersion) {
    throw new Error('update channel requires baselineVersion');
  }
  if (!m.version || !m.minAppVersion) throw new Error('missing version fields');
  if (!Array.isArray(m.files) || !m.files.length) throw new Error('no files');
}

function contentEpochFromDir(dir) {
  const files = walkFiles(dir);
  const h = crypto.createHash('sha256');
  for (const rel of files) {
    h.update(rel);
    h.update('\0');
    h.update(fs.readFileSync(path.join(dir, rel)));
  }
  return h.digest('hex').slice(0, 16);
}

function buildOne(opts) {
  const packDir = path.resolve(opts.in);
  const outDir = path.resolve(opts.out);
  const meta = readPackMeta(packDir);

  const id = opts.id || meta.id;
  const version = opts.version || meta.version;
  const minAppVersion = opts.minAppVersion || meta.minAppVersion || '0.1.0';
  const channel = opts.channel || meta.channel || 'baseline';
  const baselineVersion = opts.baselineVersion || meta.baselineVersion || null;
  const contentEpoch = opts.contentEpoch || meta.contentEpoch || contentEpochFromDir(packDir);
  const sizeBudgetBytes =
    opts.sizeBudgetBytes ?? meta.sizeBudgetBytes ?? null;
  const bundlePath = opts.bundlePath || meta.bundlePath || null;

  if (!id || !version) {
    throw new Error('Pack id/version required via pack.json or flags');
  }

  const relFiles = walkFiles(packDir);
  if (!relFiles.length) throw new Error(`No content files in ${packDir}`);

  fs.mkdirSync(outDir, { recursive: true });
  const staging = path.join(outDir, '.staging');
  fs.rmSync(staging, { recursive: true, force: true });
  fs.mkdirSync(staging, { recursive: true });

  const fileEntries = [];
  for (const rel of relFiles) {
    const src = path.join(packDir, rel);
    const dest = path.join(staging, rel);
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    fs.copyFileSync(src, dest);
    fs.chmodSync(dest, 0o644);
    const st = fs.statSync(dest);
    fileEntries.push({
      path: rel,
      sha256: sha256File(dest),
      size: st.size,
    });
  }

  const archivePath = path.join(outDir, 'pack.tar');
  const tarArgs = [
    '--format=ustar',
    '--sort=name',
    '--mtime=UTC 1970-01-01',
    '--owner=0',
    '--group=0',
    '--numeric-owner',
    '-cf',
    archivePath,
    '-C',
    staging,
    ...relFiles,
  ];
  const tar = spawnSync('tar', tarArgs, { encoding: 'utf8' });
  if (tar.status !== 0) {
    throw new Error(tar.stderr || tar.stdout || 'tar failed');
  }

  const archiveStat = fs.statSync(archivePath);
  if (channel === 'baseline' && sizeBudgetBytes != null && archiveStat.size > sizeBudgetBytes) {
    throw new Error(
      `baseline pack ${id}@${version} archive ${archiveStat.size} bytes exceeds budget ${sizeBudgetBytes}`,
    );
  }

  const manifest = {
    schema: 'omnigart.content-pack/1',
    id,
    version,
    channel,
    minAppVersion,
    contentEpoch,
    createdAt: meta.createdAt || '1970-01-01T00:00:00.000Z',
    description: meta.description || undefined,
    locale: meta.locale || undefined,
    baselineVersion: channel === 'update' ? baselineVersion || version : undefined,
    bundlePath: channel === 'baseline' ? bundlePath || undefined : undefined,
    sizeBudgetBytes: sizeBudgetBytes ?? undefined,
    files: fileEntries,
    archive: {
      file: 'pack.tar',
      sha256: sha256File(archivePath),
      size: archiveStat.size,
    },
  };
  for (const k of Object.keys(manifest)) {
    if (manifest[k] === undefined) delete manifest[k];
  }
  validateManifestShape(manifest);

  fs.writeFileSync(path.join(outDir, 'manifest.json'), `${JSON.stringify(manifest, null, 2)}\n`);

  const pointer = {
    schema: 'omnigart.content-pack-pointer/1',
    id,
    version,
    channel,
    contentEpoch,
    baselineVersion: manifest.baselineVersion,
    manifestPath: `packs/${id}/v/${version}/manifest.json`,
    archivePath: `packs/${id}/v/${version}/pack.tar`,
  };
  for (const k of Object.keys(pointer)) {
    if (pointer[k] === undefined) delete pointer[k];
  }
  fs.writeFileSync(path.join(outDir, 'current.json'), `${JSON.stringify(pointer, null, 2)}\n`);

  // Unpacked mirror for baseline embedding (apps can copy files/ without extracting tar)
  if (channel === 'baseline') {
    const filesOut = path.join(outDir, 'files');
    fs.rmSync(filesOut, { recursive: true, force: true });
    fs.cpSync(staging, filesOut, { recursive: true });
  }

  fs.rmSync(staging, { recursive: true, force: true });
  fs.copyFileSync(
    path.join(ROOT, 'schema', 'manifest.schema.json'),
    path.join(outDir, 'manifest.schema.json'),
  );

  console.log(`Built ${channel} pack ${id}@${version} (${archiveStat.size} bytes) → ${outDir}`);
  return manifest;
}

function buildFromCatalog(catalogPath, outRoot) {
  const catalog = JSON.parse(fs.readFileSync(catalogPath, 'utf8'));
  if (catalog.schema !== 'omnigart.content-catalog/1') {
    throw new Error('bad catalog schema');
  }
  const epoch =
    catalog.contentEpoch && catalog.contentEpoch !== 'local-dev'
      ? catalog.contentEpoch
      : sha256Buffer(fs.readFileSync(catalogPath)).slice(0, 16);

  const index = {
    schema: 'omnigart.content-build-index/1',
    contentEpoch: epoch,
    generatedAt: new Date().toISOString(),
    packs: [],
  };

  for (const p of catalog.packs) {
    const sourceDir = path.resolve(path.dirname(catalogPath), p.sourceDir);
    if (p.bundle) {
      const baselineOut = path.join(outRoot, 'baseline', p.id);
      const m = buildOne({
        in: sourceDir,
        out: baselineOut,
        id: p.id,
        version: p.version,
        channel: 'baseline',
        minAppVersion: p.minAppVersion || '0.1.0',
        contentEpoch: epoch,
        sizeBudgetBytes: p.sizeBudgetBytes,
        bundlePath: p.bundlePath,
      });
      index.packs.push({
        id: p.id,
        version: p.version,
        channel: 'baseline',
        out: path.relative(outRoot, baselineOut),
        archiveBytes: m.archive.size,
        sizeBudgetBytes: p.sizeBudgetBytes ?? null,
      });
    }

    if (p.publishUpdate) {
      const updateOut = path.join(outRoot, 'updates', p.id, `v${p.version}`);
      const m = buildOne({
        in: sourceDir,
        out: updateOut,
        id: p.id,
        version: p.version,
        channel: p.channel === 'config' ? 'config' : 'update',
        baselineVersion: p.version,
        minAppVersion: p.minAppVersion || '0.1.0',
        contentEpoch: epoch,
        sizeBudgetBytes: p.sizeBudgetBytes,
      });
      index.packs.push({
        id: p.id,
        version: p.version,
        channel: m.channel,
        out: path.relative(outRoot, updateOut),
        archiveBytes: m.archive.size,
        sizeBudgetBytes: p.sizeBudgetBytes ?? null,
      });
    } else if (!p.bundle && (p.channel === 'media' || p.channel === 'off')) {
      const mediaOut = path.join(outRoot, 'cdn', p.id, `v${p.version}`);
      const m = buildOne({
        in: sourceDir,
        out: mediaOut,
        id: p.id,
        version: p.version,
        channel: p.channel,
        minAppVersion: p.minAppVersion || '0.1.0',
        contentEpoch: epoch,
      });
      index.packs.push({
        id: p.id,
        version: p.version,
        channel: p.channel,
        out: path.relative(outRoot, mediaOut),
        archiveBytes: m.archive.size,
        sizeBudgetBytes: null,
      });
    }
  }

  fs.mkdirSync(outRoot, { recursive: true });
  fs.writeFileSync(path.join(outRoot, 'build-index.json'), `${JSON.stringify(index, null, 2)}\n`);
  console.log(`Catalog build complete → ${outRoot} (epoch ${epoch})`);
  return index;
}

function main() {
  const args = parseArgs(process.argv);
  try {
    if (args.catalog) {
      const out = args.out || path.join(ROOT, 'dist');
      buildFromCatalog(path.resolve(args.catalog), path.resolve(out));
      return;
    }
    if (!args.in || !args.out) {
      console.error('Required: --catalog OR (--in and --out)');
      process.exit(2);
    }
    buildOne(args);
  } catch (err) {
    console.error(String(err?.message || err));
    process.exit(1);
  }
}

main();

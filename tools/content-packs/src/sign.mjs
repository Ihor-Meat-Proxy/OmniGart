#!/usr/bin/env node
/**
 * Sign manifest.json and pack.tar with minisign.
 *
 * Preferred: run offline with an air-gapped / hardware-backed secret key.
 * CI release workflow may sign only after Environment approval.
 *
 *   MINISIGN_SECRET_KEY_FILE=~/.minisign/omnigart.key \
 *     node src/sign.mjs --dir dist/sample
 *
 * Or pipe unencrypted secret key via MINISIGN_SECRET_KEY (discouraged).
 */
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { spawnSync } from 'node:child_process';

function parseArgs(argv) {
  const out = { dir: null };
  for (let i = 2; i < argv.length; i++) {
    if (argv[i] === '--dir') out.dir = argv[++i];
  }
  if (!out.dir) {
    console.error('Usage: sign.mjs --dir <builtPackDir>');
    process.exit(2);
  }
  return out;
}

function main() {
  const { dir } = parseArgs(process.argv);
  const abs = path.resolve(dir);
  const keyFromEnvFile = process.env.MINISIGN_SECRET_KEY_FILE;
  const keyInline = process.env.MINISIGN_SECRET_KEY;

  let keyPath = keyFromEnvFile;
  let cleanup = null;
  if (!keyPath && keyInline) {
    cleanup = path.join(os.tmpdir(), `omnigart-minisign-${process.pid}.key`);
    fs.writeFileSync(cleanup, keyInline.endsWith('\n') ? keyInline : `${keyInline}\n`, {
      mode: 0o600,
    });
    keyPath = cleanup;
  }
  if (!keyPath || !fs.existsSync(keyPath)) {
    console.error('Set MINISIGN_SECRET_KEY_FILE or MINISIGN_SECRET_KEY');
    process.exit(2);
  }

  try {
    for (const name of ['manifest.json', 'pack.tar', 'current.json']) {
      const file = path.join(abs, name);
      if (!fs.existsSync(file)) {
        if (name === 'current.json') continue;
        console.error(`missing ${name}`);
        process.exit(1);
      }
      const r = spawnSync(
        'minisign',
        ['-Sm', file, '-s', keyPath],
        { encoding: 'utf8', env: { ...process.env, MINISIGN_PASSWORD: process.env.MINISIGN_PASSWORD || '' } },
      );
      if (r.status !== 0) {
        console.error(r.stderr || r.stdout);
        process.exit(1);
      }
      console.log(`Signed ${name}`);
    }
  } finally {
    if (cleanup) fs.rmSync(cleanup, { force: true });
  }
}

main();

#!/usr/bin/env node
/**
 * CI gate: bundled (baseline) content must stay within docs/APP_SIZE_BUDGET.md budgets.
 * Reads dist/build-index.json and/or docs/APP_SIZE_BUDGET.json.
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const REPO = path.resolve(__dirname, '../../..');

function parseArgs(argv) {
  const out = {
    index: path.join(REPO, 'tools/content-packs/dist/build-index.json'),
    budget: path.join(REPO, 'docs/APP_SIZE_BUDGET.json'),
  };
  for (let i = 2; i < argv.length; i++) {
    if (argv[i] === '--index') out.index = path.resolve(argv[++i]);
    else if (argv[i] === '--budget') out.budget = path.resolve(argv[++i]);
  }
  return out;
}

function fail(msg) {
  console.error(`FAIL: ${msg}`);
  process.exit(1);
}

function main() {
  const args = parseArgs(process.argv);
  if (!fs.existsSync(args.index)) fail(`missing build index: ${args.index} (run build --catalog first)`);
  if (!fs.existsSync(args.budget)) fail(`missing budget file: ${args.budget}`);

  const index = JSON.parse(fs.readFileSync(args.index, 'utf8'));
  const budget = JSON.parse(fs.readFileSync(args.budget, 'utf8'));

  const baselines = index.packs.filter((p) => p.channel === 'baseline');
  let total = 0;
  const perPack = budget.packs || {};
  let failed = false;

  for (const p of baselines) {
    total += p.archiveBytes;
    const limit = perPack[p.id]?.maxArchiveBytes;
    const declared = p.sizeBudgetBytes;
    if (typeof limit === 'number' && p.archiveBytes > limit) {
      console.error(
        `FAIL: ${p.id} archive ${p.archiveBytes} > budget.packs.${p.id}.maxArchiveBytes ${limit}`,
      );
      failed = true;
    }
    if (typeof declared === 'number' && p.archiveBytes > declared) {
      console.error(`FAIL: ${p.id} archive ${p.archiveBytes} > manifest sizeBudgetBytes ${declared}`);
      failed = true;
    }
    console.log(
      `OK: ${p.id} ${p.archiveBytes} bytes` +
        (limit != null ? ` / ${limit} budget` : ''),
    );
  }

  const totalLimit = budget.totals?.maxBundledContentBytes;
  if (typeof totalLimit === 'number' && total > totalLimit) {
    console.error(`FAIL: total bundled content ${total} > totals.maxBundledContentBytes ${totalLimit}`);
    failed = true;
  } else {
    console.log(`OK: total bundled content ${total} bytes` + (totalLimit != null ? ` / ${totalLimit}` : ''));
  }

  // Install-size headroom check is informational unless installTargetBytes set
  if (budget.totals?.installTargetBytes) {
    const headroom = budget.totals.installTargetBytes - total;
    console.log(
      `INFO: install target ${budget.totals.installTargetBytes}; content uses ${total}; remaining for code/assets ≈ ${headroom}`,
    );
    if (headroom < (budget.totals.minCodeHeadroomBytes || 0)) {
      console.error('FAIL: insufficient headroom for app code under install target');
      failed = true;
    }
  }

  if (failed) process.exit(1);
  console.log('Bundle size check passed');
}

main();

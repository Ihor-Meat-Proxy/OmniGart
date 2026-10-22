#!/usr/bin/env node
/**
 * OmniGart logo mark + colour variants + iOS alternate icons + Android adaptive /
 * monochrome launcher assets + preview PNGs.
 *
 * Mark concept: a tempered ring — a bold circular “Omni” with a diagonal grit
 * cut (гарт = temper/grit), reading as a forged G at small sizes.
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { Resvg } from '@resvg/resvg-js';
import sharp from 'sharp';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..');
const LOGO = path.join(ROOT, 'logo');
const GEN_IOS = path.join(ROOT, 'generated', 'ios', 'AppIcons');
const GEN_ANDROID = path.join(ROOT, 'generated', 'android', 'mipmap');

/** @type {Record<string, { bg: string, fg: string, accent: string, darkBg: string, darkFg: string, tintedBg: string, tintedFg: string }>} */
export const VARIANTS = {
  default: {
    bg: '#0F766E',
    fg: '#F0FDFA',
    accent: '#5EEAD4',
    darkBg: '#042F2E',
    darkFg: '#CCFBF1',
    tintedBg: '#14B8A6',
    tintedFg: '#042F2E',
  },
  ocean: {
    bg: '#0369A1',
    fg: '#E0F2FE',
    accent: '#7DD3FC',
    darkBg: '#0C4A6E',
    darkFg: '#E0F2FE',
    tintedBg: '#0EA5E9',
    tintedFg: '#082F49',
  },
  forest: {
    bg: '#166534',
    fg: '#ECFDF5',
    accent: '#86EFAC',
    darkBg: '#052E16',
    darkFg: '#DCFCE7',
    tintedBg: '#22C55E',
    tintedFg: '#052E16',
  },
  sunset: {
    bg: '#C2410C',
    fg: '#FFF7ED',
    accent: '#FDBA74',
    darkBg: '#7C2D12',
    darkFg: '#FFEDD5',
    tintedBg: '#F97316',
    tintedFg: '#431407',
  },
  graphite: {
    bg: '#334155',
    fg: '#F8FAFC',
    accent: '#94A3B8',
    darkBg: '#0F172A',
    darkFg: '#E2E8F0',
    tintedBg: '#64748B',
    tintedFg: '#020617',
  },
  sakura: {
    bg: '#BE185D',
    fg: '#FDF2F8',
    accent: '#F9A8D4',
    darkBg: '#831843',
    darkFg: '#FCE7F3',
    tintedBg: '#EC4899',
    tintedFg: '#500724',
  },
};

function markPaths(fg, accent) {
  // 1024 viewBox geometry — recognisable at 29pt / 48dp
  return `
  <!-- tempered ring -->
  <circle cx="512" cy="512" r="312" fill="none" stroke="${fg}" stroke-width="88" stroke-linecap="round"/>
  <!-- grit cut / G aperture -->
  <path d="M620 392 L780 552" fill="none" stroke="${fg}" stroke-width="88" stroke-linecap="round"/>
  <!-- temper spark -->
  <circle cx="700" cy="340" r="48" fill="${accent}"/>
`;
}

function appIconSvg({ bg, fg, accent, size = 1024 }) {
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="${size}" height="${size}">
  <rect width="1024" height="1024" rx="228" fill="${bg}"/>
  ${markPaths(fg, accent)}
</svg>
`;
}

function markOnlySvg({ fg, accent, size = 1024 }) {
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="${size}" height="${size}">
  ${markPaths(fg, accent)}
</svg>
`;
}

function adaptiveFgSvg({ fg, accent }) {
  // Safe zone: 66dp of 108dp — keep mark inside ~72% centre
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">
  <g transform="translate(54 54) scale(0.072) translate(-512 -512)">
    ${markPaths(fg, accent)}
  </g>
</svg>
`;
}

function monochromeSvg() {
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">
  <g transform="translate(54 54) scale(0.072) translate(-512 -512)" fill="#000" stroke="#000">
    <circle cx="512" cy="512" r="312" fill="none" stroke="#000000" stroke-width="88" stroke-linecap="round"/>
    <path d="M620 392 L780 552" fill="none" stroke="#000000" stroke-width="88" stroke-linecap="round"/>
    <circle cx="700" cy="340" r="48" fill="#000000"/>
  </g>
</svg>
`;
}

async function svgToPng(svg, outPath, width) {
  const resvg = new Resvg(svg, {
    fitTo: { mode: 'width', value: width },
    background: 'transparent',
  });
  const png = resvg.render().asPng();
  await sharp(png).png().toFile(outPath);
}

async function writeIosVariant(name, colors) {
  const base = path.join(GEN_IOS, `AppIcon-${name}.appiconset`);
  fs.mkdirSync(base, { recursive: true });

  const appearances = [
    { key: 'light', bg: colors.bg, fg: colors.fg, accent: colors.accent },
    { key: 'dark', bg: colors.darkBg, fg: colors.darkFg, accent: colors.accent },
    { key: 'tinted', bg: colors.tintedBg, fg: colors.tintedFg, accent: colors.tintedFg },
  ];

  const images = [];
  for (const a of appearances) {
    const svg = appIconSvg(a);
    const file = `icon-${a.key}-1024.png`;
    await svgToPng(svg, path.join(base, file), 1024);
    const entry = {
      filename: file,
      idiom: 'universal',
      platform: 'ios',
      size: '1024x1024',
    };
    if (a.key === 'dark') {
      entry.appearances = [{ appearance: 'luminosity', value: 'dark' }];
    } else if (a.key === 'tinted') {
      entry.appearances = [{ appearance: 'luminosity', value: 'tinted' }];
    }
    images.push(entry);
  }

  fs.writeFileSync(
    path.join(base, 'Contents.json'),
    JSON.stringify({ images, info: { author: 'omnigart', version: 1 } }, null, 2) + '\n',
  );
}

async function writeAndroidVariant(name, colors) {
  const dir = path.join(GEN_ANDROID, name);
  fs.mkdirSync(dir, { recursive: true });
  const fg = adaptiveFgSvg({ fg: colors.fg, accent: colors.accent });
  await svgToPng(fg, path.join(dir, 'ic_launcher_foreground.png'), 432);
  await svgToPng(
    `<?xml version="1.0"?><svg xmlns="http://www.w3.org/2000/svg" width="432" height="432"><rect width="432" height="432" fill="${colors.bg}"/></svg>`,
    path.join(dir, 'ic_launcher_background.png'),
    432,
  );
  await svgToPng(monochromeSvg(), path.join(dir, 'ic_launcher_monochrome.png'), 432);
  // Precomposed legacy fallback
  await svgToPng(appIconSvg(colors), path.join(dir, 'ic_launcher.png'), 512);

  const adaptiveXml = `<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/ic_launcher_${name}_background"/>
    <foreground android:drawable="@mipmap/ic_launcher_${name}_foreground"/>
    <monochrome android:drawable="@mipmap/ic_launcher_${name}_monochrome"/>
</adaptive-icon>
`;
  fs.writeFileSync(path.join(dir, 'ic_launcher.xml'), adaptiveXml);

  // Alias stub documentation file (merged into app module later)
  const alias = `<!-- activity-alias for set of launcher icon "${name}"
     android:name=".LauncherAlias_${name}"
     android:enabled="${name === 'default'}"
     android:icon="@mipmap/ic_launcher_${name}"
     android:roundIcon="@mipmap/ic_launcher_${name}"
     android:targetActivity=".MainActivity"
-->
`;
  fs.writeFileSync(path.join(dir, 'activity-alias-stub.xml'), alias);
}

async function writePreviews() {
  const cells = Object.keys(VARIANTS);
  const size = 160;
  const gap = 24;
  const cols = 3;
  const rows = Math.ceil(cells.length / cols);
  const width = cols * size + (cols + 1) * gap;
  const height = rows * (size + 36) + gap + 40;
  const composites = [];
  let labels = `<text x="${gap}" y="32" font-family="ui-sans-serif,system-ui,sans-serif" font-size="20" font-weight="600" fill="#0F172A">OmniGart app icon variants</text>`;

  for (let i = 0; i < cells.length; i++) {
    const name = cells[i];
    const c = VARIANTS[name];
    const col = i % cols;
    const row = Math.floor(i / cols);
    const x = gap + col * (size + gap);
    const y = 56 + row * (size + 36);
    const svg = appIconSvg(c);
    const resvg = new Resvg(svg, { fitTo: { mode: 'width', value: size } });
    composites.push({ input: resvg.render().asPng(), left: x, top: y });
    labels += `<text x="${x + size / 2}" y="${y + size + 22}" text-anchor="middle" font-family="ui-sans-serif,system-ui,sans-serif" font-size="13" fill="#475569">${name}</text>`;
  }

  const bg = await sharp(
    Buffer.from(
      `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}"><rect width="100%" height="100%" fill="#F8FAFC"/>${labels}</svg>`,
    ),
  )
    .png()
    .toBuffer();

  await sharp(bg)
    .composite(composites)
    .png()
    .toFile(path.join(LOGO, 'preview-variants.png'));

  // Mark-only preview
  const mark = markOnlySvg({ fg: '#0F766E', accent: '#14B8A6' });
  await svgToPng(mark, path.join(LOGO, 'preview-mark.png'), 512);
}

async function main() {
  fs.mkdirSync(path.join(LOGO, 'variants'), { recursive: true });
  fs.mkdirSync(GEN_IOS, { recursive: true });
  fs.mkdirSync(GEN_ANDROID, { recursive: true });

  // Master mark SVG (transparent)
  fs.writeFileSync(
    path.join(LOGO, 'omnigart-mark.svg'),
    markOnlySvg({ fg: '#0F766E', accent: '#14B8A6' }),
  );

  for (const [name, colors] of Object.entries(VARIANTS)) {
    const svg = appIconSvg(colors);
    fs.writeFileSync(path.join(LOGO, 'variants', `${name}.svg`), svg);
    // Appearance sources
    fs.writeFileSync(
      path.join(LOGO, 'variants', `${name}-dark.svg`),
      appIconSvg({ bg: colors.darkBg, fg: colors.darkFg, accent: colors.accent }),
    );
    fs.writeFileSync(
      path.join(LOGO, 'variants', `${name}-tinted.svg`),
      appIconSvg({ bg: colors.tintedBg, fg: colors.tintedFg, accent: colors.tintedFg }),
    );
    await writeIosVariant(name, colors);
    await writeAndroidVariant(name, colors);
  }

  await writePreviews();
  console.log(`Logo variants: ${Object.keys(VARIANTS).join(', ')}`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});

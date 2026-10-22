/**
 * Style Dictionary 5.7.0 — OmniGart token build (DTCG / W3C design tokens).
 * Verified latest stable: https://www.npmjs.com/package/style-dictionary (5.7.0, 2026-10-16)
 */
import StyleDictionary from 'style-dictionary';
import { formats, transformGroups } from 'style-dictionary/enums';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..');
const GEN = path.join(ROOT, 'generated');

function hexToSwiftUI(hex) {
  const h = hex.replace('#', '');
  const hasAlpha = h.length === 8;
  const r = parseInt(h.slice(0, 2), 16) / 255;
  const g = parseInt(h.slice(2, 4), 16) / 255;
  const b = parseInt(h.slice(4, 6), 16) / 255;
  const a = hasAlpha ? parseInt(h.slice(6, 8), 16) / 255 : 1;
  const f = (n) => n.toFixed(3);
  if (a === 1) return `Color(red: ${f(r)}, green: ${f(g)}, blue: ${f(b)})`;
  return `Color(red: ${f(r)}, green: ${f(g)}, blue: ${f(b)}, opacity: ${f(a)})`;
}

function hexToAndroidColor(hex) {
  const h = hex.replace('#', '');
  if (h.length === 6) return `#FF${h.toUpperCase()}`;
  if (h.length === 8) {
    const rgb = h.slice(0, 6).toUpperCase();
    const a = h.slice(6, 8).toUpperCase();
    return `#${a}${rgb}`;
  }
  return `#FF${h}`;
}

function dimPx(value) {
  if (typeof value === 'number') return value;
  const m = String(value).match(/^([\d.]+)px$/);
  return m ? parseFloat(m[1]) : value;
}

function camelPath(pathArr) {
  return pathArr
    .map((p, i) => (i === 0 ? p : p[0].toUpperCase() + p.slice(1)))
    .join('')
    .replace(/[^a-zA-Z0-9]/g, '');
}

function tokenPath(t) {
  if (Array.isArray(t.path) && t.path.length) return t.path;
  if (typeof t.key === 'string') {
    return t.key.replace(/^\{|\}$/g, '').split('.');
  }
  return [];
}

function flatTokens(dictionary) {
  return dictionary.allTokens;
}

StyleDictionary.registerFormat({
  name: 'omnigart/swift/colors',
  format: ({ dictionary }) => {
    const colors = flatTokens(dictionary).filter((t) => t.$type === 'color' || t.type === 'color');
    const lines = colors.map((t) => {
      const name = camelPath(tokenPath(t));
      const hex = t.$value || t.value;
      return `    public static let ${name} = ${hexToSwiftUI(hex)}`;
    });
    return `// Generated from design/tokens/omnigart.tokens.json — do not edit.
import SwiftUI

public extension Color {
    enum OmniGart {
${lines.join('\n')}
    }
}
`;
  },
});

StyleDictionary.registerFormat({
  name: 'omnigart/swift/typography',
  format: ({ dictionary }) => {
    const sizes = flatTokens(dictionary).filter((t) => {
      const p = tokenPath(t);
      return p[0] === 'font' && p[1] === 'size';
    });
    const lines = sizes.map((t) => {
      const name = camelPath(tokenPath(t).slice(2));
      const px = dimPx(t.$value || t.value);
      return `    public static let ${name} = Font.system(size: ${px}, weight: .regular, design: .default)`;
    });
    return `// Generated from design/tokens/omnigart.tokens.json — do not edit.
import SwiftUI

public extension Font {
    enum OmniGart {
${lines.join('\n')}
    }
}
`;
  },
});

StyleDictionary.registerFormat({
  name: 'omnigart/swift/metrics',
  format: ({ dictionary }) => {
    const spaces = flatTokens(dictionary).filter((t) => tokenPath(t)[0] === 'space');
    const radii = flatTokens(dictionary).filter((t) => tokenPath(t)[0] === 'radius');
    const motion = flatTokens(dictionary).filter((t) => {
      const p = tokenPath(t);
      return p[0] === 'motion' && p[1] === 'duration';
    });
    const spaceLines = spaces.map((t) => {
      const name = camelPath(['space', ...tokenPath(t).slice(1)]);
      return `    public static let ${name}: CGFloat = ${dimPx(t.$value || t.value)}`;
    });
    const radiusLines = radii.map((t) => {
      const name = camelPath(['radius', ...tokenPath(t).slice(1)]);
      return `    public static let ${name}: CGFloat = ${dimPx(t.$value || t.value)}`;
    });
    const motionLines = motion.map((t) => {
      const name = camelPath(['duration', ...tokenPath(t).slice(2)]);
      const raw = String(t.$value || t.value);
      const ms = parseFloat(raw);
      return `    public static let ${name}: Double = ${(ms / 1000).toFixed(3)}`;
    });
    return `// Generated from design/tokens/omnigart.tokens.json — do not edit.
import CoreGraphics

public enum OmniGartMetrics {
${spaceLines.join('\n')}

${radiusLines.join('\n')}

${motionLines.join('\n')}
}
`;
  },
});

StyleDictionary.registerFormat({
  name: 'omnigart/android/colors-xml',
  format: ({ dictionary }) => {
    const colors = flatTokens(dictionary).filter((t) => t.$type === 'color' || t.type === 'color');
    const lines = colors.map((t) => {
      const name = `og_${tokenPath(t).join('_')}`.replace(/__/g, '_').toLowerCase();
      return `    <color name="${name}">${hexToAndroidColor(t.$value || t.value)}</color>`;
    });
    return `<?xml version="1.0" encoding="utf-8"?>
<!-- Generated from design/tokens/omnigart.tokens.json — do not edit. -->
<resources>
${lines.join('\n')}
</resources>
`;
  },
});

StyleDictionary.registerFormat({
  name: 'omnigart/android/dimens-xml',
  format: ({ dictionary }) => {
    const dims = flatTokens(dictionary).filter((t) => {
      const type = t.$type || t.type;
      const p = tokenPath(t);
      return type === 'dimension' && (p[0] === 'space' || p[0] === 'radius' || p[0] === 'layout');
    });
    const lines = dims.map((t) => {
      const name = `og_${tokenPath(t).join('_')}`.toLowerCase();
      const px = dimPx(t.$value || t.value);
      return `    <dimen name="${name}">${px}dp</dimen>`;
    });
    return `<?xml version="1.0" encoding="utf-8"?>
<!-- Generated from design/tokens/omnigart.tokens.json — do not edit. -->
<resources>
${lines.join('\n')}
</resources>
`;
  },
});

StyleDictionary.registerFormat({
  name: 'omnigart/compose/theme',
  format: ({ dictionary }) => {
    const colors = flatTokens(dictionary).filter((t) => t.$type === 'color' || t.type === 'color');
    const colorProps = colors.map((t) => {
      const name = camelPath(tokenPath(t));
      const hex = String(t.$value || t.value).replace('#', '');
      if (hex.length === 8) {
        const rgb = hex.slice(0, 6);
        const a = hex.slice(6, 8);
        return `    val ${name} = Color(0x${a.toUpperCase()}${rgb.toUpperCase()})`;
      }
      return `    val ${name} = Color(0xFF${hex.toUpperCase()})`;
    });
    const spaces = flatTokens(dictionary).filter((t) => tokenPath(t)[0] === 'space');
    const spaceProps = spaces.map((t) => {
      const name = camelPath(['space', ...tokenPath(t).slice(1)]);
      return `    val ${name} = ${dimPx(t.$value || t.value)}.dp`;
    });
    const radii = flatTokens(dictionary).filter((t) => tokenPath(t)[0] === 'radius');
    const radiusProps = radii.map((t) => {
      const name = camelPath(['radius', ...tokenPath(t).slice(1)]);
      return `    val ${name} = ${dimPx(t.$value || t.value)}.dp`;
    });
    return `// Generated from design/tokens/omnigart.tokens.json — do not edit.
package com.omnigart.design.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

public object OmniGartColors {
${colorProps.join('\n')}
}

public object OmniGartSpacing {
${spaceProps.join('\n')}
}

public object OmniGartRadii {
${radiusProps.join('\n')}
}
`;
  },
});

function writeIosColorsets(dictionary) {
  const colors = flatTokens(dictionary).filter((t) => t.$type === 'color' || t.type === 'color');
  const catalog = path.join(GEN, 'ios', 'OmniGartColors.xcassets');
  fs.mkdirSync(catalog, { recursive: true });
  fs.writeFileSync(
    path.join(catalog, 'Contents.json'),
    JSON.stringify({ info: { author: 'omnigart', version: 1 } }, null, 2) + '\n',
  );

  for (const t of colors) {
    const parts = tokenPath(t);
    if (!parts.length) continue;
    const name = `og_${parts.join('_')}`;
    const dir = path.join(catalog, `${name}.colorset`);
    fs.mkdirSync(dir, { recursive: true });
    const hex = String(t.$value || t.value).replace('#', '');
    const hasAlpha = hex.length === 8;
    const r = parseInt(hex.slice(0, 2), 16) / 255;
    const g = parseInt(hex.slice(2, 4), 16) / 255;
    const b = parseInt(hex.slice(4, 6), 16) / 255;
    const a = hasAlpha ? parseInt(hex.slice(6, 8), 16) / 255 : 1;
    const comp = {
      colors: [
        {
          idiom: 'universal',
          color: {
            'color-space': 'srgb',
            components: {
              red: r.toFixed(3),
              green: g.toFixed(3),
              blue: b.toFixed(3),
              alpha: a.toFixed(3),
            },
          },
        },
      ],
      info: { author: 'omnigart', version: 1 },
    };
    fs.writeFileSync(path.join(dir, 'Contents.json'), JSON.stringify(comp, null, 2) + '\n');
  }
}

const sd = new StyleDictionary({
  usesDtcg: true,
  source: [path.join(ROOT, 'tokens', 'omnigart.tokens.json')],
  platforms: {
    ios: {
      transformGroup: transformGroups.js,
      buildPath: path.join(GEN, 'ios', 'Sources') + '/',
      files: [
        { destination: 'Color+OmniGart.swift', format: 'omnigart/swift/colors' },
        { destination: 'Font+OmniGart.swift', format: 'omnigart/swift/typography' },
        { destination: 'OmniGartMetrics.swift', format: 'omnigart/swift/metrics' },
      ],
    },
    android: {
      transformGroup: transformGroups.js,
      buildPath: path.join(GEN, 'android', 'values') + '/',
      files: [
        { destination: 'og_colors.xml', format: 'omnigart/android/colors-xml' },
        { destination: 'og_dimens.xml', format: 'omnigart/android/dimens-xml' },
      ],
    },
    compose: {
      transformGroup: transformGroups.js,
      buildPath: path.join(GEN, 'compose') + '/',
      files: [
        { destination: 'OmniGartTheme.kt', format: 'omnigart/compose/theme' },
      ],
    },
  },
});

await sd.hasInitialized;
await sd.buildAllPlatforms();
writeIosColorsets({ allTokens: sd.allTokens });
console.log('Tokens generated into design/generated/{ios,android,compose}');

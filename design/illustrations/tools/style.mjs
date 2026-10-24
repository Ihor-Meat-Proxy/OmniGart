/** Shared cartoon badge style for OmniGart illustrations. */

export const VB = 128;
export const BADGE_R = 58;

export const COLORS = {
  badge: '#FFFFFF',
  badgeStroke: '#E2E8F0',
  figure: '#94A3B8',
  figureStroke: '#64748B',
  accent: '#0F766E',
  equipment: '#334155',
  equipmentLight: '#475569',
  ground: '#CBD5E1',
};

export const STROKE = 2.25;

export function badgeBackground() {
  return [
    `<circle cx="64" cy="64" r="${BADGE_R}" fill="${COLORS.badge}" stroke="${COLORS.badgeStroke}" stroke-width="1.5"/>`,
  ];
}

export function svgDoc(inner, meta = '') {
  const data = meta ? ` data-omnigart="${meta}"` : '';
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${VB} ${VB}" width="${VB}" height="${VB}" fill="none"${data}>
${inner}
</svg>
`;
}

export function el(tag, attrs, children = '') {
  const a = Object.entries(attrs)
    .filter(([, v]) => v !== undefined && v !== null)
    .map(([k, v]) => `${k}="${v}"`)
    .join(' ');
  if (children === null) return `<${tag} ${a}/>`;
  if (!children) return `<${tag} ${a}/>`;
  return `<${tag} ${a}>${children}</${tag}>`;
}

export function circle(cx, cy, r, fill, stroke = null, sw = STROKE) {
  return el('circle', {
    cx,
    cy,
    r,
    fill,
    stroke: stroke || undefined,
    'stroke-width': stroke ? sw : undefined,
  });
}

export function rect(x, y, w, h, fill, extra = {}) {
  return el('rect', {
    x,
    y,
    width: w,
    height: h,
    rx: extra.rx ?? 2,
    fill,
    stroke: extra.stroke,
    'stroke-width': extra.stroke ? extra.sw ?? STROKE : undefined,
  });
}

export function path(d, fill, stroke = null, sw = STROKE) {
  return el('path', {
    d,
    fill,
    stroke: stroke || undefined,
    'stroke-width': stroke ? sw : undefined,
    'stroke-linecap': stroke ? 'round' : undefined,
    'stroke-linejoin': stroke ? 'round' : undefined,
  });
}

export function line(x1, y1, x2, y2, stroke = COLORS.equipment, sw = STROKE) {
  return el('line', {
    x1,
    y1,
    x2,
    y2,
    stroke,
    'stroke-width': sw,
    'stroke-linecap': 'round',
  });
}

export function ellipse(cx, cy, rx, ry, fill, stroke = null) {
  return el('ellipse', {
    cx,
    cy,
    rx,
    ry,
    fill,
    stroke: stroke || undefined,
    'stroke-width': stroke ? STROKE : undefined,
  });
}

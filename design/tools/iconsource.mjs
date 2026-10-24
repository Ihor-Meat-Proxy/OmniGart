/**
 * OmniGart original icon geometry — 24×24 grid, 1.75px rounded strokes.
 * Paths are hand-authored for this project; not derived from any icon library.
 */
export const STROKE = {
  fill: 'none',
  stroke: 'currentColor',
  'stroke-width': '1.75',
  'stroke-linecap': 'round',
  'stroke-linejoin': 'round',
};

/** @type {Record<string, { category: string, keywords: string[], paths: string[], circles?: Array<{cx:number,cy:number,r:number,fill?:boolean}>, lines?: Array<{x1:number,y1:number,x2:number,y2:number}> }>} */
export const ICONS = {
  // —— Tabs ——
  food: {
    category: 'tab',
    keywords: ['nutrition', 'meal', 'plate', 'tab'],
    paths: [
      'M4.5 14.5c0 3.5 3.2 5.5 7.5 5.5s7.5-2 7.5-5.5',
      'M7 10.5c0-1.2.9-2.2 2-2.2h6c1.1 0 2 1 2 2.2',
      'M9.2 8.3V6.8c0-.7.5-1.3 1.2-1.3h3.2c.7 0 1.2.6 1.2 1.3v1.5',
    ],
  },
  workouts: {
    category: 'tab',
    keywords: ['exercise', 'dumbbell', 'training', 'tab'],
    paths: [
      'M7.5 9.5v5',
      'M16.5 9.5v5',
      'M5 10.5v3',
      'M19 10.5v3',
      'M7.5 12h9',
      'M5 12H3.8',
      'M19 12h1.2',
    ],
  },
  recovery: {
    category: 'tab',
    keywords: ['heal', 'leaf', 'rest', 'tab'],
    paths: [
      'M12 19.5V9.5',
      'M12 9.5C12 6.5 14.8 4.5 18 4.5c0 3.2-2 6-5.5 6.5',
      'M12 9.5C12 6.8 9.4 5 6.5 5.2c.4 3 2.4 5.5 5.5 6',
    ],
  },
  planner: {
    category: 'tab',
    keywords: ['calendar', 'schedule', 'plan', 'tab'],
    paths: [
      'M5.5 8.5h13a1.5 1.5 0 0 1 1.5 1.5v9a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 19V10a1.5 1.5 0 0 1 1.5-1.5z',
      'M8 5.5v3',
      'M16 5.5v3',
      'M4 11.5h16',
    ],
    circles: [
      { cx: 8.5, cy: 15, r: 0.9, fill: true },
      { cx: 12, cy: 15, r: 0.9, fill: true },
      { cx: 15.5, cy: 15, r: 0.9, fill: true },
    ],
  },
  profile: {
    category: 'tab',
    keywords: ['user', 'account', 'person', 'tab'],
    paths: [
      'M12 12.2a3.2 3.2 0 1 0 0-6.4 3.2 3.2 0 0 0 0 6.4z',
      'M5.5 19c1.6-3 4-4.5 6.5-4.5s4.9 1.5 6.5 4.5',
    ],
  },
  insights: {
    category: 'tab',
    keywords: ['chart', 'trends', 'analytics', 'tab'],
    paths: [
      'M4.5 18.5V8.5',
      'M9.5 18.5V5.5',
      'M14.5 18.5v-7',
      'M19.5 18.5V10',
      'M4.5 18.5h15',
    ],
  },
  health: {
    category: 'tab',
    keywords: ['pulse', 'vitals', 'metrics', 'tab'],
    paths: [
      'M4.5 12h2.8l1.6-4 2.4 8 1.8-4.2H19.5',
      'M12 4.5c-1.2 0-2.2.6-2.8 1.5',
      'M12 4.5c1.2 0 2.2.6 2.8 1.5',
    ],
  },

  // —— Quick log ——
  water: {
    category: 'quick-log',
    keywords: ['hydrate', 'drop', 'drink'],
    paths: ['M12 4.5c0 0 5.5 6.2 5.5 10.2a5.5 5.5 0 1 1-11 0C6.5 10.7 12 4.5 12 4.5z'],
  },
  barcode: {
    category: 'quick-log',
    keywords: ['scan', 'upc', 'product'],
    lines: [
      { x1: 5, y1: 6, x2: 5, y2: 18 },
      { x1: 7.5, y1: 6, x2: 7.5, y2: 18 },
      { x1: 9.2, y1: 6, x2: 9.2, y2: 18 },
      { x1: 12, y1: 6, x2: 12, y2: 18 },
      { x1: 14, y1: 6, x2: 14, y2: 18 },
      { x1: 16.5, y1: 6, x2: 16.5, y2: 18 },
      { x1: 19, y1: 6, x2: 19, y2: 18 },
    ],
  },
  'food-log': {
    category: 'quick-log',
    keywords: ['fork', 'meal', 'eat'],
    paths: [
      'M8 4.5v6.5c0 1.2.8 2 2 2h0',
      'M8 4.5V8',
      'M6.2 4.5V7.5',
      'M9.8 4.5V7.5',
      'M8 13v6.5',
      'M14 4.5v15',
      'M14 8.5c2.2 0 3.5 1.2 3.5 3.2V12',
    ],
  },
  'add-set': {
    category: 'quick-log',
    keywords: ['plus', 'rep', 'strength', 'log'],
    paths: [
      'M7.5 9.8v4.4',
      'M16.5 9.8v4.4',
      'M7.5 12h5.2',
      'M5.2 10.6v2.8',
      'M18.8 10.6v2.8',
    ],
    circles: [{ cx: 17.5, cy: 6.5, r: 3.2 }],
    lines: [
      { x1: 17.5, y1: 5.2, x2: 17.5, y2: 7.8 },
      { x1: 16.2, y1: 6.5, x2: 18.8, y2: 6.5 },
    ],
  },

  // —— Disciplines ——
  strength: {
    category: 'discipline',
    keywords: ['weights', 'lift', 'gym'],
    paths: [
      'M4.5 12h15',
      'M7 8.5v7',
      'M17 8.5v7',
      'M5.2 10v4',
      'M18.8 10v4',
    ],
  },
  running: {
    category: 'discipline',
    keywords: ['run', 'cardio', 'jog'],
    paths: [
      'M14.2 6.2a1.6 1.6 0 1 0 0-3.2 1.6 1.6 0 0 0 0 3.2z',
      'M9.5 11.5l2.2-2.2 2.8 1.6 2.2-2',
      'M11.7 9.3L9 14.5l3.2 1.2',
      'M9 14.5L6.5 19',
      'M12.2 15.7L14 19',
      'M16.9 8.9l2.6 1.2',
    ],
  },
  swimming: {
    category: 'discipline',
    keywords: ['swim', 'pool', 'laps'],
    paths: [
      'M4.5 15.5c1.5-1.2 3-1.2 4.5 0s3 1.2 4.5 0 3-1.2 4.5 0 3 1.2 4.5 0',
      'M4.5 19c1.5-1.2 3-1.2 4.5 0s3 1.2 4.5 0 3-1.2 4.5 0 3 1.2 4.5 0',
      'M8.5 12.5c1.2-2.5 3.2-4 5.5-4.2',
      'M12.5 5.5a1.4 1.4 0 1 0 0-2.8 1.4 1.4 0 0 0 0 2.8z',
    ],
  },
  yoga: {
    category: 'discipline',
    keywords: ['stretch', 'pose', 'mindfulness'],
    paths: [
      'M12 7.2a1.6 1.6 0 1 0 0-3.2 1.6 1.6 0 0 0 0 3.2z',
      'M12 7.8v4.2',
      'M8 20.2L12 12l4 8.2',
      'M7.2 11.5L12 12l4.8-.5',
    ],
  },
  pilates: {
    category: 'discipline',
    keywords: ['core', 'mat', 'control'],
    paths: [
      'M12 6.8a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z',
      'M12 7.5v5',
      'M8.5 20c1-3.5 2.2-5.5 3.5-5.5S14.5 16.5 15.5 20',
      'M7 12.5h10',
    ],
  },

  // —— Recovery / biometrics ——
  sleep: {
    category: 'recovery',
    keywords: ['moon', 'night', 'rest'],
    paths: ['M15.5 5.2A7.2 7.2 0 1 0 18.8 15 5.8 5.8 0 0 1 15.5 5.2z'],
  },
  hrv: {
    category: 'recovery',
    keywords: ['heart', 'variability', 'pulse'],
    paths: [
      'M12 19s-6.5-4.2-6.5-9A3.7 3.7 0 0 1 12 7.8 3.7 3.7 0 0 1 18.5 10c0 4.8-6.5 9-6.5 9z',
      'M8.2 12.2h2.2l1.2-2.4 1.6 4.2 1.1-1.8H15.8',
    ],
  },
  stress: {
    category: 'recovery',
    keywords: ['tension', 'wave', 'load'],
    paths: [
      'M4.5 14.5c1.5-4 2.8-6 4-6s2.2 5 3.5 5 2.2-8 3.5-8 2.5 9 4 9',
    ],
  },
  'heart-rate': {
    category: 'recovery',
    keywords: ['bpm', 'cardio', 'monitor'],
    paths: [
      'M4.5 12h3.2l1.5-3.5 2.2 7 1.8-4.2H19.5',
    ],
  },
  readiness: {
    category: 'recovery',
    keywords: ['gauge', 'status', 'morning'],
    paths: [
      'M6.2 16.5a7.5 7.5 0 1 1 11.6 0',
      'M12 14.2V9.5',
    ],
    circles: [{ cx: 12, cy: 14.2, r: 1.1, fill: true }],
  },

  // —— Body ——
  measurements: {
    category: 'body',
    keywords: ['tape', 'anthropometry', 'size'],
    paths: [
      'M5 8.5h14v7H5z',
      'M8 8.5v7',
      'M11 8.5v7',
      'M14 8.5v7',
      'M17 8.5v7',
    ],
  },
  weight: {
    category: 'body',
    keywords: ['scale', 'kg', 'mass'],
    paths: [
      'M6.5 8.5h11l1.5 11h-14l1.5-11z',
      'M10 8.5V6.8a2 2 0 0 1 4 0v1.7',
      'M9.5 13.5h5',
    ],
  },

  // —— Achievements ——
  achievement: {
    category: 'achievement',
    keywords: ['medal', 'badge', 'award'],
    paths: [
      'M12 13.5a4.2 4.2 0 1 0 0-8.4 4.2 4.2 0 0 0 0 8.4z',
      'M9.8 12.8L8 19.5l4-2.2 4 2.2-1.8-6.7',
    ],
  },
  streak: {
    category: 'achievement',
    keywords: ['flame', 'days', 'consistency'],
    paths: [
      'M12 19.5c3.2 0 5.5-2.2 5.5-5.5 0-3.2-2.4-5.2-3.8-6.8-.4 1.6-1.2 2.6-2.2 3.2.2-2.4-.4-4.2-1.8-5.9-2.4 2.2-3.7 4.8-3.7 7.5 0 3.3 2.3 5.5 6 5.5z',
    ],
  },
  trophy: {
    category: 'achievement',
    keywords: ['win', 'cup', 'goal'],
    paths: [
      'M8.5 5.5h7v4.2c0 2.4-1.6 4.3-3.5 4.3s-3.5-1.9-3.5-4.3V5.5z',
      'M8.5 7H5.8c0 2.2 1.2 3.5 2.7 3.8',
      'M15.5 7h2.7c0 2.2-1.2 3.5-2.7 3.8',
      'M10 14v2.5h4V14',
      'M9 19.5h6',
    ],
  },

  // —— System ——
  settings: {
    category: 'system',
    keywords: ['gear', 'preferences', 'config'],
    paths: [
      'M12 14.8a2.8 2.8 0 1 0 0-5.6 2.8 2.8 0 0 0 0 5.6z',
      'M12 4.5v1.6M12 17.9v1.6M4.5 12h1.6M17.9 12h1.6M6.4 6.4l1.1 1.1M16.5 16.5l1.1 1.1M17.6 6.4l-1.1 1.1M7.5 16.5l-1.1 1.1',
    ],
  },
  evidence: {
    category: 'system',
    keywords: ['info', 'citation', 'science', 'about'],
    paths: [
      'M12 19.5a7.5 7.5 0 1 0 0-15 7.5 7.5 0 0 0 0 15z',
      'M12 11.2v4.2',
    ],
    circles: [{ cx: 12, cy: 8.6, r: 0.85, fill: true }],
  },
  search: {
    category: 'system',
    keywords: ['find', 'lookup'],
    paths: [
      'M11 16.5a5.5 5.5 0 1 0 0-11 5.5 5.5 0 0 0 0 11z',
      'M15.2 15.2L19 19',
    ],
  },
  check: {
    category: 'system',
    keywords: ['done', 'confirm', 'success'],
    paths: ['M5.5 12.5l4.2 4.2 8.8-9.4'],
  },
  close: {
    category: 'system',
    keywords: ['dismiss', 'cancel', 'x'],
    paths: ['M6.5 6.5l11 11M17.5 6.5l-11 11'],
  },
  edit: {
    category: 'system',
    keywords: ['pencil', 'modify'],
    paths: [
      'M14.2 5.8l4 4L9 19H5v-4L14.2 5.8z',
      'M12.8 7.2l4 4',
    ],
  },
  timer: {
    category: 'system',
    keywords: ['clock', 'rest', 'interval'],
    paths: [
      'M12 19.5a7 7 0 1 0 0-14 7 7 0 0 0 0 14z',
      'M12 9.5v3.8l2.5 1.5',
      'M10 4.5h4',
    ],
  },
  share: {
    category: 'system',
    keywords: ['export', 'send'],
    paths: [
      'M12 14.5V5.5',
      'M8.5 8.5L12 5l3.5 3.5',
      'M6 13.5v4.5a1.5 1.5 0 0 0 1.5 1.5h9a1.5 1.5 0 0 0 1.5-1.5v-4.5',
    ],
  },
  chevron: {
    category: 'system',
    keywords: ['expand', 'disclosure', 'down'],
    paths: ['M7 10l5 5 5-5'],
  },

  // —— Muscle groups ——
  'muscle-chest': {
    category: 'muscle',
    keywords: ['pectoral', 'upper body'],
    paths: [
      'M5.5 9.5c2-2.5 4-3.5 6.5-3.5s4.5 1 6.5 3.5',
      'M5.5 9.5c1.5 3 2.5 6 2.5 8.5',
      'M18.5 9.5c-1.5 3-2.5 6-2.5 8.5',
      'M8 14.5h8',
    ],
  },
  'muscle-back': {
    category: 'muscle',
    keywords: ['lats', 'posterior'],
    paths: [
      'M12 5.5v13',
      'M12 7.5c-2.5 1.5-4.5 4-5.5 7.5',
      'M12 7.5c2.5 1.5 4.5 4 5.5 7.5',
      'M8.5 11.5h7',
    ],
  },
  'muscle-shoulders': {
    category: 'muscle',
    keywords: ['delts', 'upper'],
    paths: [
      'M7 10.5a3 3 0 1 1-2.2-4.8',
      'M17 10.5a3 3 0 1 0 2.2-4.8',
      'M8.2 8.2c1.2-.8 2.5-1.2 3.8-1.2s2.6.4 3.8 1.2',
      'M12 7.5v9',
    ],
  },
  'muscle-arms': {
    category: 'muscle',
    keywords: ['biceps', 'triceps'],
    paths: [
      'M7.5 18.5V12c0-1.5.8-2.8 2-3.5L12 7l2.5 1.5c1.2.7 2 2 2 3.5v6.5',
      'M9.5 12.5c.8-1.5 1.6-2.2 2.5-2.2s1.7.7 2.5 2.2',
    ],
  },
  'muscle-core': {
    category: 'muscle',
    keywords: ['abs', 'midsection'],
    paths: [
      'M8 6.5h8v11H8z',
      'M12 6.5v11',
      'M8 10h8',
      'M8 13.5h8',
    ],
  },
  'muscle-legs': {
    category: 'muscle',
    keywords: ['quads', 'hamstrings', 'lower'],
    paths: [
      'M9 5.5v6.5L7.5 19.5',
      'M15 5.5v6.5l1.5 7.5',
      'M9 12h6',
      'M10.5 5.5h3',
    ],
  },
  'muscle-glutes': {
    category: 'muscle',
    keywords: ['hips', 'posterior chain'],
    paths: [
      'M7.5 10.5c0-2 2-3.5 4.5-3.5s4.5 1.5 4.5 3.5c0 3-1.5 5.5-4.5 8-3-2.5-4.5-5-4.5-8z',
      'M12 7v11.5',
    ],
  },
};

export const ICON_NAMES = Object.keys(ICONS);

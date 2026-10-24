/**
 * Reusable cartoon body poses. Neutral grey figure; callers pass which
 * regions to paint with the accent colour.
 */
import { COLORS, STROKE, circle, path, ellipse, rect } from '../style.mjs';

const F = COLORS.figure;
const FS = COLORS.figureStroke;
const A = COLORS.accent;

/** Map content muscle ids → local highlight regions. */
export const MUSCLE_REGIONS = {
  pectoralis_major: ['chest'],
  front_deltoid: ['shoulder_l', 'shoulder_r'],
  rear_deltoid: ['shoulder_l', 'shoulder_r'],
  side_deltoid: ['shoulder_l', 'shoulder_r'],
  deltoids: ['shoulder_l', 'shoulder_r'],
  biceps: ['arm_l', 'arm_r'],
  triceps: ['arm_l', 'arm_r'],
  latissimus_dorsi: ['back', 'lat_l', 'lat_r'],
  trapezius: ['traps'],
  rhomboids: ['back'],
  erector_spinae: ['back'],
  rectus_abdominis: ['core'],
  obliques: ['core'],
  transverse_abdominis: ['core'],
  hip_flexors: ['hip_l', 'hip_r'],
  gluteus_maximus: ['glute_l', 'glute_r'],
  gluteus_medius: ['glute_l', 'glute_r'],
  quadriceps: ['quad_l', 'quad_r'],
  hamstrings: ['ham_l', 'ham_r'],
  adductors: ['quad_l', 'quad_r'],
  calves: ['calf_l', 'calf_r'],
  soleus: ['calf_l', 'calf_r'],
  forearm_flexors: ['forearm_l', 'forearm_r'],
  forearm_extensors: ['forearm_l', 'forearm_r'],
  full_body: [
    'chest',
    'core',
    'back',
    'arm_l',
    'arm_r',
    'quad_l',
    'quad_r',
    'glute_l',
    'glute_r',
  ],
  rotator_cuff: ['shoulder_l', 'shoulder_r'],
};

export function regionsForMuscles(primary = [], secondary = []) {
  const set = new Set();
  for (const m of [...primary, ...secondary.slice(0, 2)]) {
    for (const r of MUSCLE_REGIONS[m] || []) set.add(r);
  }
  if (set.size === 0) set.add('core');
  return set;
}

function fill(region, highlights) {
  return highlights.has(region) ? A : F;
}

/** Standing neutral figure (front), centred in badge. */
export function poseStand(highlights = new Set()) {
  const parts = [];
  // legs
  parts.push(ellipse(50, 92, 7, 16, fill('quad_l', highlights), FS));
  parts.push(ellipse(78, 92, 7, 16, fill('quad_r', highlights), FS));
  parts.push(ellipse(50, 108, 5.5, 10, fill('calf_l', highlights), FS));
  parts.push(ellipse(78, 108, 5.5, 10, fill('calf_r', highlights), FS));
  // glutes (slight)
  parts.push(ellipse(52, 78, 8, 6, fill('glute_l', highlights), FS));
  parts.push(ellipse(76, 78, 8, 6, fill('glute_r', highlights), FS));
  // torso
  parts.push(
    path(
      'M46 42 C46 34 54 30 64 30 C74 30 82 34 82 42 L80 74 C80 80 72 84 64 84 C56 84 48 80 48 74 Z',
      fill('core', highlights),
      FS,
    ),
  );
  parts.push(
    path(
      'M48 40 C52 36 58 34 64 34 C70 34 76 36 80 40 L78 54 C74 52 70 50 64 50 C58 50 54 52 50 54 Z',
      fill('chest', highlights),
      FS,
    ),
  );
  // arms
  parts.push(ellipse(38, 52, 5.5, 14, fill('arm_l', highlights), FS));
  parts.push(ellipse(90, 52, 5.5, 14, fill('arm_r', highlights), FS));
  parts.push(ellipse(36, 68, 4.5, 10, fill('forearm_l', highlights), FS));
  parts.push(ellipse(92, 68, 4.5, 10, fill('forearm_r', highlights), FS));
  // shoulders
  parts.push(circle(42, 40, 6, fill('shoulder_l', highlights), FS));
  parts.push(circle(86, 40, 6, fill('shoulder_r', highlights), FS));
  // head
  parts.push(circle(64, 22, 10, F, FS));
  return parts.join('\n  ');
}

/** Squat / sit depth. */
export function poseSquat(highlights = new Set()) {
  const parts = [];
  parts.push(ellipse(46, 98, 8, 12, fill('quad_l', highlights), FS));
  parts.push(ellipse(82, 98, 8, 12, fill('quad_r', highlights), FS));
  parts.push(ellipse(42, 112, 5, 8, fill('calf_l', highlights), FS));
  parts.push(ellipse(86, 112, 5, 8, fill('calf_r', highlights), FS));
  parts.push(ellipse(50, 84, 9, 7, fill('glute_l', highlights), FS));
  parts.push(ellipse(78, 84, 9, 7, fill('glute_r', highlights), FS));
  parts.push(
    path(
      'M48 48 C48 40 56 36 64 36 C72 36 80 40 80 48 L78 78 C78 84 70 88 64 88 C58 88 50 84 50 78 Z',
      fill('core', highlights),
      FS,
    ),
  );
  parts.push(
    path(
      'M50 46 C54 42 58 40 64 40 C70 40 74 42 78 46 L76 58 C70 56 64 56 58 58 Z',
      fill('chest', highlights),
      FS,
    ),
  );
  parts.push(ellipse(36, 56, 5.5, 12, fill('arm_l', highlights), FS));
  parts.push(ellipse(92, 56, 5.5, 12, fill('arm_r', highlights), FS));
  parts.push(circle(42, 44, 6, fill('shoulder_l', highlights), FS));
  parts.push(circle(86, 44, 6, fill('shoulder_r', highlights), FS));
  parts.push(circle(64, 26, 10, F, FS));
  return parts.join('\n  ');
}

/** Hip hinge / RDL. */
export function poseHinge(highlights = new Set()) {
  const parts = [];
  parts.push(ellipse(58, 100, 7, 16, fill('ham_l', highlights), FS));
  parts.push(ellipse(74, 100, 7, 16, fill('ham_r', highlights), FS));
  parts.push(ellipse(58, 114, 5, 8, fill('calf_l', highlights), FS));
  parts.push(ellipse(74, 114, 5, 8, fill('calf_r', highlights), FS));
  parts.push(ellipse(60, 82, 9, 7, fill('glute_l', highlights), FS));
  parts.push(ellipse(76, 82, 9, 7, fill('glute_r', highlights), FS));
  // torso tilted
  parts.push(
    path(
      'M40 58 C44 44 54 38 66 42 C76 46 82 56 80 68 L70 78 C66 72 58 70 50 74 Z',
      fill('back', highlights),
      FS,
    ),
  );
  parts.push(
    path(
      'M44 52 C50 46 58 44 66 48 L70 58 C62 56 54 56 48 60 Z',
      fill('chest', highlights),
      FS,
    ),
  );
  parts.push(ellipse(34, 62, 5, 12, fill('arm_l', highlights), FS));
  parts.push(ellipse(52, 74, 5, 12, fill('arm_r', highlights), FS));
  parts.push(circle(48, 44, 6, fill('shoulder_l', highlights), FS));
  parts.push(circle(70, 48, 6, fill('shoulder_r', highlights), FS));
  parts.push(circle(58, 30, 10, F, FS));
  return parts.join('\n  ');
}

/** Push-up / plank. */
export function posePlank(highlights = new Set()) {
  const parts = [];
  parts.push(ellipse(36, 78, 14, 6, fill('quad_l', highlights), FS));
  parts.push(ellipse(36, 88, 14, 6, fill('quad_r', highlights), FS));
  parts.push(ellipse(18, 80, 8, 5, fill('calf_l', highlights), FS));
  parts.push(ellipse(18, 90, 8, 5, fill('calf_r', highlights), FS));
  parts.push(
    path(
      'M48 70 C60 66 78 66 92 72 L94 84 C78 80 60 80 48 84 Z',
      fill('core', highlights),
      FS,
    ),
  );
  parts.push(
    path(
      'M88 68 C96 70 104 74 108 80 L100 86 C96 80 90 76 84 76 Z',
      fill('chest', highlights),
      FS,
    ),
  );
  parts.push(ellipse(100, 92, 5, 10, fill('arm_l', highlights), FS));
  parts.push(ellipse(108, 92, 5, 10, fill('arm_r', highlights), FS));
  parts.push(circle(112, 72, 6, fill('shoulder_l', highlights), FS));
  parts.push(circle(104, 68, 5, fill('shoulder_r', highlights), FS));
  parts.push(circle(118, 64, 9, F, FS));
  return parts.join('\n  ');
}

/** Overhead / press. */
export function posePress(highlights = new Set()) {
  const parts = [];
  parts.push(ellipse(50, 96, 7, 14, fill('quad_l', highlights), FS));
  parts.push(ellipse(78, 96, 7, 14, fill('quad_r', highlights), FS));
  parts.push(ellipse(50, 110, 5, 8, fill('calf_l', highlights), FS));
  parts.push(ellipse(78, 110, 5, 8, fill('calf_r', highlights), FS));
  parts.push(
    path(
      'M48 48 C48 40 56 36 64 36 C72 36 80 40 80 48 L78 76 C78 82 70 86 64 86 C58 86 50 82 50 76 Z',
      fill('core', highlights),
      FS,
    ),
  );
  parts.push(
    path(
      'M50 46 C54 42 58 40 64 40 C70 40 74 42 78 46 L76 58 C70 56 64 56 58 58 Z',
      fill('chest', highlights),
      FS,
    ),
  );
  // arms up
  parts.push(ellipse(42, 28, 5.5, 14, fill('arm_l', highlights), FS));
  parts.push(ellipse(86, 28, 5.5, 14, fill('arm_r', highlights), FS));
  parts.push(circle(44, 42, 6, fill('shoulder_l', highlights), FS));
  parts.push(circle(84, 42, 6, fill('shoulder_r', highlights), FS));
  parts.push(circle(64, 24, 9, F, FS));
  return parts.join('\n  ');
}

/** Pull / hang. */
export function posePull(highlights = new Set()) {
  const parts = [];
  parts.push(ellipse(50, 100, 7, 14, fill('quad_l', highlights), FS));
  parts.push(ellipse(78, 100, 7, 14, fill('quad_r', highlights), FS));
  parts.push(
    path(
      'M48 52 C48 44 56 40 64 40 C72 40 80 44 80 52 L78 80 C78 86 70 90 64 90 C58 90 50 86 50 80 Z',
      fill('back', highlights),
      FS,
    ),
  );
  parts.push(
    path(
      'M50 50 C56 46 60 44 64 44 C68 44 72 46 78 50 L74 62 C70 58 64 58 58 62 Z',
      fill('lat_l', highlights),
      FS,
    ),
  );
  // arms up to bar
  parts.push(ellipse(40, 32, 5, 16, fill('arm_l', highlights), FS));
  parts.push(ellipse(88, 32, 5, 16, fill('arm_r', highlights), FS));
  parts.push(circle(44, 46, 6, fill('shoulder_l', highlights), FS));
  parts.push(circle(84, 46, 6, fill('shoulder_r', highlights), FS));
  parts.push(circle(64, 28, 9, F, FS));
  return parts.join('\n  ');
}

/** Lunge. */
export function poseLunge(highlights = new Set()) {
  const parts = [];
  parts.push(ellipse(42, 96, 7, 14, fill('quad_l', highlights), FS));
  parts.push(ellipse(88, 88, 7, 16, fill('quad_r', highlights), FS));
  parts.push(ellipse(38, 112, 5, 8, fill('calf_l', highlights), FS));
  parts.push(ellipse(98, 104, 5, 8, fill('calf_r', highlights), FS));
  parts.push(ellipse(54, 80, 8, 6, fill('glute_l', highlights), FS));
  parts.push(ellipse(72, 78, 8, 6, fill('glute_r', highlights), FS));
  parts.push(
    path(
      'M50 42 C50 34 58 30 64 30 C72 30 78 34 78 42 L76 72 C76 78 70 82 64 82 C58 82 52 78 52 72 Z',
      fill('core', highlights),
      FS,
    ),
  );
  parts.push(ellipse(40, 50, 5, 12, fill('arm_l', highlights), FS));
  parts.push(ellipse(88, 50, 5, 12, fill('arm_r', highlights), FS));
  parts.push(circle(44, 40, 6, fill('shoulder_l', highlights), FS));
  parts.push(circle(84, 40, 6, fill('shoulder_r', highlights), FS));
  parts.push(circle(64, 22, 10, F, FS));
  return parts.join('\n  ');
}

/** Seated / machine. */
export function poseSit(highlights = new Set()) {
  const parts = [];
  parts.push(ellipse(48, 100, 8, 12, fill('quad_l', highlights), FS));
  parts.push(ellipse(80, 100, 8, 12, fill('quad_r', highlights), FS));
  parts.push(rect(44, 78, 40, 14, fill('glute_l', highlights), { rx: 6, stroke: FS }));
  parts.push(
    path(
      'M48 40 C48 32 56 28 64 28 C72 28 80 32 80 40 L78 74 C78 78 70 82 64 82 C58 82 50 78 50 74 Z',
      fill('core', highlights),
      FS,
    ),
  );
  parts.push(
    path(
      'M50 40 C54 36 58 34 64 34 C70 34 74 36 78 40 L76 52 C70 50 64 50 58 52 Z',
      fill('chest', highlights),
      FS,
    ),
  );
  parts.push(ellipse(36, 54, 5, 12, fill('arm_l', highlights), FS));
  parts.push(ellipse(92, 54, 5, 12, fill('arm_r', highlights), FS));
  parts.push(circle(42, 42, 6, fill('shoulder_l', highlights), FS));
  parts.push(circle(86, 42, 6, fill('shoulder_r', highlights), FS));
  parts.push(circle(64, 20, 10, F, FS));
  return parts.join('\n  ');
}

/** Side plank / lateral. */
export function poseSide(highlights = new Set()) {
  const parts = [];
  parts.push(ellipse(40, 88, 16, 6, fill('quad_l', highlights), FS));
  parts.push(ellipse(64, 78, 18, 7, fill('core', highlights), FS));
  parts.push(ellipse(88, 70, 8, 6, fill('chest', highlights), FS));
  parts.push(ellipse(72, 56, 5, 12, fill('arm_l', highlights), FS));
  parts.push(ellipse(96, 52, 5, 10, fill('arm_r', highlights), FS));
  parts.push(circle(100, 44, 9, F, FS));
  parts.push(circle(70, 70, 6, fill('shoulder_l', highlights), FS));
  return parts.join('\n  ');
}

/** Run / gait. */
export function poseRun(highlights = new Set()) {
  const parts = [];
  parts.push(ellipse(44, 92, 6, 14, fill('quad_l', highlights), FS));
  parts.push(ellipse(82, 88, 6, 16, fill('quad_r', highlights), FS));
  parts.push(ellipse(38, 108, 5, 8, fill('calf_l', highlights), FS));
  parts.push(ellipse(92, 104, 5, 8, fill('calf_r', highlights), FS));
  parts.push(
    path(
      'M52 42 C52 34 58 30 64 30 C72 30 78 34 78 42 L74 72 C74 78 68 82 64 82 C58 82 54 78 54 72 Z',
      fill('core', highlights),
      FS,
    ),
  );
  parts.push(ellipse(40, 48, 5, 12, fill('arm_l', highlights), FS));
  parts.push(ellipse(88, 52, 5, 12, fill('arm_r', highlights), FS));
  parts.push(circle(46, 40, 6, fill('shoulder_l', highlights), FS));
  parts.push(circle(82, 40, 6, fill('shoulder_r', highlights), FS));
  parts.push(circle(64, 22, 10, F, FS));
  return parts.join('\n  ');
}

/** Swim horizontal. */
export function poseSwim(highlights = new Set()) {
  const parts = [];
  parts.push(ellipse(70, 64, 28, 10, fill('core', highlights), FS));
  parts.push(ellipse(98, 60, 10, 7, fill('chest', highlights), FS));
  parts.push(circle(112, 56, 8, F, FS));
  parts.push(ellipse(40, 70, 12, 5, fill('quad_l', highlights), FS));
  parts.push(ellipse(40, 58, 12, 5, fill('quad_r', highlights), FS));
  parts.push(ellipse(88, 48, 10, 4, fill('arm_l', highlights), FS));
  parts.push(ellipse(88, 78, 10, 4, fill('arm_r', highlights), FS));
  return parts.join('\n  ');
}

/** Yoga / floor stretch approximation. */
export function poseFloor(highlights = new Set()) {
  const parts = [];
  parts.push(ellipse(64, 96, 28, 8, fill('quad_l', highlights), FS));
  parts.push(
    path(
      'M40 70 C48 50 60 42 72 48 C82 52 86 64 80 78 L56 86 Z',
      fill('core', highlights),
      FS,
    ),
  );
  parts.push(ellipse(36, 78, 6, 12, fill('arm_l', highlights), FS));
  parts.push(ellipse(88, 72, 6, 12, fill('arm_r', highlights), FS));
  parts.push(circle(70, 40, 9, F, FS));
  return parts.join('\n  ');
}

export function poseForPattern(pattern, highlights) {
  switch (pattern) {
    case 'squat':
      return poseSquat(highlights);
    case 'hinge':
      return poseHinge(highlights);
    case 'lunge':
      return poseLunge(highlights);
    case 'push':
      return posePress(highlights);
    case 'pull':
      return posePull(highlights);
    case 'core_brace':
      return posePlank(highlights);
    case 'carry':
      return poseStand(highlights);
    case 'gait':
    case 'run_drill':
      return poseRun(highlights);
    case 'swim_stroke':
    case 'swim_drill':
      return poseSwim(highlights);
    case 'mobility':
    case 'balance':
      return poseFloor(highlights);
    case 'rotation':
      return poseSide(highlights);
    default:
      return poseStand(highlights);
  }
}

/** Tiny figure silhouette for equipment-only badges. */
export function tinyFigure() {
  return [
    circle(64, 40, 7, F, FS, 1.75),
    path(
      'M54 50 C54 46 58 44 64 44 C70 44 74 46 74 50 L72 72 C72 76 68 78 64 78 C60 78 56 76 56 72 Z',
      F,
      FS,
      1.75,
    ),
    ellipse(56, 88, 5, 10, F, FS),
    ellipse(72, 88, 5, 10, F, FS),
  ].join('\n  ');
}

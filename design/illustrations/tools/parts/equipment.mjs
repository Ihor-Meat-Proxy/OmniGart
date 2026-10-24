/**
 * Equipment primitives — dark grey, uniform stroke, flat cartoon.
 * Each shape returns SVG markup fragments (no root).
 */
import { COLORS, STROKE, circle, rect, path, line, ellipse } from '../style.mjs';

const E = COLORS.equipment;
const EL = COLORS.equipmentLight;

export function equipDumbbell(cx = 64, cy = 64, scale = 1) {
  const s = scale;
  return [
    rect(cx - 22 * s, cy - 3 * s, 44 * s, 6 * s, E, { rx: 2 }),
    rect(cx - 28 * s, cy - 10 * s, 8 * s, 20 * s, E, { rx: 2 }),
    rect(cx + 20 * s, cy - 10 * s, 8 * s, 20 * s, E, { rx: 2 }),
  ].join('\n  ');
}

export function equipKettlebell(cx = 64, cy = 70) {
  return [
    path(
      `M${cx - 14} ${cy - 8} C${cx - 14} ${cy - 28} ${cx + 14} ${cy - 28} ${cx + 14} ${cy - 8} L${cx + 12} ${cy + 16} C${cx + 12} ${cy + 28} ${cx - 12} ${cy + 28} ${cx - 12} ${cy + 16} Z`,
      E,
    ),
    path(
      `M${cx - 10} ${cy - 18} C${cx - 10} ${cy - 34} ${cx + 10} ${cy - 34} ${cx + 10} ${cy - 18}`,
      'none',
      E,
      STROKE + 1,
    ),
  ].join('\n  ');
}

export function equipBarbell(cx = 64, cy = 56) {
  return [
    line(cx - 40, cy, cx + 40, cy, E, 3),
    rect(cx - 42, cy - 10, 8, 20, E, { rx: 1 }),
    rect(cx - 36, cy - 14, 6, 28, EL, { rx: 1 }),
    rect(cx + 34, cy - 10, 8, 20, E, { rx: 1 }),
    rect(cx + 30, cy - 14, 6, 28, EL, { rx: 1 }),
  ].join('\n  ');
}

export function equipEzBar(cx = 64, cy = 56) {
  return [
    path(
      `M${cx - 38} ${cy} C${cx - 20} ${cy - 10} ${cx - 8} ${cy + 8} ${cx} ${cy} C${cx + 8} ${cy - 8} ${cx + 20} ${cy + 10} ${cx + 38} ${cy}`,
      'none',
      E,
      3,
    ),
    rect(cx - 42, cy - 8, 6, 16, E, { rx: 1 }),
    rect(cx + 36, cy - 8, 6, 16, E, { rx: 1 }),
  ].join('\n  ');
}

export function equipTrapBar() {
  return [
    path('M36 48 H92 V72 H76 V60 H52 V72 H36 Z', 'none', E, 3),
    rect(34, 44, 8, 12, E, { rx: 1 }),
    rect(86, 44, 8, 12, E, { rx: 1 }),
  ].join('\n  ');
}

export function equipPlates(cx = 64, cy = 64) {
  return [
    circle(cx - 14, cy, 18, 'none', E, 3),
    circle(cx - 14, cy, 6, E),
    circle(cx + 14, cy, 14, 'none', E, 3),
    circle(cx + 14, cy, 5, E),
  ].join('\n  ');
}

export function equipBand() {
  return [
    path('M36 40 C20 64 20 80 40 96', 'none', E, 3),
    path('M92 40 C108 64 108 80 88 96', 'none', E, 3),
    path('M40 40 C64 28 64 28 88 40', 'none', E, 3),
    path('M40 96 C64 108 64 108 88 96', 'none', E, 3),
  ].join('\n  ');
}

export function equipMiniBand() {
  return ellipse(64, 72, 28, 12, 'none', E);
}

export function equipTubeBand() {
  return [
    path('M30 50 C30 30 50 24 64 36 C78 24 98 30 98 50 C98 78 80 96 64 96 C48 96 30 78 30 50 Z', 'none', E, 3),
    circle(30, 50, 5, E),
    circle(98, 50, 5, E),
  ].join('\n  ');
}

export function equipPullUpBar() {
  return [
    line(28, 36, 100, 36, E, 4),
    line(32, 36, 32, 100, E, 3),
    line(96, 36, 96, 100, E, 3),
  ].join('\n  ');
}

export function equipDipBars() {
  return [
    line(40, 44, 40, 100, E, 3.5),
    line(88, 44, 88, 100, E, 3.5),
    line(40, 52, 56, 52, E, 3.5),
    line(88, 52, 72, 52, E, 3.5),
  ].join('\n  ');
}

export function equipParallettes() {
  return [
    line(40, 70, 40, 96, E, 3),
    line(40, 70, 56, 70, E, 3),
    line(56, 70, 56, 96, E, 3),
    line(72, 70, 72, 96, E, 3),
    line(72, 70, 88, 70, E, 3),
    line(88, 70, 88, 96, E, 3),
  ].join('\n  ');
}

export function equipRings() {
  return [
    line(44, 28, 44, 48, E, 2.5),
    line(84, 28, 84, 48, E, 2.5),
    circle(44, 64, 14, 'none', E, 3),
    circle(84, 64, 14, 'none', E, 3),
  ].join('\n  ');
}

export function equipBench(flat = true) {
  if (flat) {
    return [
      rect(28, 60, 72, 10, E, { rx: 2 }),
      line(36, 70, 36, 96, E, 3),
      line(92, 70, 92, 96, E, 3),
      line(36, 96, 48, 96, E, 3),
      line(80, 96, 92, 96, E, 3),
    ].join('\n  ');
  }
  return [
    path('M30 78 L70 48 L98 48 L98 58 L74 58 L40 84 Z', E),
    line(40, 84, 40, 100, E, 3),
    line(90, 58, 90, 100, E, 3),
  ].join('\n  ');
}

export function equipBenchIncline() {
  return equipBench(false);
}

export function equipBenchDecline() {
  return [
    path('M30 48 L98 72 L98 82 L30 58 Z', E),
    line(36, 58, 36, 100, E, 3),
    line(90, 78, 90, 100, E, 3),
  ].join('\n  ');
}

export function equipSquatRack() {
  return [
    line(40, 28, 40, 108, E, 3.5),
    line(88, 28, 88, 108, E, 3.5),
    line(40, 40, 88, 40, E, 3),
    line(40, 70, 88, 70, E, 2.5),
    line(40, 28, 52, 28, E, 3),
    line(88, 28, 76, 28, E, 3),
  ].join('\n  ');
}

export function equipPowerRack() {
  return [
    rect(34, 28, 60, 80, 'none', { rx: 2, stroke: E, sw: 3 }),
    line(34, 48, 94, 48, E, 2.5),
    line(34, 72, 94, 72, E, 2.5),
  ].join('\n  ');
}

export function equipCable() {
  return [
    rect(56, 28, 16, 12, E, { rx: 2 }),
    line(64, 40, 64, 88, E, 2.5),
    circle(64, 92, 8, E),
    rect(40, 100, 48, 8, EL, { rx: 2 }),
  ].join('\n  ');
}

export function equipCableCrossover() {
  return [
    line(32, 28, 32, 100, E, 3),
    line(96, 28, 96, 100, E, 3),
    line(32, 28, 96, 28, E, 3),
    line(32, 48, 50, 70, E, 2.5),
    line(96, 48, 78, 70, E, 2.5),
    circle(50, 74, 5, E),
    circle(78, 74, 5, E),
  ].join('\n  ');
}

export function equipLatPulldown() {
  return [
    rect(40, 28, 48, 10, E, { rx: 2 }),
    line(48, 38, 48, 70, E, 2.5),
    line(80, 38, 80, 70, E, 2.5),
    line(40, 70, 88, 70, E, 3),
    rect(44, 88, 40, 16, EL, { rx: 3 }),
  ].join('\n  ');
}

export function equipLegPress() {
  return [
    path('M28 88 L48 40 L92 40 L100 88 Z', 'none', E, 3),
    rect(52, 48, 32, 24, EL, { rx: 3 }),
    rect(36, 92, 64, 10, E, { rx: 2 }),
  ].join('\n  ');
}

export function equipSmithMachine() {
  return [
    line(36, 28, 36, 108, E, 3.5),
    line(92, 28, 92, 108, E, 3.5),
    line(36, 56, 92, 56, E, 4),
    rect(28, 50, 10, 12, E, { rx: 1 }),
    rect(90, 50, 10, 12, E, { rx: 1 }),
  ].join('\n  ');
}

export function equipAssistedMachine() {
  return [
    rect(40, 28, 48, 8, E, { rx: 2 }),
    line(48, 36, 48, 70, E, 2.5),
    line(80, 36, 80, 70, E, 2.5),
    ellipse(64, 88, 22, 14, EL),
    rect(50, 70, 28, 8, E, { rx: 2 }),
  ].join('\n  ');
}

export function equipGenericMachine() {
  return [
    rect(36, 36, 56, 64, 'none', { rx: 6, stroke: E, sw: 3 }),
    rect(48, 48, 32, 20, EL, { rx: 3 }),
    circle(64, 84, 8, E),
  ].join('\n  ');
}

export function equipBox() {
  return [
    path('M40 48 L64 36 L88 48 L88 92 L40 92 Z', E),
    path('M40 48 L88 48 L88 56 L40 56 Z', EL),
  ].join('\n  ');
}

export function equipMat() {
  return [
    rect(28, 48, 72, 40, EL, { rx: 4 }),
    rect(32, 52, 64, 32, 'none', { rx: 3, stroke: E, sw: 2 }),
  ].join('\n  ');
}

export function equipSuspension() {
  return [
    line(48, 28, 48, 80, E, 2.5),
    line(80, 28, 80, 80, E, 2.5),
    line(40, 28, 88, 28, E, 3),
    rect(42, 80, 12, 16, E, { rx: 2 }),
    rect(74, 80, 12, 16, E, { rx: 2 }),
  ].join('\n  ');
}

export function equipLandmine() {
  return [
    circle(40, 96, 10, E),
    line(40, 90, 96, 40, E, 4),
    rect(88, 32, 14, 10, EL, { rx: 2 }),
  ].join('\n  ');
}

export function equipAbWheel() {
  return [
    circle(64, 72, 22, 'none', E, 3.5),
    circle(64, 72, 6, E),
    line(36, 72, 28, 56, E, 3),
    line(92, 72, 100, 56, E, 3),
  ].join('\n  ');
}

export function equipMedicineBall() {
  return [
    circle(64, 64, 26, E),
    path('M44 52 C56 44 72 44 84 52', 'none', COLORS.badge, 2),
  ].join('\n  ');
}

export function equipStabilityBall() {
  return [
    circle(64, 68, 30, 'none', E, 3),
    path('M40 68 C48 52 80 52 88 68', 'none', EL, 2),
  ].join('\n  ');
}

export function equipBosu() {
  return [
    path('M28 80 C28 48 100 48 100 80 Z', E),
    rect(28, 80, 72, 10, EL, { rx: 2 }),
  ].join('\n  ');
}

export function equipFoamRoller() {
  return [
    ellipse(64, 64, 36, 16, E),
    ellipse(64, 64, 28, 10, EL),
  ].join('\n  ');
}

export function equipTreadmill() {
  return [
    rect(28, 72, 72, 16, E, { rx: 3 }),
    rect(36, 40, 40, 32, EL, { rx: 3 }),
    line(76, 40, 92, 56, E, 3),
    line(92, 56, 92, 72, E, 3),
  ].join('\n  ');
}

export function equipBike() {
  return [
    circle(44, 84, 16, 'none', E, 3),
    circle(88, 84, 16, 'none', E, 3),
    line(44, 84, 64, 52, E, 3),
    line(88, 84, 64, 52, E, 3),
    line(64, 52, 64, 40, E, 3),
    rect(56, 36, 16, 6, E, { rx: 2 }),
    line(64, 52, 80, 60, E, 2.5),
  ].join('\n  ');
}

export function equipRower() {
  return [
    line(28, 88, 108, 72, E, 4),
    rect(40, 60, 28, 12, EL, { rx: 2 }),
    circle(36, 92, 8, E),
    line(70, 66, 90, 48, E, 3),
    rect(86, 40, 16, 10, E, { rx: 2 }),
  ].join('\n  ');
}

export function equipSkiErg() {
  return [
    line(64, 28, 64, 100, E, 4),
    rect(48, 28, 32, 10, E, { rx: 2 }),
    line(64, 50, 48, 70, E, 3),
    line(64, 50, 80, 70, E, 3),
    rect(40, 96, 48, 8, EL, { rx: 2 }),
  ].join('\n  ');
}

export function equipElliptical() {
  return [
    ellipse(48, 88, 14, 8, 'none', E, 3),
    ellipse(88, 88, 14, 8, 'none', E, 3),
    path('M48 80 C60 50 80 50 88 80', 'none', E, 3),
    line(64, 56, 64, 40, E, 3),
    rect(52, 32, 24, 8, E, { rx: 2 }),
  ].join('\n  ');
}

export function equipStairClimber() {
  return [
    path('M36 100 H56 V84 H68 V68 H80 V52 H96 V100 H36 Z', E),
  ].join('\n  ');
}

export function equipAssaultBike() {
  return [
    circle(64, 56, 22, 'none', E, 3),
    circle(40, 92, 12, 'none', E, 3),
    circle(88, 92, 12, 'none', E, 3),
    line(64, 78, 40, 92, E, 3),
    line(64, 78, 88, 92, E, 3),
    line(50, 48, 40, 36, E, 3),
    line(78, 48, 88, 36, E, 3),
  ].join('\n  ');
}

export function equipPool() {
  return [
    rect(28, 48, 72, 40, COLORS.accent, { rx: 6 }),
    path('M32 60 C44 54 52 66 64 60 C76 54 84 66 96 60', 'none', COLORS.badge, 2),
    path('M32 74 C44 68 52 80 64 74 C76 68 84 80 96 74', 'none', COLORS.badge, 2),
  ].join('\n  ');
}

export function equipKickboard() {
  return [
    path('M40 40 H88 C96 40 100 48 100 56 V80 C100 88 96 96 88 96 H40 C32 96 28 88 28 80 V56 C28 48 32 40 40 40 Z', E),
  ].join('\n  ');
}

export function equipPullBuoy() {
  return [
    ellipse(48, 64, 14, 22, E),
    ellipse(80, 64, 14, 22, E),
    rect(48, 56, 32, 16, EL, { rx: 4 }),
  ].join('\n  ');
}

export function equipPaddle() {
  return [
    path('M48 40 C40 48 40 64 48 72 L80 72 C88 64 88 48 80 40 Z', E),
    line(64, 72, 64, 100, E, 3),
  ].join('\n  ');
}

export function equipFins() {
  return [
    path('M36 48 L52 48 L60 96 L28 96 Z', E),
    path('M76 48 L92 48 L100 96 L68 96 Z', E),
  ].join('\n  ');
}

export function equipSnorkel() {
  return [
    circle(64, 64, 18, 'none', E, 3),
    path('M78 52 C90 40 90 28 82 24', 'none', E, 3),
    rect(78, 20, 10, 8, E, { rx: 2 }),
  ].join('\n  ');
}

export function equipTrack() {
  return [
    ellipse(64, 64, 40, 28, 'none', E, 3),
    ellipse(64, 64, 28, 18, 'none', EL, 2),
    line(64, 36, 64, 46, E, 2),
  ].join('\n  ');
}

export function equipRoad() {
  return [
    rect(48, 28, 32, 72, E, { rx: 2 }),
    line(64, 36, 64, 48, COLORS.badge, 2),
    line(64, 56, 64, 68, COLORS.badge, 2),
    line(64, 76, 64, 88, COLORS.badge, 2),
  ].join('\n  ');
}

export function equipTrail() {
  return [
    path('M40 100 C48 80 44 60 56 48 C68 36 72 28 88 28', 'none', E, 4),
    path('M52 100 C58 84 70 70 80 56', 'none', EL, 3),
  ].join('\n  ');
}

export function equipWall() {
  return [
    rect(40, 28, 48, 72, EL, { rx: 2 }),
    line(40, 52, 88, 52, E, 2),
    line(40, 76, 88, 76, E, 2),
    line(64, 28, 64, 100, E, 2),
  ].join('\n  ');
}

export function equipChair() {
  return [
    rect(40, 56, 48, 10, E, { rx: 2 }),
    line(44, 66, 44, 100, E, 3),
    line(84, 66, 84, 100, E, 3),
    line(40, 56, 40, 36, E, 3),
    line(40, 36, 88, 36, E, 3),
  ].join('\n  ');
}

export function equipDoorway() {
  return [
    path('M40 108 V36 C40 28 48 28 48 28 H80 C80 28 88 28 88 36 V108', 'none', E, 3.5),
  ].join('\n  ');
}

export function equipStairs() {
  return [
    path('M28 100 H48 V84 H64 V68 H80 V52 H100 V100 H28 Z', 'none', E, 3),
  ].join('\n  ');
}

export function equipParkBench() {
  return [
    rect(28, 64, 72, 8, E, { rx: 2 }),
    line(36, 72, 36, 96, E, 3),
    line(92, 72, 92, 96, E, 3),
    line(28, 56, 100, 56, E, 3),
    line(28, 56, 28, 64, E, 3),
    line(100, 56, 100, 64, E, 3),
  ].join('\n  ');
}

export function equipTree() {
  return [
    path('M64 48 C40 48 36 72 52 80 H76 C92 72 88 48 64 48 Z', E),
    rect(58, 80, 12, 24, EL, { rx: 2 }),
  ].join('\n  ');
}

export function equipRope() {
  return [
    path('M64 28 C60 40 68 52 64 64 C60 76 68 88 64 108', 'none', E, 3.5),
    circle(64, 28, 5, E),
  ].join('\n  ');
}

export function equipJumpRope() {
  return [
    path('M36 88 C20 60 20 40 40 32 C64 24 64 24 88 32 C108 40 108 60 92 88', 'none', E, 2.5),
    rect(32, 88, 10, 16, E, { rx: 2 }),
    rect(86, 88, 10, 16, E, { rx: 2 }),
  ].join('\n  ');
}

export function equipBattleRopes() {
  return [
    path('M28 40 C40 52 36 64 48 76 C56 84 52 96 64 104', 'none', E, 3.5),
    path('M100 40 C88 52 92 64 80 76 C72 84 76 96 64 104', 'none', E, 3.5),
  ].join('\n  ');
}

export function equipSled() {
  return [
    rect(32, 70, 64, 16, E, { rx: 3 }),
    line(40, 70, 40, 48, E, 3),
    line(88, 70, 88, 48, E, 3),
    line(40, 48, 88, 48, E, 3),
    rect(52, 52, 24, 14, EL, { rx: 2 }),
  ].join('\n  ');
}

export function equipFarmerHandles() {
  return [
    rect(36, 48, 10, 40, E, { rx: 2 }),
    rect(82, 48, 10, 40, E, { rx: 2 }),
    line(36, 56, 46, 56, EL, 3),
    line(82, 56, 92, 56, EL, 3),
  ].join('\n  ');
}

export function equipWeightVest() {
  return [
    path('M44 40 H84 V88 H72 V72 H56 V88 H44 Z', E),
    rect(52, 48, 8, 8, EL, { rx: 1 }),
    rect(68, 48, 8, 8, EL, { rx: 1 }),
    rect(52, 60, 8, 8, EL, { rx: 1 }),
    rect(68, 60, 8, 8, EL, { rx: 1 }),
  ].join('\n  ');
}

export function equipSandbag() {
  return [
    path('M32 56 C32 44 48 40 64 40 C80 40 96 44 96 56 L92 88 C92 96 80 100 64 100 C48 100 36 96 36 88 Z', E),
  ].join('\n  ');
}

export function equipClubbell() {
  return [
    circle(64, 40, 14, E),
    rect(60, 52, 8, 40, E, { rx: 2 }),
    ellipse(64, 96, 8, 6, EL),
  ].join('\n  ');
}

export function equipMacebell() {
  return [
    circle(64, 36, 16, E),
    rect(61, 50, 6, 48, E, { rx: 2 }),
  ].join('\n  ');
}

export function equipSlamBall() {
  return [
    circle(64, 68, 28, E),
    path('M48 56 C56 48 72 48 80 56', 'none', EL, 2),
  ].join('\n  ');
}

export function equipWallBall() {
  return equipMedicineBall();
}

export function equipAnkleWeights() {
  return [
    rect(36, 72, 20, 14, E, { rx: 4 }),
    rect(72, 72, 20, 14, E, { rx: 4 }),
  ].join('\n  ');
}

export function equipStep() {
  return [
    rect(32, 64, 64, 28, E, { rx: 3 }),
    rect(36, 56, 56, 10, EL, { rx: 2 }),
  ].join('\n  ');
}

export function equipDowel() {
  return line(28, 64, 100, 64, E, 4);
}

export function equipYogaBlock() {
  return rect(40, 48, 48, 36, E, { rx: 3 });
}

export function equipYogaStrap() {
  return [
    path('M40 40 C64 28 64 28 88 40 C100 56 100 72 88 88 C64 100 64 100 40 88 C28 72 28 56 40 40 Z', 'none', E, 3),
  ].join('\n  ');
}

export function equipBolster() {
  return ellipse(64, 68, 36, 18, E);
}

export function equipReformer() {
  return [
    rect(28, 56, 72, 28, 'none', { rx: 4, stroke: E, sw: 3 }),
    rect(36, 64, 32, 12, EL, { rx: 2 }),
    line(80, 56, 80, 84, E, 2),
  ].join('\n  ');
}

export function equipCadillac() {
  return [
    rect(32, 36, 64, 64, 'none', { rx: 4, stroke: E, sw: 3 }),
    line(32, 36, 32, 28, E, 3),
    line(96, 36, 96, 28, E, 3),
    line(32, 28, 96, 28, E, 3),
    rect(44, 72, 40, 16, EL, { rx: 2 }),
  ].join('\n  ');
}

export function equipPilatesChair() {
  return [
    rect(40, 60, 48, 12, E, { rx: 2 }),
    line(44, 72, 44, 100, E, 3),
    line(84, 72, 84, 100, E, 3),
    path('M40 60 C40 40 88 40 88 60', 'none', E, 3),
  ].join('\n  ');
}

export function equipPilatesBarrel() {
  return [
    path('M32 80 C32 48 96 48 96 80 Z', E),
    rect(32, 80, 64, 12, EL, { rx: 2 }),
  ].join('\n  ');
}

export function equipBalancePad() {
  return [
    rect(32, 56, 64, 28, EL, { rx: 8 }),
    rect(36, 60, 56, 20, 'none', { rx: 6, stroke: E, sw: 2 }),
  ].join('\n  ');
}

export function equipWedge() {
  return path('M32 88 L96 88 L96 48 Z', E);
}

export function equipSlantBoard() {
  return [
    path('M28 92 L100 92 L100 60 L28 80 Z', E),
  ].join('\n  ');
}

export function equipMassageBall() {
  return circle(64, 64, 18, E);
}

export function equipGripTrainer() {
  return [
    path('M44 40 C36 56 36 72 44 88', 'none', E, 4),
    path('M84 40 C92 56 92 72 84 88', 'none', E, 4),
    line(48, 48, 80, 48, E, 3),
    line(48, 80, 80, 80, E, 3),
  ].join('\n  ');
}

export function equipWristRoller() {
  return [
    line(32, 64, 96, 64, E, 3),
    circle(64, 64, 10, E),
    path('M64 74 C64 90 64 96 64 104', 'none', EL, 2),
  ].join('\n  ');
}

export function equipDipBelt() {
  return [
    path('M36 56 C36 40 92 40 92 56 L88 80 C88 92 40 92 40 80 Z', 'none', E, 3),
    circle(64, 88, 10, E),
  ].join('\n  ');
}

export function equipStraps() {
  return [
    path('M44 36 C44 60 44 80 44 100', 'none', E, 3),
    path('M84 36 C84 60 84 80 84 100', 'none', E, 3),
    path('M44 48 C64 56 64 56 84 48', 'none', E, 2.5),
  ].join('\n  ');
}

export function equipSleeves() {
  return [
    path('M40 48 C36 64 36 80 44 96 L56 92 C50 78 50 64 54 52 Z', E),
    path('M88 48 C92 64 92 80 84 96 L72 92 C78 78 78 64 74 52 Z', E),
  ].join('\n  ');
}

export function equipBelt() {
  return [
    path('M32 64 C40 52 88 52 96 64 C88 76 40 76 32 64 Z', E),
    rect(58, 58, 12, 12, EL, { rx: 2 }),
  ].join('\n  ');
}

export function equipChalk() {
  return rect(48, 48, 32, 32, EL, { rx: 4 });
}

export function equipPegboard() {
  return [
    rect(40, 28, 48, 72, 'none', { rx: 2, stroke: E, sw: 3 }),
    ...[0, 1, 2, 3, 4].flatMap((row) =>
      [0, 1, 2].map((col) => circle(52 + col * 12, 40 + row * 12, 2.5, E)),
    ),
  ].join('\n  ');
}

export function equipStallBars() {
  return [
    line(40, 28, 40, 108, E, 3),
    line(88, 28, 88, 108, E, 3),
    ...[0, 1, 2, 3, 4, 5].map((i) => line(40, 36 + i * 12, 88, 36 + i * 12, E, 2.5)),
  ].join('\n  ');
}

export function equipSafetyBar() {
  return [
    line(28, 56, 100, 56, E, 3),
    path('M40 56 C40 40 56 36 64 36 C72 36 88 40 88 56', 'none', E, 3),
    rect(24, 48, 10, 16, E, { rx: 1 }),
    rect(94, 48, 10, 16, E, { rx: 1 }),
  ].join('\n  ');
}

export function equipSwissBar() {
  return [
    path('M28 64 H44 V52 H56 V64 H72 V52 H84 V64 H100', 'none', E, 3.5),
    rect(24, 56, 8, 16, E, { rx: 1 }),
    rect(96, 56, 8, 16, E, { rx: 1 }),
  ].join('\n  ');
}

export function equipCamberedBar() {
  return [
    path('M28 56 C48 80 80 80 100 56', 'none', E, 3.5),
    rect(24, 48, 8, 16, E, { rx: 1 }),
    rect(96, 48, 8, 16, E, { rx: 1 }),
  ].join('\n  ');
}

export function equipHalfRack() {
  return [
    line(44, 28, 44, 108, E, 3.5),
    line(84, 28, 84, 108, E, 3.5),
    line(44, 28, 84, 28, E, 3),
    line(44, 60, 84, 60, E, 2.5),
    line(84, 28, 100, 40, E, 3),
  ].join('\n  ');
}

export function equipSquatStand() {
  return [
    line(40, 36, 40, 108, E, 3.5),
    line(88, 36, 88, 108, E, 3.5),
    line(40, 48, 56, 48, E, 3),
    line(88, 48, 72, 48, E, 3),
  ].join('\n  ');
}

export function equipJHooks() {
  return [
    line(48, 40, 48, 80, E, 3),
    path('M48 80 H64 V72', 'none', E, 3),
    line(80, 40, 80, 80, E, 3),
    path('M80 80 H64 V72', 'none', E, 3),
  ].join('\n  ');
}

export function equipSafetyArms() {
  return [
    line(36, 48, 36, 88, E, 3),
    line(36, 88, 60, 88, E, 3.5),
    line(92, 48, 92, 88, E, 3),
    line(92, 88, 68, 88, E, 3.5),
  ].join('\n  ');
}

export function equipPreacherBench() {
  return [
    path('M36 88 L56 48 L84 48 L84 58 L62 58 L48 88 Z', E),
    rect(44, 88, 40, 10, EL, { rx: 2 }),
    line(52, 98, 52, 108, E, 3),
    line(76, 98, 76, 108, E, 3),
  ].join('\n  ');
}

export function equipRomanChair() {
  return [
    rect(40, 56, 48, 10, E, { rx: 2 }),
    line(48, 66, 48, 100, E, 3),
    line(80, 66, 80, 100, E, 3),
    rect(36, 48, 16, 12, EL, { rx: 3 }),
    rect(76, 48, 16, 12, EL, { rx: 3 }),
  ].join('\n  ');
}

export function equipSeatedRow() {
  return [
    rect(32, 80, 48, 12, E, { rx: 2 }),
    line(40, 48, 40, 80, E, 3),
    line(40, 48, 72, 56, E, 3),
    circle(76, 58, 6, E),
    rect(72, 88, 20, 16, EL, { rx: 3 }),
  ].join('\n  ');
}

export function equipChestPress() {
  return [
    rect(40, 48, 48, 40, 'none', { rx: 4, stroke: E, sw: 3 }),
    circle(36, 60, 8, E),
    circle(92, 60, 8, E),
    rect(48, 88, 32, 12, EL, { rx: 2 }),
  ].join('\n  ');
}

export function equipShoulderPress() {
  return [
    rect(44, 56, 40, 36, 'none', { rx: 4, stroke: E, sw: 3 }),
    line(40, 48, 56, 56, E, 3),
    line(88, 48, 72, 56, E, 3),
    circle(36, 44, 6, E),
    circle(92, 44, 6, E),
  ].join('\n  ');
}

export function equipPecDeck() {
  return [
    rect(48, 40, 32, 48, 'none', { rx: 4, stroke: E, sw: 3 }),
    path('M48 56 C28 56 28 88 48 88', 'none', E, 3),
    path('M80 56 C100 56 100 88 80 88', 'none', E, 3),
  ].join('\n  ');
}

export function equipRearDelt() {
  return equipPecDeck();
}

export function equipHackSquat() {
  return [
    path('M40 36 L88 36 L96 100 L32 100 Z', 'none', E, 3),
    rect(48, 48, 32, 28, EL, { rx: 3 }),
  ].join('\n  ');
}

export function equipLegExtension() {
  return [
    rect(40, 48, 40, 16, E, { rx: 3 }),
    line(56, 64, 56, 88, E, 3),
    line(72, 64, 72, 88, E, 3),
    rect(48, 88, 32, 10, EL, { rx: 3 }),
  ].join('\n  ');
}

export function equipLegCurl() {
  return [
    ellipse(64, 56, 28, 14, E),
    line(48, 68, 48, 96, E, 3),
    line(80, 68, 80, 96, E, 3),
    rect(44, 96, 40, 8, EL, { rx: 2 }),
  ].join('\n  ');
}

export function equipHipAbduction() {
  return [
    rect(44, 40, 40, 48, 'none', { rx: 4, stroke: E, sw: 3 }),
    path('M44 72 C28 80 28 96 44 96', 'none', E, 3),
    path('M84 72 C100 80 100 96 84 96', 'none', E, 3),
  ].join('\n  ');
}

export function equipHipAdduction() {
  return equipHipAbduction();
}

export function equipCalfMachine() {
  return [
    rect(48, 36, 32, 12, E, { rx: 2 }),
    line(56, 48, 56, 88, E, 3),
    line(72, 48, 72, 88, E, 3),
    rect(44, 88, 40, 12, EL, { rx: 3 }),
  ].join('\n  ');
}

export function equipHipThrustMachine() {
  return [
    rect(28, 64, 72, 12, E, { rx: 3 }),
    rect(48, 40, 32, 24, EL, { rx: 4 }),
    line(40, 76, 40, 100, E, 3),
    line(88, 76, 88, 100, E, 3),
  ].join('\n  ');
}

export function equipCrunchMachine() {
  return [
    path('M40 88 C40 48 88 48 88 88', 'none', E, 3),
    rect(48, 88, 32, 12, EL, { rx: 3 }),
    line(64, 48, 64, 36, E, 3),
  ].join('\n  ');
}

export function equipRotaryTorso() {
  return [
    circle(64, 64, 28, 'none', E, 3),
    rect(56, 48, 16, 32, EL, { rx: 3 }),
    line(64, 36, 64, 48, E, 3),
  ].join('\n  ');
}

export function equipSliders() {
  return [
    ellipse(44, 72, 16, 10, E),
    ellipse(84, 72, 16, 10, E),
  ].join('\n  ');
}

export function equipOpenWater() {
  return [
    path('M28 64 C40 52 52 76 64 64 C76 52 88 76 100 64', 'none', COLORS.accent, 3),
    path('M28 80 C40 68 52 92 64 80 C76 68 88 92 100 80', 'none', E, 3),
  ].join('\n  ');
}

export function equipSwimChute() {
  return [
    path('M48 40 L80 40 L96 88 L32 88 Z', 'none', E, 3),
    line(64, 40, 64, 28, E, 2.5),
  ].join('\n  ');
}

export function equipFigure() {
  // bodyweight / none — small figure only (caller adds tinyFigure separately)
  return '';
}

const SHAPES = {
  figure: equipFigure,
  dumbbell: equipDumbbell,
  kettlebell: equipKettlebell,
  barbell: equipBarbell,
  ez_bar: equipEzBar,
  trap_bar: equipTrapBar,
  plates: equipPlates,
  band: equipBand,
  mini_band: equipMiniBand,
  tube_band: equipTubeBand,
  pull_up_bar: equipPullUpBar,
  dip_bars: equipDipBars,
  parallettes: equipParallettes,
  rings: equipRings,
  bench: () => equipBench(true),
  bench_incline: equipBenchIncline,
  bench_decline: equipBenchDecline,
  squat_rack: equipSquatRack,
  power_rack: equipPowerRack,
  half_rack: equipHalfRack,
  squat_stand: equipSquatStand,
  j_hooks: equipJHooks,
  safety_arms: equipSafetyArms,
  cable: equipCable,
  cable_crossover: equipCableCrossover,
  lat_pulldown: equipLatPulldown,
  seated_row: equipSeatedRow,
  chest_press: equipChestPress,
  shoulder_press: equipShoulderPress,
  pec_deck: equipPecDeck,
  rear_delt: equipRearDelt,
  leg_press: equipLegPress,
  hack_squat: equipHackSquat,
  leg_extension: equipLegExtension,
  leg_curl: equipLegCurl,
  hip_abduction: equipHipAbduction,
  hip_adduction: equipHipAdduction,
  calf_machine: equipCalfMachine,
  smith_machine: equipSmithMachine,
  assisted_machine: equipAssistedMachine,
  generic_machine: equipGenericMachine,
  hip_thrust_machine: equipHipThrustMachine,
  crunch_machine: equipCrunchMachine,
  rotary_torso: equipRotaryTorso,
  box: equipBox,
  step: equipStep,
  mat: equipMat,
  suspension: equipSuspension,
  landmine: equipLandmine,
  ab_wheel: equipAbWheel,
  medicine_ball: equipMedicineBall,
  slam_ball: equipSlamBall,
  wall_ball: equipWallBall,
  stability_ball: equipStabilityBall,
  bosu: equipBosu,
  foam_roller: equipFoamRoller,
  massage_ball: equipMassageBall,
  treadmill: equipTreadmill,
  bike: equipBike,
  rower: equipRower,
  ski_erg: equipSkiErg,
  elliptical: equipElliptical,
  stair_climber: equipStairClimber,
  assault_bike: equipAssaultBike,
  pool: equipPool,
  kickboard: equipKickboard,
  pull_buoy: equipPullBuoy,
  paddle: equipPaddle,
  fins: equipFins,
  snorkel: equipSnorkel,
  swim_chute: equipSwimChute,
  open_water: equipOpenWater,
  track: equipTrack,
  road: equipRoad,
  trail: equipTrail,
  wall: equipWall,
  chair: equipChair,
  doorway: equipDoorway,
  stairs: equipStairs,
  park_bench: equipParkBench,
  tree: equipTree,
  rope: equipRope,
  jump_rope: equipJumpRope,
  battle_ropes: equipBattleRopes,
  sled: equipSled,
  farmer_handles: equipFarmerHandles,
  weight_vest: equipWeightVest,
  sandbag: equipSandbag,
  clubbell: equipClubbell,
  macebell: equipMacebell,
  ankle_weights: equipAnkleWeights,
  dowel: equipDowel,
  yoga_block: equipYogaBlock,
  yoga_strap: equipYogaStrap,
  bolster: equipBolster,
  reformer: equipReformer,
  cadillac: equipCadillac,
  pilates_chair: equipPilatesChair,
  pilates_barrel: equipPilatesBarrel,
  balance_pad: equipBalancePad,
  wedge: equipWedge,
  slant_board: equipSlantBoard,
  grip_trainer: equipGripTrainer,
  wrist_roller: equipWristRoller,
  dip_belt: equipDipBelt,
  straps: equipStraps,
  sleeves: equipSleeves,
  belt: equipBelt,
  chalk: equipChalk,
  pegboard: equipPegboard,
  stall_bars: equipStallBars,
  safety_bar: equipSafetyBar,
  swiss_bar: equipSwissBar,
  cambered_bar: equipCamberedBar,
  preacher_bench: equipPreacherBench,
  roman_chair: equipRomanChair,
  sliders: equipSliders,
};

export function equipmentShape(shapeId) {
  const fn = SHAPES[shapeId] || equipGenericMachine;
  return fn();
}

export function equipmentForContentTag(tag) {
  const map = {
    bodyweight: 'figure',
    none: 'figure',
    dumbbell: 'dumbbell',
    kettlebell: 'kettlebell',
    barbell: 'barbell',
    ez_bar: 'ez_bar',
    trap_bar: 'trap_bar',
    plates: 'plates',
    bands: 'band',
    pull_up_bar: 'pull_up_bar',
    dip_bars: 'dip_bars',
    bench: 'bench',
    bench_flat: 'bench',
    bench_adjustable: 'bench_incline',
    squat_rack: 'squat_rack',
    cable: 'cable',
    smith_machine: 'smith_machine',
    leg_press: 'leg_press',
    lat_pulldown: 'lat_pulldown',
    assisted_pullup_dip_machine: 'assisted_machine',
    machine: 'generic_machine',
    suspension: 'suspension',
    box: 'box',
    landmine: 'landmine',
    yoga_mat: 'mat',
    pilates_mat: 'mat',
    pool: 'pool',
    kickboard: 'kickboard',
    pull_buoy: 'pull_buoy',
    paddle: 'paddle',
    fins: 'fins',
    road: 'road',
    track: 'track',
    treadmill: 'treadmill',
  };
  return equipmentShape(map[tag] || 'generic_machine');
}

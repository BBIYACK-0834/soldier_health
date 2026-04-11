export const GOAL_TYPE_LABELS = {
  BULK: '벌크업',
  CUT: '감량',
  MAINTAIN: '유지',
  FITNESS_TEST: '체력시험 준비',
  GENERAL_FITNESS: '일반 체력 향상',
};

export const WORKOUT_LEVEL_LABELS = {
  BEGINNER: '초급',
  NOVICE: '초중급',
  INTERMEDIATE: '중급',
};

export const BRANCH_TYPE_LABELS = {
  ARMY: '육군',
  NAVY: '해군',
  AIR_FORCE: '공군',
  MARINES: '해병대',
  ETC: '기타',
};

export const WEEKDAY_LABELS = {
  MON: '월',
  TUE: '화',
  WED: '수',
  THU: '목',
  FRI: '금',
  SAT: '토',
  SUN: '일',
};

export function toLabel(map, value, fallback = '-') {
  if (!value) return fallback;
  return map[value] || value;
}

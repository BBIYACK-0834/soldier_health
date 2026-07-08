import { exerciseCatalog } from '../constants/defaultData';
import { getWorkoutRoutineIndex } from './workoutStorage';

const strengthSplits = {
  1: ['전신'],
  2: ['상체', '하체'],
  3: ['Push · 가슴/어깨/삼두', 'Pull · 등/이두', 'Legs · 하체'],
  4: ['상체 Push', '하체 Quad', '상체 Pull', '하체 Posterior'],
  5: ['가슴', '등', '어깨', '팔', '하체'],
  6: ['Push', 'Pull', 'Legs', 'Push 보강', 'Pull 보강', 'Legs 보강'],
};

const splitKeywords = {
  전신: ['가슴', '등', '어깨', '하체', '코어', '전신'],
  상체: ['가슴', '등', '어깨', '이두', '삼두', '상완', '전완'],
  하체: ['하체', '둔근', '햄스트링', '종아리'],
  'Push · 가슴/어깨/삼두': ['가슴', '어깨', '삼두'],
  'Pull · 등/이두': ['등', '이두', '후면어깨'],
  'Legs · 하체': ['하체', '둔근', '햄스트링', '종아리'],
  '상체 Push': ['가슴', '어깨', '삼두'],
  '하체 Quad': ['하체', '스쿼트', '대퇴', '종아리'],
  '상체 Pull': ['등', '이두', '후면어깨'],
  '하체 Posterior': ['둔근', '햄스트링', '하체', '종아리'],
  가슴: ['가슴', '삼두'],
  등: ['등', '이두'],
  어깨: ['어깨'],
  팔: ['삼두', '이두', '상완', '전완'],
  Push: ['가슴', '어깨', '삼두'],
  Pull: ['등', '이두'],
  Legs: ['하체', '둔근', '햄스트링', '종아리'],
  'Push 보강': ['가슴', '어깨', '삼두'],
  'Pull 보강': ['등', '이두', '코어'],
  'Legs 보강': ['하체', '둔근', '햄스트링', '종아리'],
};

function normalizeDays(days) {
  return Math.max(1, Math.min(Number(days) || 3, 6));
}

function withExerciseName(exercise) {
  return { ...exercise, exerciseName: exercise.exerciseName || exercise.name };
}

function matchesEquipment(exercise, selectedEquipmentNames = []) {
  if (!selectedEquipmentNames.length) return true;
  if (['warmup-mobility', 'cooldown-breath'].includes(exercise.id)) return true;

  const haystack = `${exercise.exerciseName} ${exercise.category}`;
  return selectedEquipmentNames.some((name) => haystack.includes(name));
}

function pickExercises(keywords, limit = 7, selectedEquipmentNames = []) {
  const warmup = exerciseCatalog.find((exercise) => exercise.id === 'warmup-mobility');
  const cooldown = exerciseCatalog.find((exercise) => exercise.id === 'cooldown-breath');
  const matched = exerciseCatalog
    .filter((exercise) => keywords.some((keyword) => exercise.category.includes(keyword) || exercise.exerciseName.includes(keyword)))
    .filter((exercise) => !['warmup-mobility', 'cooldown-breath'].includes(exercise.id))
    .filter((exercise) => matchesEquipment(exercise, selectedEquipmentNames))
    .slice(0, limit);
  return [warmup, ...matched, cooldown].filter(Boolean).map(withExerciseName);
}

function addCutFinisher(exercises, profile, index) {
  if (profile?.goalType !== 'CUT') return exercises;

  const nextExercises = [...exercises];
  nextExercises.splice(nextExercises.length - 1, 0, {
    id: `cardio-finisher-${index}`,
    exerciseName: '감량 유산소 피니셔',
    category: '유산소 · 다이어트',
    durationSeconds: profile?.workoutLevel === 'BEGINNER' ? 900 : 1200,
    restSeconds: 0,
    sets: 1,
    reps: profile?.workoutLevel === 'BEGINNER' ? '15분 빠른 걷기' : '20분 인터벌',
    caloriesBurned: 0,
    intensity: profile?.workoutLevel === 'BEGINNER' ? 'Medium' : 'High',
  });
  return nextExercises;
}

function buildFitnessTestWorkout(level) {
  const hard = level === 'INTERMEDIATE';
  return {
    routineKey: 'FITNESS_TEST',
    todayFocus: '윗몸 86개 · 푸시업 72개 · 3km 12분 30초',
    routineType: '특급전사 3종 집중 루틴',
    exercises: [
      { id: 'fitness-run-3km', exerciseName: '3km 목표 페이스 뜀걸음', category: '뜀걸음 · 특급전사', durationSeconds: hard ? 750 : 900, restSeconds: 60, sets: 1, reps: '12분 30초 목표 페이스', caloriesBurned: 0, intensity: 'High' },
      { id: 'fitness-push-up', exerciseName: '푸시업', category: '푸시업 · 특급전사', durationSeconds: 60, restSeconds: 60, sets: hard ? 5 : 4, reps: '목표 72개까지 최대반복', caloriesBurned: 0, intensity: 'High' },
      { id: 'fitness-sit-up', exerciseName: '윗몸일으키기', category: '윗몸 · 특급전사', durationSeconds: 60, restSeconds: 60, sets: hard ? 5 : 4, reps: '목표 86개까지 최대반복', caloriesBurned: 0, intensity: 'High' },
    ],
  };
}

export function buildWorkoutPlanFromProfile(profile, selectedEquipmentNames = []) {
  if (profile?.goalType === 'FITNESS_TEST') {
    const workout = buildFitnessTestWorkout(profile?.workoutLevel);
    return {
      planKey: 'FITNESS_TEST',
      routineType: workout.routineType,
      routines: [{ ...workout, routineIndex: 0 }],
    };
  }

  const days = normalizeDays(profile?.workoutDaysPerWeek);
  const split = strengthSplits[days] ?? strengthSplits[5];
  const routineType = `주 ${days}회 ${days}분할 순환 루틴${profile?.goalType === 'CUT' ? ' + 유산소' : ''}`;
  const routines = split.map((focus, index) => ({
    routineKey: `${profile?.goalType || 'BULK'}-${days}-${index}-${focus}`,
    routineIndex: index,
    todayFocus: focus,
    routineType,
    exercises: addCutFinisher(pickExercises(splitKeywords[focus] ?? splitKeywords.전신, 7, selectedEquipmentNames), profile, index),
  }));

  return {
    planKey: `${profile?.goalType || 'BULK'}-${days}-${profile?.workoutLevel || 'BEGINNER'}`,
    routineType,
    routines,
  };
}

export function buildWorkoutFromProfile(profile) {
  const plan = buildWorkoutPlanFromProfile(profile);
  const index = getWorkoutRoutineIndex() % plan.routines.length;
  return plan.routines[index];
}

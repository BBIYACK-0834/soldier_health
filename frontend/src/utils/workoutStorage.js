const WORKOUT_LOG_KEY = 'tg_workout_log_v1';
const WEEKLY_WORKOUT_TARGET = 4;
const WORKOUT_ROUTINE_INDEX_KEY = 'tg_workout_routine_index_v1';
const CUSTOM_WORKOUT_PLAN_KEY = 'tg_custom_workout_plan_v1';

export function formatWorkoutDate(date = new Date()) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function readCustomWorkoutPlan() {
  try {
    const raw = localStorage.getItem(CUSTOM_WORKOUT_PLAN_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function saveCustomWorkoutPlan(plan) {
  if (!plan?.routines?.length) return null;

  const nextPlan = {
    planKey: plan.planKey,
    routineType: plan.routineType,
    routines: plan.routines,
    updatedAt: new Date().toISOString(),
  };

  localStorage.setItem(CUSTOM_WORKOUT_PLAN_KEY, JSON.stringify(nextPlan));
  window.dispatchEvent(new Event('tg-workout-plan-updated'));
  return nextPlan;
}

export function applyCustomWorkoutPlan(plan) {
  if (!plan?.routines?.length) return plan;

  const customPlan = readCustomWorkoutPlan();
  if (!customPlan?.routines?.length || customPlan.planKey !== plan.planKey) return plan;

  const customRoutineByKey = new Map(customPlan.routines.map((routine) => [routine.routineKey, routine]));

  return {
    ...plan,
    routines: plan.routines.map((routine) => {
      const customRoutine = customRoutineByKey.get(routine.routineKey);
      if (!customRoutine?.exercises?.length) return routine;
      return {
        ...routine,
        exercises: customRoutine.exercises,
      };
    }),
  };
}

function readLog() {
  try {
    const raw = localStorage.getItem(WORKOUT_LOG_KEY);
    return raw ? JSON.parse(raw) : {};
  } catch {
    return {};
  }
}

function writeLog(log) {
  localStorage.setItem(WORKOUT_LOG_KEY, JSON.stringify(log));
  window.dispatchEvent(new Event('tg-workout-progress-updated'));
}

export function readWorkoutProgress(dateKey = formatWorkoutDate()) {
  const log = readLog();
  return log[dateKey] ?? null;
}

export function saveWorkoutProgress(progress, dateKey = formatWorkoutDate()) {
  const log = readLog();
  log[dateKey] = {
    ...(log[dateKey] ?? {}),
    ...progress,
    updatedAt: new Date().toISOString(),
  };
  writeLog(log);
  return log[dateKey];
}

export function saveWorkoutPlan(workout, dateKey = formatWorkoutDate()) {
  if (!workout?.exercises?.length) return readWorkoutProgress(dateKey);

  const current = readWorkoutProgress(dateKey) ?? {};
  const validExerciseIds = new Set(workout.exercises.map((exercise) => exercise.id));
  const completedSets = Object.fromEntries(
    Object.entries(current.completedSets ?? {}).filter(([exerciseId]) => validExerciseIds.has(exerciseId))
  );

  const sameRoutine = !current.routineKey || current.routineKey === workout.routineKey;

  return saveWorkoutProgress({
    ...current,
    routineKey: workout.routineKey,
    routineType: workout.routineType,
    todayFocus: workout.todayFocus,
    exercises: workout.exercises,
    completedSets: sameRoutine ? completedSets : {},
    workoutCompleted: sameRoutine ? Boolean(current.workoutCompleted) : false,
    completedAt: sameRoutine ? current.completedAt ?? null : null,
  }, dateKey);
}

export function saveCompletedSets(completedSets, dateKey = formatWorkoutDate()) {
  return saveWorkoutProgress({ completedSets }, dateKey);
}

export function getWorkoutRoutineIndex() {
  return Number(localStorage.getItem(WORKOUT_ROUTINE_INDEX_KEY) || 0) || 0;
}

export function advanceWorkoutRoutineIndex() {
  const next = getWorkoutRoutineIndex() + 1;
  localStorage.setItem(WORKOUT_ROUTINE_INDEX_KEY, String(next));
  return next;
}

export function markWorkoutComplete(workout, dateKey = formatWorkoutDate()) {
  const current = readWorkoutProgress(dateKey);
  if (!current?.workoutCompleted) {
    advanceWorkoutRoutineIndex();
  }
  return saveWorkoutProgress({
    routineKey: workout?.routineKey,
    routineType: workout?.routineType,
    todayFocus: workout?.todayFocus,
    exercises: workout?.exercises,
    completedSets: Object.fromEntries((workout?.exercises ?? []).map((exercise) => [exercise.id, exercise.sets])),
    workoutCompleted: true,
    completedAt: new Date().toISOString(),
  }, dateKey);
}

export function resetWorkoutProgress(dateKey = formatWorkoutDate()) {
  const current = readWorkoutProgress(dateKey) ?? {};
  return saveWorkoutProgress({
    ...current,
    completedSets: {},
    workoutCompleted: false,
    completedAt: null,
  }, dateKey);
}

export function getWeeklyWorkoutSummary(referenceDate = new Date()) {
  const log = readLog();
  const start = new Date(referenceDate);
  start.setHours(0, 0, 0, 0);
  start.setDate(start.getDate() - start.getDay());

  const completedDates = Array.from({ length: 7 }, (_, index) => {
    const date = new Date(start);
    date.setDate(start.getDate() + index);
    const dateKey = formatWorkoutDate(date);
    return log[dateKey]?.workoutCompleted ? dateKey : null;
  }).filter(Boolean);

  return {
    completed: Math.min(completedDates.length, WEEKLY_WORKOUT_TARGET),
    target: WEEKLY_WORKOUT_TARGET,
    completedDates,
  };
}

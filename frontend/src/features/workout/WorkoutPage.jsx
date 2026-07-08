import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import TabSwitcher from '../../components/ui/TabSwitcher';
import { getMyProfile } from '../../api/userApi';
import { getMyEquipments } from '../../api/equipmentApi';
import { emptyWorkout, exerciseCatalog } from '../../constants/defaultData';
import { buildWorkoutPlanFromProfile } from '../../utils/workoutPlanner';
import { applyCustomWorkoutPlan, getWorkoutRoutineIndex, readWorkoutProgress, saveCustomWorkoutPlan, saveWorkoutPlan } from '../../utils/workoutStorage';
import styles from './WorkoutPage.module.css';
import screen from '../../components/ui/Screen.module.css';

function toExercise(raw, index) {
  const sets = Number(raw?.sets) > 0 ? Number(raw.sets) : 1;
  const durationSeconds = Number(raw?.durationSeconds) > 0
    ? Number(raw.durationSeconds)
    : Math.max(20, Number(raw?.durationMinutes ?? raw?.minutes ?? 0) * 60 || 40);
  return {
    id: raw?.id ?? `${raw?.exerciseName ?? raw?.name ?? 'exercise'}-${index}`,
    exerciseName: raw?.exerciseName ?? raw?.name ?? '운동',
    category: raw?.category ?? '추천',
    durationSeconds,
    restSeconds: Number(raw?.restSeconds) >= 0 ? Number(raw.restSeconds) : 20,
    sets,
    reps: raw?.reps ?? '시간 수행',
    caloriesBurned: Number(raw?.caloriesBurned ?? raw?.calories ?? 0) || 0,
    intensity: raw?.intensity ?? 'Medium',
  };
}

function buildDefaultWorkoutPlan(data) {
  const apiExercises = data?.exercises?.length ? data.exercises.map(toExercise) : [];
  const exercises = apiExercises.length > 0 ? apiExercises : exerciseCatalog.map(toExercise);
  const workout = {
    routineKey: data?.routineKey ?? 'free-choice-0',
    routineIndex: 0,
    todayFocus: data?.todayFocus ?? '오늘 운동 선택',
    routineType: data?.routineType ?? '자유 선택 루틴',
    exercises,
  };

  return {
    planKey: 'free-choice',
    routineType: workout.routineType,
    routines: [workout],
  };
}

function getTodayRoutine(plan) {
  const routines = plan?.routines ?? [];
  if (routines.length === 0) return emptyWorkout;
  return routines[getWorkoutRoutineIndex() % routines.length];
}

function makeAddedExercise(catalogExercise, routineKey) {
  return {
    ...catalogExercise,
    id: `${catalogExercise.id}-${routineKey}-${Date.now()}`,
    exerciseName: catalogExercise.exerciseName || catalogExercise.name,
  };
}

function insertBeforeCooldown(exercises, exercise) {
  const cooldownIndex = exercises.findIndex((item) => item.id === 'cooldown-breath');
  if (cooldownIndex < 0) return [...exercises, exercise];
  return [...exercises.slice(0, cooldownIndex), exercise, ...exercises.slice(cooldownIndex)];
}

function ExerciseAddControl({ value, customValue, onChange, onCustomChange, onAdd, onAddCustom, disabled }) {
  return (
    <div className={styles.addPanel}>
      <div className={styles.addControl}>
        <select value={value} onChange={(event) => onChange(event.target.value)} aria-label="추가할 운동 선택">
          {exerciseCatalog.map((exercise) => <option key={exercise.id} value={exercise.id}>{exercise.exerciseName}</option>)}
        </select>
        <button type="button" className={styles.iconButton} onClick={onAdd} disabled={disabled} aria-label="운동 추가">＋</button>
      </div>
      <div className={styles.addControl}>
        <input value={customValue} onChange={(event) => onCustomChange(event.target.value)} placeholder="직접 운동명 입력" aria-label="직접 추가할 운동명" />
        <button type="button" className={styles.iconButton} onClick={onAddCustom} disabled={disabled || !customValue.trim()} aria-label="직접 입력 운동 추가">＋</button>
      </div>
    </div>
  );
}

function ExerciseList({ exercises, completedSets = {}, onRemove, showProgress = false }) {
  return (
    <ul className={styles.list}>
      {exercises.map((routine) => {
        const doneSets = completedSets[routine.id] ?? 0;
        const complete = showProgress && doneSets >= routine.sets;
        return (
          <li key={routine.id} className={complete ? styles.completed : ''}>
            <div className={styles.listItem}>
              <span>
                <strong>{routine.exerciseName}</strong>
                <small>{routine.category} · {routine.sets}세트 · {routine.reps}</small>
              </span>
              <div className={styles.itemActions}>
                {showProgress ? <em>{Math.min(doneSets, routine.sets)}/{routine.sets}</em> : null}
                <button type="button" className={styles.removeButton} onClick={() => onRemove(routine.id)} aria-label={`${routine.exerciseName} 제거`}>－</button>
              </div>
            </div>
          </li>
        );
      })}
    </ul>
  );
}

export default function WorkoutPage() {
  const navigate = useNavigate();
  const [tab, setTab] = useState('today');
  const [workoutPlan, setWorkoutPlan] = useState({ planKey: '', routineType: '', routines: [] });
  const [workout, setWorkout] = useState(emptyWorkout);
  const [completedSets, setCompletedSets] = useState({});
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [selectedExerciseByRoutine, setSelectedExerciseByRoutine] = useState({});
  const [customExerciseByRoutine, setCustomExerciseByRoutine] = useState({});
  const [openRoutineKeys, setOpenRoutineKeys] = useState({});
  const [equipmentNames, setEquipmentNames] = useState([]);

  useEffect(() => {
    let mounted = true;
    async function load() {
      try {
        setLoading(true);
        const [profile, myEquipments] = await Promise.all([getMyProfile(), getMyEquipments()]);
        const nextEquipmentNames = (myEquipments ?? []).map((item) => item.name).filter(Boolean);
        const nextPlan = applyCustomWorkoutPlan(buildWorkoutPlanFromProfile(profile, nextEquipmentNames));
        const nextWorkout = getTodayRoutine(nextPlan);
        if (!mounted) return;
        const saved = saveWorkoutPlan(nextWorkout);
        setEquipmentNames(nextEquipmentNames);
        setWorkoutPlan(nextPlan);
        setWorkout(nextWorkout);
        setCompletedSets(saved?.completedSets ?? {});
      } catch (error) {
        if (!mounted) return;
        const saved = readWorkoutProgress();
        const nextPlan = saved?.exercises?.length
          ? buildDefaultWorkoutPlan({ routineKey: saved.routineKey, todayFocus: saved.todayFocus, routineType: saved.routineType, exercises: saved.exercises })
          : buildDefaultWorkoutPlan();
        const customizedPlan = applyCustomWorkoutPlan(nextPlan);
        const nextWorkout = saved?.exercises?.length
          ? { routineKey: saved.routineKey, todayFocus: saved.todayFocus, routineType: saved.routineType, exercises: saved.exercises }
          : getTodayRoutine(customizedPlan);
        setWorkoutPlan(customizedPlan);
        setWorkout(nextWorkout);
        setCompletedSets(saved?.completedSets ?? {});
        setErrorMessage('프로필 기반 루틴을 만들지 못해 저장된 운동 또는 기본 운동 목록을 표시합니다.');
      } finally {
        if (mounted) setLoading(false);
      }
    }

    load();
    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    const handleProgressUpdate = () => {
      const saved = readWorkoutProgress();
      setCompletedSets(saved?.completedSets ?? {});
      if (saved?.exercises?.length) {
        setWorkout({ routineKey: saved.routineKey, todayFocus: saved.todayFocus, routineType: saved.routineType, exercises: saved.exercises });
      }
    };

    window.addEventListener('tg-workout-progress-updated', handleProgressUpdate);
    return () => window.removeEventListener('tg-workout-progress-updated', handleProgressUpdate);
  }, []);

  const exercises = workout?.exercises ?? [];
  const routines = workoutPlan?.routines ?? [];
  const selectedTodayExercise = selectedExerciseByRoutine[workout?.routineKey] ?? exerciseCatalog[0]?.id;
  const customTodayExercise = customExerciseByRoutine[workout?.routineKey] ?? '';
  const workoutPlanSummary = useMemo(() => `${routines.length}개 루틴 · 총 ${routines.reduce((sum, routine) => sum + (routine.exercises?.length ?? 0), 0)}개 운동`, [routines]);

  const persistPlan = (nextPlan) => {
    const savedPlan = saveCustomWorkoutPlan(nextPlan) ?? nextPlan;
    const nextWorkout = savedPlan.routines.find((routine) => routine.routineKey === workout?.routineKey) ?? getTodayRoutine(savedPlan);
    const saved = saveWorkoutPlan(nextWorkout);
    setWorkoutPlan(savedPlan);
    setWorkout(nextWorkout);
    setCompletedSets(saved?.completedSets ?? {});
  };

  const updateRoutineExercises = (routineKey, updater) => {
    if (!routineKey) return;
    const nextPlan = {
      ...workoutPlan,
      routines: routines.map((routine) => (
        routine.routineKey === routineKey
          ? { ...routine, exercises: updater(routine.exercises ?? []) }
          : routine
      )),
    };
    persistPlan(nextPlan);
  };

  const makeCustomExercise = (name, routineKey) => ({
    id: `custom-${routineKey}-${Date.now()}`,
    exerciseName: name.trim(),
    category: '직접 추가',
    durationSeconds: 40,
    restSeconds: 30,
    sets: 3,
    reps: '직접 설정',
    caloriesBurned: 0,
    intensity: 'Medium',
  });

  const addExercise = (routineKey) => {
    const selectedId = selectedExerciseByRoutine[routineKey] ?? exerciseCatalog[0]?.id;
    const catalogExercise = exerciseCatalog.find((exercise) => exercise.id === selectedId);
    if (!catalogExercise) return;
    updateRoutineExercises(routineKey, (currentExercises) => insertBeforeCooldown(currentExercises, makeAddedExercise(catalogExercise, routineKey)));
  };

  const addCustomExercise = (routineKey) => {
    const customName = customExerciseByRoutine[routineKey] ?? '';
    if (!customName.trim()) return;
    updateRoutineExercises(routineKey, (currentExercises) => insertBeforeCooldown(currentExercises, makeCustomExercise(customName, routineKey)));
    setCustomExerciseByRoutine((prev) => ({ ...prev, [routineKey]: '' }));
  };

  const removeExercise = (routineKey, exerciseId) => {
    updateRoutineExercises(routineKey, (currentExercises) => currentExercises.filter((exercise) => exercise.id !== exerciseId));
  };

  const startWorkout = () => {
    saveWorkoutPlan(workout);
    navigate('/exercise/session');
  };

  const toggleRoutine = (routineKey) => {
    setOpenRoutineKeys((prev) => ({ ...prev, [routineKey]: !prev[routineKey] }));
  };

  return (
    <AppLayout title="운동" subtitle="기구 선택을 기준으로 오늘 루틴을 추천하고 직접 수정하세요." headerAction={<span className={styles.calendar}>🗓️</span>}>
      <TabSwitcher
        tabs={[{ value: 'today', label: '오늘 루틴' }, { value: 'all', label: '전체 루틴' }]}
        value={tab}
        onChange={setTab}
      />

      {tab === 'today' ? (
        <Card>
          <div className={styles.row}>
            <div>
              <h3 className={styles.title}>오늘 운동 목록</h3>
              <span className={styles.smallMeta}>{workout?.todayFocus || '운동 데이터 없음'} · {workout?.routineType || '루틴 미선택'}</span>
            </div>
            <button type="button" onClick={startWorkout} disabled={exercises.length === 0}>운동 시작</button>
          </div>
          {equipmentNames.length > 0 ? <p className={styles.equipmentMeta}>선택 기구: {equipmentNames.join(' · ')}</p> : <p className={styles.equipmentMeta}>기구 선택이 필요합니다. 선택한 기구에 맞춰 운동을 추천합니다.</p>}
          {loading ? <p className={screen.muted}>불러오는 중...</p> : null}
          {!loading && exercises.length > 0 ? (
            <>
              <ExerciseAddControl
                value={selectedTodayExercise}
                onChange={(value) => setSelectedExerciseByRoutine((prev) => ({ ...prev, [workout.routineKey]: value }))}
                customValue={customTodayExercise}
                onCustomChange={(value) => setCustomExerciseByRoutine((prev) => ({ ...prev, [workout.routineKey]: value }))}
                onAdd={() => addExercise(workout.routineKey)}
                onAddCustom={() => addCustomExercise(workout.routineKey)}
                disabled={!workout?.routineKey}
              />
              <ExerciseList exercises={exercises} completedSets={completedSets} onRemove={(exerciseId) => removeExercise(workout.routineKey, exerciseId)} showProgress />
            </>
          ) : null}
          {!loading && exercises.length === 0 ? <p className={screen.muted}>표시할 운동이 없습니다. 전체 루틴에서 운동을 추가해보세요.</p> : null}
        </Card>
      ) : null}

      {tab === 'all' ? (
        <div className={styles.routineStack}>
          <Card>
            <h3 className={styles.title}>전체 추천 루틴</h3>
            <p className={styles.smallMeta}>{workoutPlan?.routineType || '루틴 미선택'} · {workoutPlanSummary}</p>
          </Card>
          {routines.map((routine, index) => {
            const selectedExercise = selectedExerciseByRoutine[routine.routineKey] ?? exerciseCatalog[0]?.id;
            return (
              <Card key={routine.routineKey}>
                <button type="button" className={styles.routineHeader} onClick={() => toggleRoutine(routine.routineKey)} aria-expanded={Boolean(openRoutineKeys[routine.routineKey])}>
                  <div>
                    <strong>{index + 1}일차 · {routine.todayFocus}</strong>
                    <small>{routine.exercises?.length ?? 0}개 운동</small>
                  </div>
                  <span>{routine.routineKey === workout?.routineKey ? '오늘 · ' : ''}{openRoutineKeys[routine.routineKey] ? '접기' : '열기'}</span>
                </button>
                {openRoutineKeys[routine.routineKey] ? <div className={styles.accordionBody}>
                <ExerciseAddControl
                  value={selectedExercise}
                  onChange={(value) => setSelectedExerciseByRoutine((prev) => ({ ...prev, [routine.routineKey]: value }))}
                  customValue={customExerciseByRoutine[routine.routineKey] ?? ''}
                  onCustomChange={(value) => setCustomExerciseByRoutine((prev) => ({ ...prev, [routine.routineKey]: value }))}
                  onAdd={() => addExercise(routine.routineKey)}
                  onAddCustom={() => addCustomExercise(routine.routineKey)}
                  disabled={!routine.routineKey}
                />
                <ExerciseList exercises={routine.exercises ?? []} onRemove={(exerciseId) => removeExercise(routine.routineKey, exerciseId)} />
                </div> : null}
              </Card>
            );
          })}
        </div>
      ) : null}

      <button type="button" className={styles.secondary} onClick={() => navigate('/exercise/add/equipment')}>기구/데이터셋 편집</button>
      {errorMessage ? <p className={screen.muted}>{errorMessage}</p> : null}
    </AppLayout>
  );
}

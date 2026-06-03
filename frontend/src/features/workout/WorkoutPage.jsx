import { useEffect, useMemo, useState } from 'react';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { getTodayWorkoutRecommendation } from '../../api/workoutApi';
import { emptyWorkout, exerciseCatalog } from '../../constants/defaultData';
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

function formatTime(totalSeconds) {
  const minutes = Math.floor(totalSeconds / 60).toString().padStart(2, '0');
  const seconds = Math.max(0, totalSeconds % 60).toString().padStart(2, '0');
  return `${minutes}:${seconds}`;
}

export default function WorkoutPage() {
  const [workout, setWorkout] = useState({ ...emptyWorkout, exercises: exerciseCatalog });
  const [selectedExerciseId, setSelectedExerciseId] = useState(exerciseCatalog[0]?.id ?? null);
  const [activeSet, setActiveSet] = useState(1);
  const [remainingSeconds, setRemainingSeconds] = useState(exerciseCatalog[0]?.durationSeconds ?? 0);
  const [isRunning, setIsRunning] = useState(false);
  const [completedSets, setCompletedSets] = useState({});
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    let mounted = true;
    async function load() {
      try {
        setLoading(true);
        const data = await getTodayWorkoutRecommendation();
        if (!mounted) return;
        const apiExercises = data?.exercises?.length ? data.exercises.map(toExercise) : [];
        const exercises = apiExercises.length > 0 ? apiExercises : exerciseCatalog.map(toExercise);
        setWorkout({
          todayFocus: data?.todayFocus ?? '오늘 운동 선택',
          routineType: data?.routineType ?? '자유 선택 루틴',
          exercises,
        });
        setSelectedExerciseId(exercises[0]?.id ?? null);
        setRemainingSeconds(exercises[0]?.durationSeconds ?? 0);
      } catch (error) {
        if (!mounted) return;
        const exercises = exerciseCatalog.map(toExercise);
        setWorkout({ todayFocus: '오늘 운동 선택', routineType: '자유 선택 루틴', exercises });
        setSelectedExerciseId(exercises[0]?.id ?? null);
        setRemainingSeconds(exercises[0]?.durationSeconds ?? 0);
        setErrorMessage('서버 추천을 불러오지 못해 기본 운동 목록을 표시합니다.');
      } finally {
        if (mounted) setLoading(false);
      }
    }

    load();
    return () => {
      mounted = false;
    };
  }, []);

  const exercises = workout?.exercises ?? [];
  const selectedExercise = exercises.find((exercise) => exercise.id === selectedExerciseId) ?? exercises[0];
  const selectedCompletedSets = selectedExercise ? completedSets[selectedExercise.id] ?? 0 : 0;
  const totalSets = selectedExercise?.sets ?? 0;
  const isSelectedComplete = totalSets > 0 && selectedCompletedSets >= totalSets;
  const allWorkoutComplete = exercises.length > 0 && exercises.every((exercise) => (completedSets[exercise.id] ?? 0) >= exercise.sets);

  useEffect(() => {
    if (!isRunning) return undefined;
    if (remainingSeconds <= 0) return undefined;

    const timer = window.setInterval(() => {
      setRemainingSeconds((seconds) => Math.max(0, seconds - 1));
    }, 1000);

    return () => window.clearInterval(timer);
  }, [isRunning, remainingSeconds]);

  useEffect(() => {
    if (!selectedExercise) return;
    setActiveSet(Math.min((completedSets[selectedExercise.id] ?? 0) + 1, selectedExercise.sets));
    setRemainingSeconds(selectedExercise.durationSeconds);
    setIsRunning(false);
  }, [selectedExerciseId]);

  const totalCalories = useMemo(
    () => exercises.reduce((sum, exercise) => sum + (exercise.caloriesBurned ?? 0), 0),
    [exercises]
  );

  const totalSetCount = exercises.reduce((sum, exercise) => sum + exercise.sets, 0);
  const completedSetCount = exercises.reduce((sum, exercise) => sum + Math.min(completedSets[exercise.id] ?? 0, exercise.sets), 0);

  const selectExercise = (exercise) => {
    setSelectedExerciseId(exercise.id);
  };

  const completeCurrentSet = () => {
    if (!selectedExercise || isSelectedComplete) return;

    setCompletedSets((prev) => {
      const nextCount = Math.min((prev[selectedExercise.id] ?? 0) + 1, selectedExercise.sets);
      return { ...prev, [selectedExercise.id]: nextCount };
    });
    setIsRunning(false);
    setRemainingSeconds(selectedExercise.restSeconds || selectedExercise.durationSeconds);
    setActiveSet((set) => Math.min(set + 1, selectedExercise.sets));
  };

  const resetTodayWorkout = () => {
    setCompletedSets({});
    setIsRunning(false);
    if (selectedExercise) {
      setActiveSet(1);
      setRemainingSeconds(selectedExercise.durationSeconds);
    }
  };

  return (
    <AppLayout title="운동 탭" subtitle="운동을 고르고 세트별 타이머로 진행하세요." headerAction={<span className={styles.calendar}>🗓️</span>}>
      <Card className={allWorkoutComplete ? styles.completeHero : ''}>
        <div className={styles.row}>
          <span>{allWorkoutComplete ? '오늘 하루 운동 완료' : '오늘 운동 진행률'}</span>
          <strong>{completedSetCount} / {totalSetCount}세트</strong>
        </div>
        <p className={styles.kcal}>{totalCalories.toLocaleString()} kcal</p>
        <p className={screen.muted}>{workout?.todayFocus || '운동 데이터 없음'} · {workout?.routineType || '루틴 미선택'}</p>
        {allWorkoutComplete ? <button type="button" className={styles.finishButton} onClick={resetTodayWorkout}>운동 끝내기</button> : null}
      </Card>

      <Card className={styles.timerCard}>
        {selectedExercise ? (
          <>
            <div className={styles.timerTop}>
              <div>
                <span className={styles.category}>{selectedExercise.category}</span>
                <h3>{selectedExercise.exerciseName}</h3>
              </div>
              <span className={isSelectedComplete ? styles.doneBadge : styles.setBadge}>
                {isSelectedComplete ? '완료' : `${activeSet}/${totalSets}세트`}
              </span>
            </div>
            <div className={styles.timerCircle}>{formatTime(remainingSeconds)}</div>
            <p className={styles.timerHint}>{remainingSeconds === 0 ? '세트 시간이 끝났습니다. 완료를 눌러 기록하세요.' : `${selectedExercise.reps} · 휴식 ${selectedExercise.restSeconds}초`}</p>
            <div className={styles.timerActions}>
              <button type="button" onClick={() => setIsRunning((value) => !value)} disabled={isSelectedComplete}>{isRunning ? '일시정지' : '시작'}</button>
              <button type="button" className={styles.secondaryButton} onClick={() => setRemainingSeconds(selectedExercise.durationSeconds)} disabled={isSelectedComplete}>다시</button>
              <button type="button" className={styles.completeButton} onClick={completeCurrentSet} disabled={isSelectedComplete}>세트 완료</button>
            </div>
          </>
        ) : (
          <p className={screen.muted}>선택 가능한 운동이 없습니다.</p>
        )}
      </Card>

      <Card>
        <div className={styles.row}>
          <h3 className={styles.title}>운동 목록</h3>
          <span className={styles.smallMeta}>클릭해서 운동 선택</span>
        </div>
        <ul className={styles.list}>
          {exercises.map((routine) => {
            const doneSets = completedSets[routine.id] ?? 0;
            const complete = doneSets >= routine.sets;
            return (
              <li key={routine.id} className={`${selectedExercise?.id === routine.id ? styles.selected : ''} ${complete ? styles.completed : ''}`}>
                <button type="button" onClick={() => selectExercise(routine)}>
                  <span>
                    <strong>{routine.exerciseName}</strong>
                    <small>{routine.category} · {routine.sets}세트 · {routine.reps}</small>
                  </span>
                  <em>{complete ? '완료' : `${doneSets}/${routine.sets}`}</em>
                </button>
              </li>
            );
          })}
        </ul>
        {loading ? <p className={screen.muted}>불러오는 중...</p> : null}
        {errorMessage ? <p className={screen.muted}>{errorMessage}</p> : null}
      </Card>
    </AppLayout>
  );
}

import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { exerciseCatalog } from '../../constants/defaultData';
import { markWorkoutComplete, readWorkoutProgress, saveCompletedSets, saveWorkoutPlan } from '../../utils/workoutStorage';
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

function fallbackWorkout() {
  return {
    todayFocus: '오늘 운동 선택',
    routineType: '자유 선택 루틴',
    exercises: exerciseCatalog.map(toExercise),
  };
}

function formatTime(totalSeconds) {
  const minutes = Math.floor(totalSeconds / 60).toString().padStart(2, '0');
  const seconds = Math.max(0, totalSeconds % 60).toString().padStart(2, '0');
  return `${minutes}:${seconds}`;
}

function findNextExerciseId(exercises, completedSets, currentExerciseId) {
  const currentIndex = exercises.findIndex((exercise) => exercise.id === currentExerciseId);
  const ordered = [...exercises.slice(currentIndex + 1), ...exercises.slice(0, currentIndex + 1)];
  return ordered.find((exercise) => (completedSets[exercise.id] ?? 0) < exercise.sets)?.id ?? currentExerciseId;
}

export default function WorkoutSessionPage() {
  const navigate = useNavigate();
  const savedProgress = useMemo(() => readWorkoutProgress(), []);
  const initialWorkout = savedProgress?.exercises?.length
    ? { todayFocus: savedProgress.todayFocus, routineType: savedProgress.routineType, exercises: savedProgress.exercises }
    : fallbackWorkout();

  const [workout] = useState(initialWorkout);
  const [selectedExerciseId, setSelectedExerciseId] = useState(initialWorkout.exercises[0]?.id ?? null);
  const [completedSets, setCompletedSets] = useState(savedProgress?.completedSets ?? {});
  const [remainingSeconds, setRemainingSeconds] = useState(initialWorkout.exercises[0]?.durationSeconds ?? 0);
  const [isRunning, setIsRunning] = useState(false);
  const [timerPhase, setTimerPhase] = useState('work');
  const [pendingExerciseId, setPendingExerciseId] = useState(null);

  const exercises = workout?.exercises ?? [];
  const selectedExercise = exercises.find((exercise) => exercise.id === selectedExerciseId) ?? exercises[0];
  const selectedCompletedSets = selectedExercise ? completedSets[selectedExercise.id] ?? 0 : 0;
  const activeSet = selectedExercise ? Math.min(selectedCompletedSets + 1, selectedExercise.sets) : 0;
  const isSelectedComplete = selectedExercise ? selectedCompletedSets >= selectedExercise.sets : false;
  const allWorkoutComplete = exercises.length > 0 && exercises.every((exercise) => (completedSets[exercise.id] ?? 0) >= exercise.sets);
  const totalSetCount = exercises.reduce((sum, exercise) => sum + exercise.sets, 0);
  const completedSetCount = exercises.reduce((sum, exercise) => sum + Math.min(completedSets[exercise.id] ?? 0, exercise.sets), 0);

  useEffect(() => {
    saveWorkoutPlan(workout);
  }, [workout]);

  useEffect(() => {
    if (!selectedExercise) return;
    setTimerPhase('work');
    setPendingExerciseId(null);
    setRemainingSeconds(selectedExercise.durationSeconds);
    setIsRunning(false);
  }, [selectedExerciseId, selectedExercise]);

  useEffect(() => {
    if (!isRunning || remainingSeconds <= 0) return undefined;

    const timer = window.setInterval(() => {
      setRemainingSeconds((seconds) => Math.max(0, seconds - 1));
    }, 1000);

    return () => window.clearInterval(timer);
  }, [isRunning, remainingSeconds]);

  useEffect(() => {
    if (!isRunning || remainingSeconds !== 0 || timerPhase !== 'rest') return;
    setTimerPhase('work');
    setIsRunning(false);
    const nextId = pendingExerciseId;
    if (nextId) {
      setSelectedExerciseId(nextId);
      setPendingExerciseId(null);
      const nextExercise = exercises.find((exercise) => exercise.id === nextId);
      setRemainingSeconds(nextExercise?.durationSeconds ?? 0);
    } else if (selectedExercise) {
      setRemainingSeconds(selectedExercise.durationSeconds);
    }
  }, [exercises, isRunning, pendingExerciseId, remainingSeconds, selectedExercise, timerPhase]);

  const completeCurrentSet = () => {
    if (!selectedExercise || isSelectedComplete) return;

    const nextCompletedSets = {
      ...completedSets,
      [selectedExercise.id]: Math.min(selectedCompletedSets + 1, selectedExercise.sets),
    };
    setCompletedSets(nextCompletedSets);
    saveCompletedSets(nextCompletedSets);
    setIsRunning(false);

    const nextExerciseId = findNextExerciseId(exercises, nextCompletedSets, selectedExercise.id);
    const currentStillHasSets = nextCompletedSets[selectedExercise.id] < selectedExercise.sets;
    const nextExercise = exercises.find((exercise) => exercise.id === nextExerciseId);

    if (!allWorkoutComplete && selectedExercise.restSeconds > 0) {
      setTimerPhase('rest');
      setPendingExerciseId(currentStillHasSets ? selectedExercise.id : nextExerciseId);
      setRemainingSeconds(selectedExercise.restSeconds);
      setIsRunning(true);
      return;
    }

    setTimerPhase('work');
    setSelectedExerciseId(nextExerciseId);
    setRemainingSeconds(nextExercise?.durationSeconds ?? 0);
  };

  const completeWorkout = () => {
    markWorkoutComplete(workout);
    navigate('/home');
  };

  return (
    <AppLayout title="운동 진행" subtitle="현재 운동과 타이머를 보며 세트별로 기록하세요." showBottomNav={false}>
      <Card className={styles.timerCard}>
        {selectedExercise ? (
          <>
            <div className={styles.timerTop}>
              <div>
                <span className={styles.category}>{timerPhase === 'rest' ? '휴식' : selectedExercise.category}</span>
                <h3>{selectedExercise.exerciseName}</h3>
              </div>
              <span className={isSelectedComplete ? styles.doneBadge : styles.setBadge}>
                {isSelectedComplete ? '완료' : `${activeSet}/${selectedExercise.sets}세트`}
              </span>
            </div>
            <div className={`${styles.timerCircle} ${timerPhase === 'rest' ? styles.restCircle : ''}`}>{formatTime(remainingSeconds)}</div>
            <p className={styles.timerHint}>{timerPhase === 'rest' ? '휴식 중입니다. 시간이 끝나면 다음 세트를 준비하세요.' : (remainingSeconds === 0 ? '세트 시간이 끝났습니다. 세트 완료를 눌러 기록하세요.' : `${selectedExercise.reps} · 휴식 ${selectedExercise.restSeconds}초`)}</p>
            <div className={styles.timerActions}>
              <button type="button" onClick={() => setIsRunning((value) => !value)} disabled={isSelectedComplete || allWorkoutComplete}>{isRunning ? '일시정지' : '시작'}</button>
              <button type="button" className={styles.secondaryButton} onClick={() => setRemainingSeconds(timerPhase === 'rest' ? selectedExercise.restSeconds : selectedExercise.durationSeconds)} disabled={isSelectedComplete || allWorkoutComplete}>다시</button>
              <button type="button" className={styles.completeButton} onClick={completeCurrentSet} disabled={timerPhase === 'rest' || isSelectedComplete || allWorkoutComplete}>세트 완료</button>
            </div>
          </>
        ) : (
          <p className={screen.muted}>진행할 운동이 없습니다.</p>
        )}
      </Card>

      <Card className={allWorkoutComplete ? styles.completeHero : ''}>
        <div className={styles.row}>
          <span>전체 진행률</span>
          <strong>{completedSetCount} / {totalSetCount}세트</strong>
        </div>
        <p className={screen.muted}>모든 세트를 끝낸 뒤 운동 완료를 누르면 홈 화면의 이번 주 운동 잔디가 채워집니다.</p>
        {allWorkoutComplete ? (
          <button type="button" className={styles.finishButton} onClick={completeWorkout}>운동 완료</button>
        ) : null}
      </Card>

      <Card>
        <div className={styles.row}>
          <h3 className={styles.title}>오늘 루틴</h3>
          <button type="button" onClick={() => navigate('/exercise')}>목록으로</button>
        </div>
        <ul className={styles.list}>
          {exercises.map((routine) => {
            const doneSets = completedSets[routine.id] ?? 0;
            const complete = doneSets >= routine.sets;
            return (
              <li key={routine.id} className={`${selectedExercise?.id === routine.id ? styles.selected : ''} ${complete ? styles.completed : ''}`}>
                <button type="button" onClick={() => setSelectedExerciseId(routine.id)}>
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
      </Card>
    </AppLayout>
  );
}

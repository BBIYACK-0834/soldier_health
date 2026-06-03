import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { getTodayWorkoutRecommendation } from '../../api/workoutApi';
import { emptyWorkout, exerciseCatalog } from '../../constants/defaultData';
import { readWorkoutProgress, resetWorkoutProgress, saveWorkoutPlan } from '../../utils/workoutStorage';
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

function buildDefaultWorkout(data) {
  const apiExercises = data?.exercises?.length ? data.exercises.map(toExercise) : [];
  const exercises = apiExercises.length > 0 ? apiExercises : exerciseCatalog.map(toExercise);
  return {
    todayFocus: data?.todayFocus ?? '오늘 운동 선택',
    routineType: data?.routineType ?? '자유 선택 루틴',
    exercises,
  };
}

export default function WorkoutPage() {
  const navigate = useNavigate();
  const [workout, setWorkout] = useState({ ...emptyWorkout, exercises: exerciseCatalog.map(toExercise) });
  const [completedSets, setCompletedSets] = useState({});
  const [workoutCompleted, setWorkoutCompleted] = useState(false);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    let mounted = true;
    async function load() {
      try {
        setLoading(true);
        const data = await getTodayWorkoutRecommendation();
        if (!mounted) return;
        const nextWorkout = buildDefaultWorkout(data);
        const saved = saveWorkoutPlan(nextWorkout);
        setWorkout(nextWorkout);
        setCompletedSets(saved?.completedSets ?? {});
        setWorkoutCompleted(Boolean(saved?.workoutCompleted));
      } catch (error) {
        if (!mounted) return;
        const saved = readWorkoutProgress();
        const nextWorkout = saved?.exercises?.length
          ? { todayFocus: saved.todayFocus, routineType: saved.routineType, exercises: saved.exercises }
          : buildDefaultWorkout();
        setWorkout(nextWorkout);
        setCompletedSets(saved?.completedSets ?? {});
        setWorkoutCompleted(Boolean(saved?.workoutCompleted));
        setErrorMessage('서버 추천을 불러오지 못해 저장된 운동 또는 기본 운동 목록을 표시합니다.');
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
      setWorkoutCompleted(Boolean(saved?.workoutCompleted));
      if (saved?.exercises?.length) {
        setWorkout({ todayFocus: saved.todayFocus, routineType: saved.routineType, exercises: saved.exercises });
      }
    };

    window.addEventListener('tg-workout-progress-updated', handleProgressUpdate);
    return () => window.removeEventListener('tg-workout-progress-updated', handleProgressUpdate);
  }, []);

  const exercises = workout?.exercises ?? [];
  const totalSetCount = exercises.reduce((sum, exercise) => sum + exercise.sets, 0);
  const completedSetCount = exercises.reduce((sum, exercise) => sum + Math.min(completedSets[exercise.id] ?? 0, exercise.sets), 0);
  const allWorkoutComplete = exercises.length > 0 && exercises.every((exercise) => (completedSets[exercise.id] ?? 0) >= exercise.sets);

  const startWorkout = () => {
    saveWorkoutPlan(workout);
    navigate('/exercise/session');
  };

  const resetTodayWorkout = () => {
    const saved = resetWorkoutProgress();
    setCompletedSets(saved?.completedSets ?? {});
    setWorkoutCompleted(false);
  };

  return (
    <AppLayout title="운동 탭" subtitle="운동 목록을 확인하고 별도 진행 화면에서 타이머를 시작하세요." headerAction={<span className={styles.calendar}>🗓️</span>}>
      <Card>
        <div className={styles.row}>
          <div>
            <h3 className={styles.title}>운동 목록</h3>
            <span className={styles.smallMeta}>{workout?.todayFocus || '운동 데이터 없음'} · {workout?.routineType || '루틴 미선택'}</span>
          </div>
          <button type="button" onClick={startWorkout} disabled={exercises.length === 0}>운동 시작</button>
        </div>
        <ul className={styles.list}>
          {exercises.map((routine) => {
            const doneSets = completedSets[routine.id] ?? 0;
            const complete = doneSets >= routine.sets;
            return (
              <li key={routine.id} className={complete ? styles.completed : ''}>
                <div className={styles.listItem}>
                  <span>
                    <strong>{routine.exerciseName}</strong>
                    <small>{routine.category} · {routine.sets}세트 · {routine.reps}</small>
                  </span>
                  <em>{complete ? '완료' : `${doneSets}/${routine.sets}`}</em>
                </div>
              </li>
            );
          })}
        </ul>
        {loading ? <p className={screen.muted}>불러오는 중...</p> : null}
        {errorMessage ? <p className={screen.muted}>{errorMessage}</p> : null}
      </Card>

      <Card className={workoutCompleted || allWorkoutComplete ? styles.completeHero : ''}>
        <div className={styles.row}>
          <span>{workoutCompleted || allWorkoutComplete ? '오늘 하루 운동 완료' : '오늘 운동 진행률'}</span>
          <strong>{completedSetCount} / {totalSetCount}세트</strong>
        </div>
        <p className={screen.muted}>
          운동 완료 버튼을 누르면 홈 화면의 이번 주 운동 잔디에 바로 반영됩니다.
        </p>
        {workoutCompleted || allWorkoutComplete ? <button type="button" className={styles.finishButton} onClick={resetTodayWorkout}>기록 초기화</button> : null}
      </Card>
    </AppLayout>
  );
}

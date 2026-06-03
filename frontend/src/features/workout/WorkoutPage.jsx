import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { getTodayWorkoutRecommendation } from '../../api/workoutApi';
import { mockExercises } from '../../constants/mockData';
import styles from './WorkoutPage.module.css';
import screen from '../../components/ui/Screen.module.css';

const REST_SECONDS = 60;
const DEFAULT_WORK_SECONDS = 60;

const getExerciseName = (exercise) => exercise?.exerciseName || exercise?.name || '운동';
const getExerciseSets = (exercise) => Math.max(1, Number(exercise?.sets) || 1);
const getExerciseDurationSeconds = (exercise) => {
  const durationMinutes = Number(exercise?.durationMinutes ?? exercise?.minutes);
  if (!Number.isFinite(durationMinutes) || durationMinutes <= 0) return DEFAULT_WORK_SECONDS;

  return Math.max(1, Math.round((durationMinutes * 60) / getExerciseSets(exercise)));
};

const createTimerState = (exerciseIndex = 0, setNumber = 1, phase = 'work', secondsLeft = DEFAULT_WORK_SECONDS, isRunning = false) => ({
  exerciseIndex,
  setNumber,
  phase,
  secondsLeft,
  isRunning,
});

export default function WorkoutPage() {
  const navigate = useNavigate();
  const [workout, setWorkout] = useState({ todayFocus: '체력 유지', routineType: '기초 체력 루틴', exercises: mockExercises });
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [timer, setTimer] = useState(() => createTimerState(0, 1, 'work', getExerciseDurationSeconds(mockExercises[0])));

  useEffect(() => {
    let mounted = true;
    async function load() {
      try {
        setLoading(true);
        const data = await getTodayWorkoutRecommendation();
        if (!mounted) return;
        setWorkout(data?.exercises?.length ? data : { todayFocus: data?.todayFocus || '체력 유지', routineType: data?.routineType || '기초 체력 루틴', exercises: mockExercises });
      } catch (error) {
        if (!mounted) return;
        setWorkout({ todayFocus: '체력 유지', routineType: '기초 체력 루틴', exercises: mockExercises });
        setErrorMessage('서버 연결 전이라 예시 운동 루틴으로 표시합니다.');
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
  const currentExercise = exercises[timer.exerciseIndex] ?? exercises[0] ?? mockExercises[0];
  const totalSets = getExerciseSets(currentExercise);
  const phaseTotalSeconds = timer.phase === 'rest' ? REST_SECONDS : getExerciseDurationSeconds(currentExercise);
  const elapsedSeconds = Math.max(0, phaseTotalSeconds - timer.secondsLeft);
  const progressPercent = phaseTotalSeconds > 0 ? Math.min(100, (elapsedSeconds / phaseTotalSeconds) * 100) : 0;
  const timerLabel = timer.phase === 'rest' ? '휴식' : '운동';
  const isRoutineComplete = exercises.length > 0 && timer.exerciseIndex >= exercises.length;

  useEffect(() => {
    if (exercises.length === 0) return;
    setTimer(createTimerState(0, 1, 'work', getExerciseDurationSeconds(exercises[0])));
  }, [exercises]);

  const moveToNextSetOrExercise = (previousTimer) => {
    const routine = workout?.exercises ?? [];
    const exercise = routine[previousTimer.exerciseIndex];
    const exerciseSets = getExerciseSets(exercise);

    if (previousTimer.setNumber < exerciseSets) {
      const nextSetNumber = previousTimer.setNumber + 1;
      return createTimerState(
        previousTimer.exerciseIndex,
        nextSetNumber,
        'work',
        getExerciseDurationSeconds(exercise),
        false
      );
    }

    const nextExerciseIndex = previousTimer.exerciseIndex + 1;
    const nextExercise = routine[nextExerciseIndex];
    if (!nextExercise) {
      return createTimerState(nextExerciseIndex, 1, 'work', 0, false);
    }

    return createTimerState(nextExerciseIndex, 1, 'work', getExerciseDurationSeconds(nextExercise), false);
  };

  const moveToRest = (previousTimer) => createTimerState(
    previousTimer.exerciseIndex,
    previousTimer.setNumber,
    'rest',
    REST_SECONDS,
    false
  );

  const handlePhaseComplete = (previousTimer) => (previousTimer.phase === 'work' ? moveToRest(previousTimer) : moveToNextSetOrExercise(previousTimer));

  useEffect(() => {
    if (!timer.isRunning || isRoutineComplete) return undefined;

    const intervalId = window.setInterval(() => {
      setTimer((previousTimer) => {
        if (!previousTimer.isRunning) return previousTimer;
        if (previousTimer.secondsLeft <= 1) return handlePhaseComplete(previousTimer);
        return { ...previousTimer, secondsLeft: previousTimer.secondsLeft - 1 };
      });
    }, 1000);

    return () => window.clearInterval(intervalId);
  }, [timer.isRunning, isRoutineComplete, workout]);

  const totalCalories = useMemo(
    () => (workout?.exercises ?? []).reduce((sum, exercise) => sum + (exercise.caloriesBurned ?? exercise.calories ?? 0), 0),
    [workout]
  );

  const formattedTime = useMemo(() => {
    const minutes = Math.floor(timer.secondsLeft / 60).toString().padStart(2, '0');
    const seconds = (timer.secondsLeft % 60).toString().padStart(2, '0');
    return `${minutes}:${seconds}`;
  }, [timer.secondsLeft]);

  const toggleTimer = () => {
    if (isRoutineComplete) {
      const firstExercise = exercises[0] ?? mockExercises[0];
      setTimer(createTimerState(0, 1, 'work', getExerciseDurationSeconds(firstExercise), true));
      return;
    }

    setTimer((previousTimer) => ({ ...previousTimer, isRunning: !previousTimer.isRunning }));
  };

  const completeSet = () => {
    if (isRoutineComplete) return;
    setTimer((previousTimer) => moveToNextSetOrExercise(previousTimer));
  };

  return (
    <AppLayout title="운동 기록" subtitle="오늘 수행한 운동을 요약합니다." headerAction={<span className={styles.calendar}>🗓️</span>}>
      <Card>
        <div className={styles.row}>
          <span>총 소모 칼로리</span>
          <button type="button" onClick={() => navigate('/exercise/routine/edit')}>운동 수정</button>
        </div>
        <p className={styles.kcal}>{totalCalories.toLocaleString()} kcal</p>
        <p className={screen.muted}>{workout?.todayFocus} · {workout?.routineType}</p>
      </Card>

      <Card>
        <div className={styles.row}>
          <div>
            <p className={styles.timerEyebrow}>{isRoutineComplete ? '루틴 완료' : `${timerLabel} 타이머`}</p>
            <h3 className={styles.title}>{isRoutineComplete ? '오늘 루틴을 모두 마쳤습니다' : getExerciseName(currentExercise)}</h3>
          </div>
          <span className={styles.setBadge}>{isRoutineComplete ? '완료' : `${timer.setNumber}/${totalSets} 세트`}</span>
        </div>
        <div className={styles.timerPanel}>
          <div
            className={styles.timerRing}
            style={{ '--timer-progress': `${progressPercent}%` }}
            aria-label={`${timerLabel} 진행률 ${Math.round(progressPercent)}%`}
          >
            <div className={styles.timerFace}>
              <span className={styles.timerPhase}>{isRoutineComplete ? '완료' : timerLabel}</span>
              <strong>{isRoutineComplete ? '00:00' : formattedTime}</strong>
            </div>
          </div>
          <div className={styles.timerActions}>
            <button type="button" className={styles.primaryAction} onClick={toggleTimer}>{timer.isRunning ? '일시정지' : isRoutineComplete ? '다시 시작' : '시작'}</button>
            <button type="button" className={styles.secondaryAction} onClick={completeSet} disabled={isRoutineComplete}>세트 완료</button>
          </div>
          <p className={styles.timerHint}>{timer.phase === 'work' ? '시간이 끝나면 자동으로 휴식 타이머로 전환되고, 세트 완료를 누르면 다음 세트로 이동합니다.' : '휴식 후 세트 완료를 누르면 다음 세트 또는 다음 운동으로 이동합니다.'}</p>
        </div>
      </Card>

      <Card>
        <div className={styles.row}>
          <h3 className={styles.title}>운동 루틴</h3>
          <button type="button" onClick={() => navigate('/exercise/add/equipment')}>+ 추가</button>
        </div>
        <ul className={styles.list}>
          {exercises.map((routine, index) => (
            <li key={routine.id ?? `${routine.name}-${routine.sets}`} className={index === timer.exerciseIndex && !isRoutineComplete ? styles.currentRoutine : ''}>
              <strong>{getExerciseName(routine)}</strong>
              <span>{routine.sets ? `${routine.sets}세트 · ` : ''}{routine.reps ? `${routine.reps}회 · ` : ''}{routine.durationMinutes ?? routine.minutes ?? 0}분</span>
            </li>
          ))}
        </ul>
        {loading ? <p className={screen.muted}>불러오는 중...</p> : null}
        {errorMessage ? <p className={screen.muted}>{errorMessage}</p> : null}
      </Card>
    </AppLayout>
  );
}

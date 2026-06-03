import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { getTodayWorkoutRecommendation } from '../../api/workoutApi';
import { mockExercises } from '../../constants/mockData';
import styles from './WorkoutPage.module.css';
import screen from '../../components/ui/Screen.module.css';

export default function WorkoutPage() {
  const navigate = useNavigate();
  const [workout, setWorkout] = useState({ todayFocus: '체력 유지', routineType: '기초 체력 루틴', exercises: mockExercises });
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

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

  const totalCalories = useMemo(
    () => (workout?.exercises ?? []).reduce((sum, exercise) => sum + (exercise.caloriesBurned ?? exercise.calories ?? 0), 0),
    [workout]
  );

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
          <h3 className={styles.title}>운동 루틴</h3>
          <button type="button" onClick={() => navigate('/exercise/add/equipment')}>+ 추가</button>
        </div>
        <ul className={styles.list}>
          {(workout?.exercises ?? []).map((routine) => (
            <li key={routine.id ?? `${routine.name}-${routine.sets}`}>
              <strong>{routine.exerciseName || routine.name}</strong>
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

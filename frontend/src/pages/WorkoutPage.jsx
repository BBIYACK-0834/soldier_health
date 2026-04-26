import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import { getTodayWorkoutRecommendation } from '../api/workoutApi';
import styles from '../features/design/WorkoutPage.module.css';

export default function WorkoutPage() {
  const navigate = useNavigate();
  const [workout, setWorkout] = useState(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    let mounted = true;
    async function load() {
      try {
        setLoading(true);
        const data = await getTodayWorkoutRecommendation();
        if (!mounted) return;
        setWorkout(data ?? null);
      } catch (error) {
        if (!mounted) return;
        setErrorMessage(error.message || '운동 추천 정보를 불러오지 못했습니다.');
      } finally {
        if (mounted) setLoading(false);
      }
    }

    load();
    return () => {
      mounted = false;
    };
  }, []);

  return (
    <AppLayout title="운동 기록" subtitle="추천 API 데이터" headerAction={<span className={styles.calendar}>🗓️</span>}>
      <Card>
        <div className={styles.row}><span>오늘 포커스</span><button type="button" onClick={() => navigate('/workout/edit')}>운동 설정</button></div>
        <p className={styles.kcal}>{workout?.todayFocus || '-'}</p>
        <p>{workout?.routineType || '등록된 루틴 유형이 없습니다.'}</p>
      </Card>
      <Card>
        <h3 className={styles.title}>운동 루틴</h3>
        {workout?.exercises?.length > 0 ? (
          <ul className={styles.list}>
            {workout.exercises.map((routine) => <li key={`${routine.name}-${routine.sets}`}><strong>{routine.name}</strong><span>{routine.sets}세트 · {routine.reps}</span></li>)}
          </ul>
        ) : (
          <p>등록된 운동 추천 데이터가 없습니다.</p>
        )}
        {loading ? <p>불러오는 중...</p> : null}
        {errorMessage ? <p>{errorMessage}</p> : null}
      </Card>
    </AppLayout>
  );
}

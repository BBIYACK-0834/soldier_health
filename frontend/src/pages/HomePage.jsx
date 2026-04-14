import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MobileShell from '../components/layout/MobileShell';
import { useAppContext } from '../app/AppContext';
import { getMe } from '../api/authApi';
import { getMyUnit } from '../api/unitApi';
import { getTodayMeal } from '../api/mealApi';
import { getTodayNutritionRecommendation } from '../api/nutritionApi';
import { getTodayWorkoutRecommendation } from '../api/workoutApi';
import { isProfileReady } from '../utils/profile';
import styles from '../features/home/HomeCards.module.css';

export default function HomePage() {
  const navigate = useNavigate();
  const { state, actions } = useAppContext();
  const [nickname, setNickname] = useState(state.user?.nickname || '전사');
  const [meal, setMeal] = useState(null);
  const [nutrition, setNutrition] = useState(null);
  const [workout, setWorkout] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const me = await getMe();
        const unit = await getMyUnit().catch(() => null);

        if (!isProfileReady(me, Boolean(unit?.id))) {
          navigate('/onboarding', { replace: true });
          return;
        }

        const [mealData, nutritionData, workoutData] = await Promise.all([
          getTodayMeal().catch(() => null),
          getTodayNutritionRecommendation().catch(() => null),
          getTodayWorkoutRecommendation().catch(() => null),
        ]);

        setNickname(me.nickname || '전사');
        actions.setUser(me);
        setMeal(mealData);
        setNutrition(nutritionData);
        setWorkout(workoutData);
      } catch {
        actions.logout();
      } finally {
        setIsLoading(false);
      }
    })();
  }, [actions, navigate]);

  const summary = nutrition?.summary;
  const caloriesText = useMemo(
    () => `${summary?.intakeCalories ?? 0} / ${summary?.targetCalories ?? 0} kcal`,
    [summary?.intakeCalories, summary?.targetCalories]
  );

  if (isLoading) {
    return (
      <MobileShell title="특급전사">
        <p className={styles.muted}>로딩 중입니다...</p>
      </MobileShell>
    );
  }

  return (
    <MobileShell title="특급전사">
      <section className={styles.hero}>
        <p className={styles.heroTitle}>메인 대시보드</p>
        <p className={styles.heroSub}>{nickname}님의 오늘 핵심 정보만 모아봤어요.</p>
      </section>

      <section className={styles.card}>
        <h3>그날 식단</h3>
        <div className={styles.subInfoBox}>
          <p className={styles.subText}>조식: {meal?.breakfastRaw || '정보 없음'}</p>
          <p className={styles.subText}>중식: {meal?.lunchRaw || '정보 없음'}</p>
          <p className={styles.subText}>석식: {meal?.dinnerRaw || '정보 없음'}</p>
        </div>
      </section>

      <section className={styles.card}>
        <h3>칼로리 현황</h3>
        <p className={styles.item}><span className={styles.label}>내가 먹은 칼로리</span>{summary?.intakeCalories ?? 0}kcal</p>
        <p className={styles.item}><span className={styles.label}>먹어야 할 칼로리</span>{summary?.targetCalories ?? 0}kcal</p>
        <p className={styles.item}><span className={styles.label}>진행</span><span className={styles.strongNumber}>{caloriesText}</span></p>
      </section>

      <section className={styles.card}>
        <h3>오늘의 운동 루틴</h3>
        <p className={styles.item}><span className={styles.label}>포커스</span>{workout?.todayFocus || workout?.routineType || '루틴 정보 없음'}</p>
        <button type="button" className={styles.primaryButton} onClick={() => navigate('/workout')}>
          운동 상세로 이동
        </button>
      </section>
    </MobileShell>
  );
}

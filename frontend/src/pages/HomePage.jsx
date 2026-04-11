import { useEffect, useState } from 'react';
import MobileShell from '../components/layout/MobileShell';
import { useAppContext } from '../app/AppContext';
import { getMe } from '../api/authApi';
import { getTodayMeal } from '../api/mealApi';
import { getTodayNutritionRecommendation } from '../api/nutritionApi';
import { getTodayWorkoutRecommendation } from '../api/workoutApi';
import { getMyAlarms } from '../api/alarmApi';
import styles from '../features/home/HomeCards.module.css';

function HomeCard({ title, children }) {
  return (
    <section className={styles.card}>
      <h3>{title}</h3>
      {children}
    </section>
  );
}

export default function HomePage() {
  const { state, actions } = useAppContext();
  const [nickname, setNickname] = useState(state.user?.nickname || '전사');
  const [meal, setMeal] = useState(null);
  const [nutrition, setNutrition] = useState(null);
  const [workout, setWorkout] = useState(null);
  const [nextAlarm, setNextAlarm] = useState(null);

  useEffect(() => {
    (async () => {
      try {
        const [me, mealData, nutritionData, workoutData, alarms] = await Promise.all([
          getMe(),
          getTodayMeal(),
          getTodayNutritionRecommendation(),
          getTodayWorkoutRecommendation(),
          getMyAlarms(),
        ]);
        setNickname(me.nickname || '전사');
        actions.setUser(me);
        setMeal(mealData);
        setNutrition(nutritionData);
        setWorkout(workoutData);
        const enabled = (alarms || []).filter((alarm) => alarm.enabled);
        setNextAlarm(enabled.length ? enabled[0] : null);
      } catch {
        actions.logout();
      }
    })();
  }, [actions]);

  return (
    <MobileShell title="특급전사 홈" actions={<button onClick={actions.logout}>로그아웃</button>}>
      <p className={styles.muted}>{nickname}님, 오늘도 전투력 관리 시작!</p>

      <HomeCard title="오늘 식단">
        {meal ? (
          <>
            <p className={styles.item}><strong>조식:</strong> {meal.breakfastRaw}</p>
            <p className={styles.item}><strong>중식:</strong> {meal.lunchRaw}</p>
            <p className={styles.item}><strong>석식:</strong> {meal.dinnerRaw}</p>
          </>
        ) : (
          <p className={styles.muted}>식단 데이터가 없습니다.</p>
        )}
      </HomeCard>

      <HomeCard title="오늘 운동">
        {workout ? (
          <>
            <p className={styles.item}>{workout.routineType} / {workout.todayFocus}</p>
            {workout.exercises?.map((exercise) => (
              <p className={styles.item} key={exercise.name}>
                - {exercise.name} {exercise.sets}세트 ({exercise.reps})
              </p>
            ))}
          </>
        ) : (
          <p className={styles.muted}>운동 추천 데이터가 없습니다.</p>
        )}
      </HomeCard>

      <HomeCard title="오늘 부족 영양소">
        {nutrition ? (
          <>
            <p className={styles.item}>단백질 부족: {nutrition.summary.deficitProteinG}g</p>
            <p className={styles.item}>탄수 부족: {nutrition.summary.deficitCarbG}g</p>
            <p className={styles.item}>지방 부족: {nutrition.summary.deficitFatG}g</p>
            <p className={styles.muted}>{nutrition.recommendationText}</p>
            <p className={styles.item}><strong>보유식품 추천:</strong> {(nutrition.ownedFoodSuggestions || []).join(', ') || '없음'}</p>
            <p className={styles.item}><strong>PX 추천:</strong> {(nutrition.pxSuggestions || []).join(', ') || '없음'}</p>
          </>
        ) : (
          <p className={styles.muted}>영양 추천 데이터가 없습니다.</p>
        )}
      </HomeCard>

      <HomeCard title="다음 운동 알람">
        {nextAlarm ? (
          <p className={styles.item}>{String(nextAlarm.hour).padStart(2, '0')}:{String(nextAlarm.minute).padStart(2, '0')} / {nextAlarm.repeatDaysJson}</p>
        ) : (
          <p className={styles.muted}>설정된 알람이 없습니다.</p>
        )}
      </HomeCard>
    </MobileShell>
  );
}

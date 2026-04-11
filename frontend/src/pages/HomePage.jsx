import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MobileShell from '../components/layout/MobileShell';
import { useAppContext } from '../app/AppContext';
import { getMe } from '../api/authApi';
import { getTodayMeal } from '../api/mealApi';
import { getTodayNutritionRecommendation } from '../api/nutritionApi';
import { getTodayWorkoutRecommendation } from '../api/workoutApi';
import { getMyAlarms } from '../api/alarmApi';
import { getMyUnit } from '../api/unitApi';
import { WEEKDAY_LABELS, toLabel } from '../constants/labels';
import { isProfileReady } from '../utils/profile';
import styles from '../features/home/HomeCards.module.css';

function HomeCard({ title, children }) {
  return (
    <section className={styles.card}>
      <h3>{title}</h3>
      {children}
    </section>
  );
}

function hasMealData(meal) {
  return Boolean(meal?.breakfastRaw || meal?.lunchRaw || meal?.dinnerRaw);
}

function formatRepeatDays(raw) {
  if (!raw) return '반복 없음';
  return raw
    .split(',')
    .map((day) => toLabel(WEEKDAY_LABELS, day.trim(), day.trim()))
    .join(', ');
}

export default function HomePage() {
  const navigate = useNavigate();
  const { state, actions } = useAppContext();
  const [nickname, setNickname] = useState(state.user?.nickname || '전사');
  const [meal, setMeal] = useState(null);
  const [nutrition, setNutrition] = useState(null);
  const [workout, setWorkout] = useState(null);
  const [nextAlarm, setNextAlarm] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const me = await getMe();
        let unit = null;
        try {
          unit = await getMyUnit();
        } catch {
          unit = null;
        }

        if (!isProfileReady(me, Boolean(unit?.id))) {
          navigate('/onboarding', { replace: true });
          return;
        }

        const [mealData, nutritionData, workoutData, alarms] = await Promise.all([
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
      } finally {
        setIsLoading(false);
      }
    })();
  }, [actions, navigate]);

  if (isLoading) {
    return <MobileShell title="특급전사 홈"><p className={styles.muted}>로딩 중입니다...</p></MobileShell>;
  }

  return (
    <MobileShell title="특급전사 홈" actions={<button className={styles.logout} onClick={actions.logout}>로그아웃</button>}>
      <p className={styles.muted}>{nickname}님, 오늘도 전투력 관리를 시작합니다.</p>

      <HomeCard title="오늘 식단">
        {hasMealData(meal) ? (
          <>
            <p className={styles.item}><strong>조식:</strong> {meal.breakfastRaw || '정보 없음'}</p>
            <p className={styles.item}><strong>중식:</strong> {meal.lunchRaw || '정보 없음'}</p>
            <p className={styles.item}><strong>석식:</strong> {meal.dinnerRaw || '정보 없음'}</p>
          </>
        ) : (
          <p className={styles.muted}>오늘 식단 정보가 아직 없습니다. 식단 데이터가 준비되지 않았습니다.</p>
        )}
      </HomeCard>

      <HomeCard title="오늘 운동 추천">
        {workout ? (
          <>
            <p className={styles.item}>{workout.routineType} · {workout.todayFocus}</p>
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

      <HomeCard title="부족 영양소">
        {nutrition ? (
          <>
            <p className={styles.item}>단백질 부족: {nutrition.summary.deficitProteinG}g</p>
            <p className={styles.item}>탄수화물 부족: {nutrition.summary.deficitCarbG}g</p>
            <p className={styles.item}>지방 부족: {nutrition.summary.deficitFatG}g</p>
            <p className={styles.muted}>{nutrition.recommendationText}</p>
            <p className={styles.item}><strong>보유 식품 추천:</strong> {(nutrition.ownedFoodSuggestions || []).join(', ') || '없음'}</p>
            <p className={styles.item}><strong>PX 추천:</strong> {(nutrition.pxSuggestions || []).join(', ') || '없음'}</p>
          </>
        ) : (
          <p className={styles.muted}>영양 추천 데이터가 없습니다.</p>
        )}
      </HomeCard>

      <HomeCard title="다음 운동 알람">
        {nextAlarm ? (
          <p className={styles.item}>
            {String(nextAlarm.hour).padStart(2, '0')}:{String(nextAlarm.minute).padStart(2, '0')} / {formatRepeatDays(nextAlarm.repeatDaysJson)}
          </p>
        ) : (
          <p className={styles.muted}>설정된 운동 알람이 없습니다.</p>
        )}
      </HomeCard>
    </MobileShell>
  );
}

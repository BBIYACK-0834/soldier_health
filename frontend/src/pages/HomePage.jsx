import { useEffect, useMemo, useState } from 'react';
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

function MainSection({ title, subtitle, children }) {
  return (
    <section className={styles.card}>
      <h3>{title}</h3>
      {subtitle ? <p className={styles.subtitle}>{subtitle}</p> : null}
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
  const [sessionStarted, setSessionStarted] = useState(false);
  const [completedExercises, setCompletedExercises] = useState({});

  useEffect(() => {
    (async () => {
      try {
        const me = await getMe();
        const unit = await getMyUnit().catch(() => null);

        if (!isProfileReady(me, Boolean(unit?.id))) {
          navigate('/onboarding', { replace: true });
          return;
        }

        const [mealData, nutritionData, workoutData, alarms] = await Promise.all([
          getTodayMeal().catch(() => null),
          getTodayNutritionRecommendation().catch(() => null),
          getTodayWorkoutRecommendation().catch(() => null),
          getMyAlarms().catch(() => []),
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

  const exercises = workout?.exercises || [];
  const completedCount = useMemo(
    () => exercises.filter((exercise) => completedExercises[exercise.name]).length,
    [completedExercises, exercises]
  );
  const progressPercent = exercises.length
    ? Math.round((completedCount / exercises.length) * 100)
    : 0;

  const toggleExerciseDone = (name) => {
    setCompletedExercises((prev) => ({ ...prev, [name]: !prev[name] }));
  };

  if (isLoading) {
    return (
      <MobileShell title="특급전사 홈">
        <p className={styles.muted}>로딩 중입니다...</p>
      </MobileShell>
    );
  }

  return (
    <MobileShell
      title="특급전사 홈"
      actions={
        <button className={styles.logout} onClick={actions.logout}>
          로그아웃
        </button>
      }
    >
      <section className={styles.hero}>
        <p className={styles.badge}>오늘의 핵심 관리</p>
        <h2>{nickname}님, 오늘은 운동/식단 두 가지에 집중하세요.</h2>
        <p className={styles.muted}>핵심 기능만 빠르게 확인하도록 메인 화면을 재정리했습니다.</p>
      </section>

      <MainSection title="운동 관리" subtitle="루틴 수행 체크 + 알람 확인">
        {workout ? (
          <>
            <div className={styles.workoutHeader}>
              <p className={styles.item}>
                <strong>{workout.routineType}</strong> · {workout.todayFocus}
              </p>
              <button
                className={styles.startButton}
                type="button"
                onClick={() => setSessionStarted((prev) => !prev)}
              >
                {sessionStarted ? '운동 세션 종료' : '운동 세션 시작'}
              </button>
            </div>

            <div className={styles.progressWrap}>
              <div className={styles.progressBar}>
                <div className={styles.progressFill} style={{ width: `${progressPercent}%` }} />
              </div>
              <p className={styles.progressText}>진행률 {progressPercent}% ({completedCount}/{exercises.length})</p>
            </div>

            <div className={styles.exerciseList}>
              {exercises.map((exercise, idx) => {
                const done = Boolean(completedExercises[exercise.name]);
                return (
                  <article key={exercise.name} className={`${styles.exerciseCard} ${done ? styles.exerciseDone : ''}`}>
                    <div>
                      <p className={styles.exerciseTitle}>{idx + 1}. {exercise.name}</p>
                      <p className={styles.exerciseMeta}>{exercise.sets}세트 · {exercise.reps}</p>
                      {exercise.alternative ? <p className={styles.exerciseAlt}>대체 운동: {exercise.alternative}</p> : null}
                    </div>
                    <button
                      className={done ? styles.doneButton : styles.todoButton}
                      type="button"
                      onClick={() => toggleExerciseDone(exercise.name)}
                    >
                      {done ? '완료' : '수행'}
                    </button>
                  </article>
                );
              })}
            </div>

            <div className={styles.subInfoBox}>
              <p className={styles.subInfoTitle}>다음 운동 알람</p>
              {nextAlarm ? (
                <p className={styles.item}>
                  {String(nextAlarm.hour).padStart(2, '0')}:{String(nextAlarm.minute).padStart(2, '0')} / {formatRepeatDays(nextAlarm.repeatDaysJson)}
                </p>
              ) : (
                <p className={styles.muted}>설정된 운동 알람이 없습니다.</p>
              )}
            </div>
          </>
        ) : (
          <p className={styles.muted}>운동 추천 데이터가 없습니다.</p>
        )}
      </MainSection>

      <MainSection title="식단 관리" subtitle="오늘 식단 + 부족 영양소 점검">
        {hasMealData(meal) ? (
          <>
            <p className={styles.item}><strong>조식</strong> {meal.breakfastRaw || '정보 없음'}</p>
            <p className={styles.item}><strong>중식</strong> {meal.lunchRaw || '정보 없음'}</p>
            <p className={styles.item}><strong>석식</strong> {meal.dinnerRaw || '정보 없음'}</p>
          </>
        ) : (
          <p className={styles.muted}>오늘 식단 정보가 없습니다. 식단 데이터가 아직 준비되지 않았습니다.</p>
        )}

        <div className={styles.subInfoBox}>
          <p className={styles.subInfoTitle}>부족 영양소 / 보충 추천</p>
          {nutrition ? (
            <>
              <p className={styles.item}>목표 칼로리: {nutrition.summary?.targetCalories ?? 0}kcal / 섭취: {nutrition.summary?.intakeCalories ?? 0}kcal / 남음: {nutrition.summary?.remainingCalories ?? 0}kcal</p>
              <p className={styles.item}>단백질 {nutrition.summary?.intakeProteinG ?? 0}g / {nutrition.summary?.targetProteinG ?? 0}g ({nutrition.summary?.proteinProgressPct ?? 0}%) · 부족 {nutrition.summary?.remainingProteinG ?? 0}g</p>
              <p className={styles.item}>탄수화물 {nutrition.summary?.intakeCarbG ?? 0}g / {nutrition.summary?.targetCarbG ?? 0}g ({nutrition.summary?.carbProgressPct ?? 0}%) · 부족 {nutrition.summary?.remainingCarbG ?? 0}g</p>
              <p className={styles.item}>지방 {nutrition.summary?.intakeFatG ?? 0}g / {nutrition.summary?.targetFatG ?? 0}g ({nutrition.summary?.fatProgressPct ?? 0}%) · 부족 {nutrition.summary?.remainingFatG ?? 0}g</p>
              <p className={styles.muted}>{nutrition.summary?.note}</p>
              <p className={styles.muted}>{nutrition.recommendationText}</p>
              <p className={styles.item}><strong>보유 식품 추천:</strong> {(nutrition.ownedFoodSuggestions || []).join(', ') || '없음'}</p>
              <p className={styles.item}><strong>PX 추천:</strong> {(nutrition.pxSuggestions || []).join(', ') || '없음'}</p>
            </>
          ) : (
            <p className={styles.muted}>영양 추천 데이터가 없습니다.</p>
          )}
        </div>
      </MainSection>
    </MobileShell>
  );
}

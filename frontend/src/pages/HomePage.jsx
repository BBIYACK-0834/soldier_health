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

function MainSection({ title, subtitle, children, action }) {
  return (
    <section className={styles.card}>
      <div className={styles.sectionHead}>
        <div>
          <h3>{title}</h3>
          {subtitle ? <p className={styles.subtitle}>{subtitle}</p> : null}
        </div>
        {action}
      </div>
      {children}
    </section>
  );
}

function formatRepeatDays(raw) {
  if (!raw) return '반복 없음';
  return raw
    .split(',')
    .map((day) => toLabel(WEEKDAY_LABELS, day.trim(), day.trim()))
    .join(', ');
}

function toDateKey(date) {
  return date.toISOString().slice(0, 10);
}

function getWeekStart(date) {
  const base = new Date(date);
  const day = base.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  base.setDate(base.getDate() + diff);
  base.setHours(0, 0, 0, 0);
  return base;
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
  const [exerciseGoalDays, setExerciseGoalDays] = useState(3);
  const [weeklyCompletedDays, setWeeklyCompletedDays] = useState([]);
  const [isMealOpen, setIsMealOpen] = useState(false);
  const [isSupplementOpen, setIsSupplementOpen] = useState(false);

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
        setExerciseGoalDays(me.workoutDaysPerWeek || 3);
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

  useEffect(() => {
    const weekStart = toDateKey(getWeekStart(new Date()));
    const key = `workout-week-${weekStart}`;
    const stored = JSON.parse(localStorage.getItem(key) || '[]');
    setWeeklyCompletedDays(stored);
  }, []);

  const exercises = workout?.exercises || [];

  const completedCount = useMemo(
    () => exercises.filter((exercise) => completedExercises[exercise.name]).length,
    [completedExercises, exercises]
  );

  const progressPercent = exercises.length
    ? Math.round((completedCount / exercises.length) * 100)
    : 0;

  useEffect(() => {
    if (!exercises.length || completedCount !== exercises.length) return;
    const todayKey = toDateKey(new Date());
    const weekStart = toDateKey(getWeekStart(new Date()));
    const storageKey = `workout-week-${weekStart}`;
    const stored = JSON.parse(localStorage.getItem(storageKey) || '[]');
    if (stored.includes(todayKey)) return;
    const updated = [...stored, todayKey];
    localStorage.setItem(storageKey, JSON.stringify(updated));
    setWeeklyCompletedDays(updated);
  }, [completedCount, exercises.length]);

  const toggleExerciseDone = (name) => {
    setCompletedExercises((prev) => ({ ...prev, [name]: !prev[name] }));
  };

  const weeklyProgress = weeklyCompletedDays.length;
  const weeklyRatio = Math.min(100, Math.round((weeklyProgress / exerciseGoalDays) * 100));
  const remainingWeeklyGoal = Math.max(exerciseGoalDays - weeklyProgress, 0);
  const todayRemainingCount = Math.max(exercises.length - completedCount, 0);

  const statusGuide =
    todayRemainingCount > 0
      ? `오늘 운동 ${todayRemainingCount}개 남음`
      : remainingWeeklyGoal > 0
        ? `목표까지 ${remainingWeeklyGoal}회 남았습니다`
        : '이번 주 목표를 달성했습니다';

  const proteinGap = nutrition?.summary?.remainingProteinG ?? 0;
  const recommendedSupplement = nutrition?.pxSuggestions?.[0] || '프로틴바';

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
        <p className={styles.heroTitle}>오늘 상태</p>
        <p className={styles.heroSub}>{nickname}님 · 이번 주 목표 진행 중</p>

        <div className={styles.metricRow}>
          <p className={styles.metricMain}>{weeklyProgress}/{exerciseGoalDays}</p>
          <p className={styles.metricLabel}>주간 달성률 {weeklyRatio}%</p>
        </div>

        <div className={styles.progressBar}>
          <div className={styles.progressFill} style={{ width: `${weeklyRatio}%` }} />
        </div>
        <p className={styles.heroHint}>{statusGuide}</p>
      </section>

      <MainSection
        title="운동"
        subtitle="오늘 루틴과 진행 상태"
        action={(
          <button
            className={styles.primaryButton}
            type="button"
            onClick={() => setSessionStarted((prev) => !prev)}
          >
            {sessionStarted ? '운동 종료' : '시작하기'}
          </button>
        )}
      >
        {workout ? (
          <>
            <p className={styles.item}><span className={styles.label}>오늘 루틴</span>{workout.todayFocus || workout.routineType || '루틴 정보 없음'}</p>
            <p className={styles.item}><span className={styles.label}>진행률</span><span className={styles.strongNumber}>{progressPercent}%</span> ({completedCount}/{exercises.length || 0})</p>
            <p className={styles.subText}>{nextAlarm ? `다음 알람 ${String(nextAlarm.hour).padStart(2, '0')}:${String(nextAlarm.minute).padStart(2, '0')} · ${formatRepeatDays(nextAlarm.repeatDaysJson)}` : '설정된 운동 알람이 없습니다'}</p>
            {exercises.length ? (
              <div className={styles.exerciseList}>
                {exercises.slice(0, 3).map((exercise, idx) => {
                  const done = Boolean(completedExercises[exercise.name]);
                  return (
                    <button
                      type="button"
                      key={`${exercise.name}-${idx}`}
                      className={`${styles.exerciseRow} ${done ? styles.exerciseDone : ''}`}
                      onClick={() => toggleExerciseDone(exercise.name)}
                    >
                      <span>{exercise.name} · {exercise.sets}세트</span>
                      <span>{done ? '완료' : '미완료'}</span>
                    </button>
                  );
                })}
              </div>
            ) : null}
          </>
        ) : (
          <p className={styles.muted}>오늘 운동 데이터가 없습니다.</p>
        )}
      </MainSection>

      <MainSection
        title="식단"
        subtitle="오늘 영양 균형"
        action={(
          <button className={styles.secondaryButton} type="button" onClick={() => setIsMealOpen((prev) => !prev)}>
            식단 보기
          </button>
        )}
      >
        <p className={styles.item}><span className={styles.label}>단백질 부족</span><span className={styles.strongNumber}>{proteinGap}g</span></p>
        <p className={styles.item}><span className={styles.label}>상태</span>{proteinGap > 0 ? '영양 균형 확인 필요' : '현재 균형 양호'}</p>
        {isMealOpen ? (
          <div className={styles.subInfoBox}>
            <p className={styles.subInfoTitle}>오늘 식단</p>
            <p className={styles.subText}>조식: {meal?.breakfastRaw || '정보 없음'}</p>
            <p className={styles.subText}>중식: {meal?.lunchRaw || '정보 없음'}</p>
            <p className={styles.subText}>석식: {meal?.dinnerRaw || '정보 없음'}</p>
          </div>
        ) : null}
      </MainSection>

      <MainSection
        title="추천 보충"
        subtitle="부족 영양 기반 추천"
        action={(
          <button className={styles.secondaryButton} type="button" onClick={() => setIsSupplementOpen((prev) => !prev)}>
            확인하기
          </button>
        )}
      >
        <p className={styles.item}><span className={styles.label}>현재 상태</span>{proteinGap > 0 ? '단백질 보충 필요' : '유지 섭취 권장'}</p>
        <p className={styles.item}><span className={styles.label}>추천</span>{recommendedSupplement}</p>
        {isSupplementOpen ? (
          <div className={styles.subInfoBox}>
            <p className={styles.subText}>{nutrition?.recommendationText || '추천 정보가 없습니다.'}</p>
            <button type="button" className={styles.inlineLink} onClick={() => navigate('/community')}>
              다른 전우 추천 보기
            </button>
          </div>
        ) : null}
      </MainSection>
    </MobileShell>
  );
}

import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MobileShell from '../components/layout/MobileShell';
import { useAppContext } from '../app/AppContext';
import { getMe } from '../api/authApi';
import { getMealByDate, getTodayMeal } from '../api/mealApi';
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

function toDateKey(date) {
  return date.toISOString().slice(0, 10);
}

function toKoreanDate(date) {
  return `${date.getMonth() + 1}월 ${date.getDate()}일`;
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
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [exerciseGoalDays, setExerciseGoalDays] = useState(3);
  const [weeklyCompletedDays, setWeeklyCompletedDays] = useState([]);
  const [editableExercises, setEditableExercises] = useState([]);
  const [isEditMode, setIsEditMode] = useState(false);

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
        setEditableExercises(workoutData?.exercises || []);

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
    if (!selectedDate) return;
    (async () => {
      const date = toDateKey(selectedDate);
      const mealData = await getMealByDate(date).catch(() => null);
      setMeal(mealData);
    })();
  }, [selectedDate]);

  useEffect(() => {
    const weekStart = toDateKey(getWeekStart(new Date()));
    const key = `workout-week-${weekStart}`;
    const stored = JSON.parse(localStorage.getItem(key) || '[]');
    setWeeklyCompletedDays(stored);
  }, []);

  const exercises = editableExercises;

  useEffect(() => {
    const todayKey = toDateKey(new Date());
    const stored = JSON.parse(localStorage.getItem(`custom-routine-${todayKey}`) || '[]');
    if (stored.length) {
      setEditableExercises(stored);
    }
  }, []);

  useEffect(() => {
    const todayKey = toDateKey(new Date());
    if (!editableExercises.length) return;
    localStorage.setItem(`custom-routine-${todayKey}`, JSON.stringify(editableExercises));
  }, [editableExercises]);

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
  const isGoalMet = weeklyProgress >= exerciseGoalDays;
  const streakHearts = Array.from({ length: exerciseGoalDays }, (_, idx) => idx < weeklyProgress);


  const updateExercise = (index, key, value) => {
    setEditableExercises((prev) => prev.map((exercise, idx) => (idx === index ? { ...exercise, [key]: value } : exercise)));
  };

  const addExercise = () => {
    setEditableExercises((prev) => [...prev, { name: '새 운동', sets: 3, reps: '10-12회', alternative: '' }]);
  };

  const deleteExercise = (index) => {
    setEditableExercises((prev) => prev.filter((_, idx) => idx !== index));
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
      actions={(
        <div className={styles.headerActions}>
          <button className={styles.profileButton} onClick={() => navigate('/community')}>
            커뮤니티
          </button>
          <button className={styles.profileButton} onClick={() => navigate('/profile')}>
            프로필 수정
          </button>
          <button className={styles.logout} onClick={actions.logout}>
            로그아웃
          </button>
        </div>
      )}
    >
      <section className={styles.hero}>
        <p className={styles.badge}>오늘의 성취 대시보드</p>
        <h2>{nickname}님, 이번 주 작전 목표를 채워볼까요?</h2>
        <div className={isGoalMet ? styles.goalSuccess : styles.goalDanger}>
          <p className={styles.goalTitle}>
            주간 운동 달성 {weeklyProgress}/{exerciseGoalDays}일
          </p>
          <p className={styles.goalHint}>
            {isGoalMet
              ? '목표 달성! 오늘은 가벼운 스트레칭으로 컨디션을 챙겨요 ✨'
              : '아직 목표 미달성! 하트를 채우면 귀여운 메달이 완성돼요 💖'}
          </p>
          <div className={styles.heartRow}>
            {streakHearts.map((filled, idx) => (
              <span key={`heart-${idx}`} className={filled ? styles.heartOn : styles.heartOff}>
                {filled ? '💖' : '🤍'}
              </span>
            ))}
            {isGoalMet ? <span className={styles.medal}>🏅</span> : null}
          </div>
        </div>
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
              <button
                className={styles.dateButton}
                type="button"
                onClick={() => setIsEditMode((prev) => !prev)}
              >
                {isEditMode ? '편집 완료' : '루틴 편집'}
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
                  <article key={`${exercise.name}-${idx}`} className={`${styles.exerciseCard} ${done ? styles.exerciseDone : ''}`}>
                    <div>
                      {isEditMode ? (
                        <>
                          <input className={styles.inlineInput} value={exercise.name} onChange={(e) => updateExercise(idx, 'name', e.target.value)} />
                          <div className={styles.inlineRow}>
                            <input className={styles.inlineInputSmall} value={exercise.sets} onChange={(e) => updateExercise(idx, 'sets', e.target.value)} />
                            <input className={styles.inlineInput} value={exercise.reps} onChange={(e) => updateExercise(idx, 'reps', e.target.value)} />
                          </div>
                          <input className={styles.inlineInput} value={exercise.alternative || ''} onChange={(e) => updateExercise(idx, 'alternative', e.target.value)} placeholder="대체 운동" />
                        </>
                      ) : (
                        <>
                          <p className={styles.exerciseTitle}>{idx + 1}. {exercise.name}</p>
                          <p className={styles.exerciseMeta}>{exercise.sets}세트 · {exercise.reps}</p>
                          {exercise.alternative ? <p className={styles.exerciseAlt}>대체 운동: {exercise.alternative}</p> : null}
                        </>
                      )}
                    </div>
                    {isEditMode ? (
                      <button className={styles.todoButton} type="button" onClick={() => deleteExercise(idx)}>삭제</button>
                    ) : (
                      <button
                        className={done ? styles.doneButton : styles.todoButton}
                        type="button"
                        onClick={() => toggleExerciseDone(exercise.name)}
                      >
                        {done ? '완료' : '수행'}
                      </button>
                    )}
                  </article>
                );
              })}
              {isEditMode ? <button className={styles.startButton} type="button" onClick={addExercise}>운동 추가</button> : null}
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

      <MainSection title="식단 관리" subtitle="날짜별 식단 확인 + 오늘 영양소 점검">
        <div className={styles.datePager}>
          <button className={styles.dateButton} type="button" onClick={() => setSelectedDate((prev) => new Date(prev.getFullYear(), prev.getMonth(), prev.getDate() - 1))}>
            이전 날
          </button>
          <p className={styles.dateLabel}>{toKoreanDate(selectedDate)}</p>
          <button className={styles.dateButton} type="button" onClick={() => setSelectedDate((prev) => new Date(prev.getFullYear(), prev.getMonth(), prev.getDate() + 1))}>
            다음 날
          </button>
        </div>

        {hasMealData(meal) ? (
          <>
            <p className={styles.item}><strong>조식</strong> {meal.breakfastRaw || '정보 없음'}</p>
            <p className={styles.item}><strong>중식</strong> {meal.lunchRaw || '정보 없음'}</p>
            <p className={styles.item}><strong>석식</strong> {meal.dinnerRaw || '정보 없음'}</p>
          </>
        ) : (
          <p className={styles.muted}>선택한 날짜의 식단 정보가 없습니다.</p>
        )}

        <div className={styles.subInfoBox}>
          <p className={styles.subInfoTitle}>오늘 부족 영양소 / 보충 추천</p>
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

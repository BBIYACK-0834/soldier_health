import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import ProgressBar from '../../components/ui/ProgressBar';
import { getTodayNutrition } from '../../api/nutritionApi';
import { emptyDashboardSummary, emptyUser } from '../../constants/defaultData';
import { getWeeklyWorkoutSummary } from '../../utils/workoutStorage';
import { useAppContext } from '../../app/AppContext';
import styles from './HomePage.module.css';

export default function HomePage() {
  const navigate = useNavigate();
  const { state } = useAppContext();
  const [summary, setSummary] = useState(emptyDashboardSummary);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [localWeeklyExercise, setLocalWeeklyExercise] = useState(() => getWeeklyWorkoutSummary());

  useEffect(() => {
    let mounted = true;
    async function load() {
      try {
        setLoading(true);
        const data = await getTodayNutrition();
        if (!mounted) return;
        setSummary(data ? { ...emptyDashboardSummary, ...data } : emptyDashboardSummary);
      } catch (error) {
        if (!mounted) return;
        setSummary(emptyDashboardSummary);
        setErrorMessage('건강 데이터를 불러오지 못했습니다. 연결 전에는 모든 수치를 0으로 표시합니다.');
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
    const refreshWorkoutSummary = () => setLocalWeeklyExercise(getWeeklyWorkoutSummary());
    window.addEventListener('focus', refreshWorkoutSummary);
    window.addEventListener('tg-workout-progress-updated', refreshWorkoutSummary);
    return () => {
      window.removeEventListener('focus', refreshWorkoutSummary);
      window.removeEventListener('tg-workout-progress-updated', refreshWorkoutSummary);
    };
  }, []);

  const intakeCalories = Number.isFinite(summary?.intakeCalories) ? summary.intakeCalories : emptyDashboardSummary.intakeCalories;
  const targetCalories = Number.isFinite(summary?.targetCalories) ? summary.targetCalories : emptyDashboardSummary.targetCalories;
  const apiWeeklyExercise = summary?.weeklyExercise ?? emptyDashboardSummary.weeklyExercise;
  const weeklyExercise = {
    completed: Math.max(apiWeeklyExercise.completed ?? 0, localWeeklyExercise.completed),
    target: apiWeeklyExercise.target || localWeeklyExercise.target || 4,
    completedDates: localWeeklyExercise.completedDates ?? [],
  };

  const grassDays = useMemo(() => {
    const completedDateSet = new Set(weeklyExercise.completedDates);
    const today = new Date();
    return Array.from({ length: 35 }, (_, index) => {
      const date = new Date(today);
      date.setHours(0, 0, 0, 0);
      date.setDate(today.getDate() - (34 - index));
      const dateKey = date.toISOString().slice(0, 10);
      return { dateKey, completed: completedDateSet.has(dateKey) };
    });
  }, [weeklyExercise.completedDates]);

  const macroData = useMemo(
    () => [
      { label: '탄수화물', intake: summary?.intakeCarbG ?? 0, target: summary?.targetCarbG ?? 0, color: '#4f739e', tone: styles.carbCard },
      { label: '단백질', intake: summary?.intakeProteinG ?? 0, target: summary?.targetProteinG ?? 0, color: '#4f713b', tone: styles.proteinCard },
      { label: '지방', intake: summary?.intakeFatG ?? 0, target: summary?.targetFatG ?? 0, color: '#c27b18', tone: styles.fatCard },
    ],
    [summary]
  );

  const content = loading ? (
    <Card className={styles.loadingCard}>
      <p className={styles.meta}>데이터를 동기화하는 중...</p>
    </Card>
  ) : (
    <>
      <Card className={styles.calorieCard}>
        <p className={styles.sectionLabel}>필요 / 추정 섭취 칼로리</p>
        <div className={styles.calorieValue}>
          <strong>{targetCalories.toLocaleString()}</strong>
          <span>/ {intakeCalories.toLocaleString()} kcal</span>
        </div>
        <ProgressBar value={intakeCalories} max={targetCalories || 1} />
        <div className={styles.macroGrid}>
          {macroData.map((macro) => (
            <div key={macro.label} className={`${styles.macroCard} ${macro.tone}`}>
              <p style={{ color: macro.color }}>{macro.label}</p>
              <strong>{macro.target}g /<br />{macro.intake}g</strong>
              <ProgressBar value={macro.intake} max={macro.target || 1} color={macro.color} />
            </div>
          ))}
        </div>
      </Card>

      <Card className={styles.workoutCard}>
        <div className={styles.rowBetween}>
          <h3 className={styles.cardTitle}>이번 주 운동</h3>
          <strong>{weeklyExercise.completed} / {weeklyExercise.target}회</strong>
        </div>
        <div className={styles.grassGrid} aria-label="운동 현황판">
          {grassDays.map((day) => (
            <span key={day.dateKey} className={day.completed ? styles.grassDone : styles.grassEmpty} title={day.dateKey} />
          ))}
        </div>
        <p className={styles.grassHint}>하루 운동을 완료할 때마다 한 칸씩 채워집니다.</p>
        <button type="button" className={styles.primaryButton} onClick={() => navigate('/exercise')}>운동 기록하기</button>
      </Card>

      {errorMessage ? <small className={styles.meta}>{errorMessage}</small> : null}
    </>
  );

  return (
    <AppLayout>
      <header className={styles.heroHeader}>
        <div>
          <h1>{(state.user?.nickname || emptyUser.nickname)}님, 오늘도 파이팅입니다.</h1>
          <p>오늘의 건강 상태를 한눈에 확인하세요.</p>
        </div>
        <button type="button" className={styles.iconBtn} onClick={() => navigate('/mypage/notifications')} aria-label="알림 설정">🔔</button>
      </header>
      {content}
    </AppLayout>
  );
}

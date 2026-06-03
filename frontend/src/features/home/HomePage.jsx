import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import ProgressBar from '../../components/ui/ProgressBar';
import MacroBox from '../../components/ui/MacroBox';
import WorkoutCheckCircle from '../../components/ui/WorkoutCheckCircle';
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
    target: apiWeeklyExercise.target || localWeeklyExercise.target,
  };

  const macroData = useMemo(
    () => [
      { label: '탄수화물', intake: summary?.intakeCarbG ?? 0, target: summary?.targetCarbG ?? 0, color: '#50739a', tone: '#dfe5ef' },
      { label: '단백질', intake: summary?.intakeProteinG ?? 0, target: summary?.targetProteinG ?? 0, color: '#6f8f55', tone: '#e4e9de' },
      { label: '지방', intake: summary?.intakeFatG ?? 0, target: summary?.targetFatG ?? 0, color: '#d28a2c', tone: '#efe2cf' },
    ],
    [summary]
  );

  return (
    <AppLayout
      title={`${(state.user?.nickname || emptyUser.nickname)}님, 오늘도 파이팅입니다.`}
      subtitle="오늘의 건강 상태를 한눈에 확인하세요."
      headerAction={<button type="button" className={styles.iconBtn} onClick={() => navigate('/mypage/notifications')}>🔔</button>}
    >
      <Card className={styles.mainDietCard}>
        <div className={styles.rowBetween}>
          <span className={styles.pill}>오늘의 식단</span>
          <span className={styles.percent}>{targetCalories > 0 ? Math.round((intakeCalories / targetCalories) * 100) : 0}%</span>
        </div>
        <p className={styles.title}>총 섭취 칼로리</p>
        <p className={styles.kcal}>{intakeCalories.toLocaleString()} <span>/ {targetCalories.toLocaleString()} kcal</span></p>
        <ProgressBar value={intakeCalories} max={targetCalories || 1} />
        <div className={styles.macroGrid}>
          {macroData.map((macro) => (
            <MacroBox key={macro.label} label={macro.label} intake={macro.intake} target={macro.target} color={macro.color} tone={macro.tone} />
          ))}
        </div>
      </Card>

      <Card>
        <div className={styles.rowBetween}>
          <h3 className={styles.cardTitle}>이번 주 운동</h3>
          <strong>{weeklyExercise.completed} / {weeklyExercise.target}회</strong>
        </div>
        <div className={styles.checkGrid}>
          {[0, 1, 2, 3].map((index) => (
            <WorkoutCheckCircle key={index} checked={index < weeklyExercise.completed} label={`${index + 1}회`} />
          ))}
        </div>
        <button type="button" className={styles.smallBtn} onClick={() => navigate('/exercise')}>운동 기록하기</button>
      </Card>

      <Card className={styles.tipCard}>
        <div className={styles.avatar}>🪖</div>
        <div>
          <span className={styles.tipPill}>D-0 전역</span>
          <p>{summary?.recommendation || '아직 추천 데이터가 없습니다. 식단과 운동을 기록하면 맞춤 팁이 표시됩니다.'}</p>
        </div>
      </Card>

      <div className={styles.quickGrid}>
        <button type="button" onClick={() => navigate('/diet/add')}>+ 식단 추가</button>
        <button type="button" onClick={() => navigate('/exercise/add/equipment')}>+ 운동 추가</button>
      </div>
      {loading ? <small className={styles.meta}>데이터를 동기화하는 중...</small> : null}
      {errorMessage ? <small className={styles.meta}>{errorMessage}</small> : null}
    </AppLayout>
  );
}

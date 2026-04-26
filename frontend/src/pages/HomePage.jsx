import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import ProgressBar from '../components/ui/ProgressBar';
import MacroBox from '../components/ui/MacroBox';
import { getTodayNutrition } from '../api/nutritionApi';
import styles from '../features/design/HomePage.module.css';

export default function HomePage() {
  const navigate = useNavigate();
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    let mounted = true;
    async function load() {
      try {
        setLoading(true);
        const data = await getTodayNutrition();
        if (!mounted) return;
        setSummary(data ?? null);
      } catch (error) {
        if (!mounted) return;
        setErrorMessage(error.message || '영양 정보를 불러오지 못했습니다.');
      } finally {
        if (mounted) setLoading(false);
      }
    }

    load();
    return () => {
      mounted = false;
    };
  }, []);

  const intakeCalories = Number.isFinite(summary?.intakeCalories) ? summary.intakeCalories : null;
  const targetCalories = Number.isFinite(summary?.targetCalories) ? summary.targetCalories : null;
  const remainingCalories = Number.isFinite(summary?.remainingCalories) ? summary.remainingCalories : null;

  const macroData = useMemo(
    () => [
      { label: '탄수화물', intake: summary?.intakeCarbG ?? 0, target: summary?.targetCarbG ?? 0, color: '#50739a', tone: '#dfe5ef' },
      { label: '단백질', intake: summary?.intakeProteinG ?? 0, target: summary?.targetProteinG ?? 0, color: '#6f8f55', tone: '#e4e9de' },
      { label: '지방', intake: summary?.intakeFatG ?? 0, target: summary?.targetFatG ?? 0, color: '#d28a2c', tone: '#efe2cf' },
    ],
    [summary]
  );

  return (
    <AppLayout title="홈" subtitle="DB/API 기반 오늘 데이터를 보여드립니다." headerAction={<span className={styles.bell}>🔔</span>}>
      <Card className={styles.mainDietCard}>
        <div className={styles.rowBetween}><span className={styles.pill}>오늘의 식단</span></div>
        <p className={styles.title}>총 섭취 칼로리</p>
        <p className={styles.kcal}>{intakeCalories == null ? '-' : intakeCalories.toLocaleString()} <span>{targetCalories == null ? '' : `/ ${targetCalories.toLocaleString()} kcal`}</span></p>
        <ProgressBar value={intakeCalories ?? 0} max={targetCalories || 1} />
        <div className={styles.rowBetween}><p className={styles.leftCal}>{remainingCalories == null ? '아직 수집된 식단이 없습니다.' : `남은 칼로리 ${remainingCalories.toLocaleString()} kcal`}</p><button type="button" className={styles.smallBtn} onClick={() => navigate('/diet')}>식단 보기</button></div>
        <div className={styles.macroGrid}>
          {macroData.map((macro) => (
            <MacroBox key={macro.label} label={macro.label} intake={macro.intake} target={macro.target} color={macro.color} tone={macro.tone} />
          ))}
        </div>
        {loading ? <small>불러오는 중...</small> : null}
        {errorMessage ? <small>{errorMessage}</small> : null}
      </Card>

      <Card>
        <div className={styles.rowBetween}><h3>운동</h3><button type="button" className={styles.smallBtn} onClick={() => navigate('/workout')}>운동 보기</button></div>
        <p className={styles.meta}>운동 데이터는 추천 API 결과 또는 사용자 입력 기반으로 표시됩니다.</p>
      </Card>
    </AppLayout>
  );
}

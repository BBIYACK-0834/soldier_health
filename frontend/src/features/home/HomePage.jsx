import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import ProgressBar from '../../components/ui/ProgressBar';
import MacroBox from '../../components/ui/MacroBox';
import WorkoutCheckCircle from '../../components/ui/WorkoutCheckCircle';
import { getTodayNutrition } from '../../api/nutritionApi';
import { getMyProfile } from '../../api/userApi';
import { mockDashboardSummary, mockUser } from '../../constants/mockData';
import { calculateMilitaryService } from '../../utils/militaryService';
import styles from './HomePage.module.css';

export default function HomePage() {
  const navigate = useNavigate();
  const [summary, setSummary] = useState(mockDashboardSummary);
  const [profile, setProfile] = useState(mockUser);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    let mounted = true;
    async function load() {
      try {
        setLoading(true);
        const [data, profileData] = await Promise.all([getTodayNutrition(), getMyProfile()]);
        if (!mounted) return;
        setSummary(data ? { ...mockDashboardSummary, ...data } : mockDashboardSummary);
        setProfile(profileData ? { ...mockUser, ...profileData } : mockUser);
      } catch (error) {
        if (!mounted) return;
        setSummary(mockDashboardSummary);
        setProfile(mockUser);
        setErrorMessage('서버 연결 전이라 예시 데이터로 홈을 표시합니다.');
      } finally {
        if (mounted) setLoading(false);
      }
    }

    load();
    return () => {
      mounted = false;
    };
  }, []);

  const intakeCalories = Number.isFinite(summary?.intakeCalories) ? summary.intakeCalories : mockDashboardSummary.intakeCalories;
  const targetCalories = Number.isFinite(summary?.targetCalories) ? summary.targetCalories : mockDashboardSummary.targetCalories;
  const weeklyExercise = summary?.weeklyExercise ?? mockDashboardSummary.weeklyExercise;
  const serviceInfo = calculateMilitaryService(profile?.enlistmentDate) ?? profile;

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
      title={`${profile?.nickname || mockUser.nickname}님, 오늘도 파이팅입니다.`}
      subtitle="오늘의 건강 상태를 한눈에 확인하세요."
      headerAction={<button type="button" className={styles.iconBtn} onClick={() => navigate('/mypage/notifications')}>🔔</button>}
    >
      <Card className={styles.mainDietCard}>
        <div className={styles.rowBetween}>
          <span className={styles.pill}>오늘의 식단</span>
          <span className={styles.percent}>{Math.round((intakeCalories / targetCalories) * 100)}%</span>
        </div>
        <p className={styles.title}>총 섭취 칼로리</p>
        <p className={styles.kcal}>{intakeCalories.toLocaleString()} <span>/ {targetCalories.toLocaleString()} kcal</span></p>
        <ProgressBar value={intakeCalories} max={targetCalories} />
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
          <span className={styles.tipPill}>{serviceInfo?.daysUntilDischarge != null ? `D-${serviceInfo.daysUntilDischarge} 전역` : '입대일 설정 필요'}</span>
          <p>{summary?.recommendation || mockDashboardSummary.recommendation}</p>
        </div>
      </Card>

      {loading ? <small className={styles.meta}>데이터를 동기화하는 중...</small> : null}
      {errorMessage ? <small className={styles.meta}>{errorMessage}</small> : null}
    </AppLayout>
  );
}

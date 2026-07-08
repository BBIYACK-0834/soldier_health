import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { getMyProfile, updateGoals } from '../../api/userApi';
import { emptyUser } from '../../constants/defaultData';
import screen from '../../components/ui/Screen.module.css';
import styles from './MyPage.module.css';

const goals = [
  { value: 'BULK', label: '벌크업' },
  { value: 'CUT', label: '다이어트' },
  { value: 'FITNESS_TEST', label: '특급전사' },
];

const levels = [
  { value: 'BEGINNER', label: '입문' },
  { value: 'NOVICE', label: '초급' },
  { value: 'INTERMEDIATE', label: '중급' },
];


export default function GoalSettingsPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    goalType: 'BULK',
    workoutLevel: 'BEGINNER',
    targetWeight: '',
    workoutDaysPerWeek: 5,
    preferredWorkoutMinutes: 60,
    branchType: 'ARMY',
  });
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    let mounted = true;
    getMyProfile()
      .then((profile) => {
        if (!mounted) return;
        const next = { ...emptyUser, ...profile };
        setForm((prev) => ({
          ...prev,
          goalType: next.goalType || prev.goalType,
          workoutLevel: next.workoutLevel || prev.workoutLevel,
          targetWeight: next.targetWeight || '',
          workoutDaysPerWeek: next.workoutDaysPerWeek || prev.workoutDaysPerWeek,
          preferredWorkoutMinutes: next.preferredWorkoutMinutes || prev.preferredWorkoutMinutes,
          branchType: next.branchType || prev.branchType,
        }));
      })
      .catch(() => setMessage('기존 목표를 불러오지 못했습니다. 다시 저장하면 새 설정으로 반영됩니다.'))
      .finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, []);

  const setField = (key, value) => setForm((prev) => ({ ...prev, [key]: value }));

  const handleSubmit = async (event) => {
    event.preventDefault();
    setMessage('');

    if (!Number(form.targetWeight) || Number(form.targetWeight) <= 0) {
      setMessage('목표 체중을 입력해야 홈/식단 영양 목표가 계산됩니다.');
      return;
    }

    try {
      setSubmitting(true);
      await updateGoals({
        goalType: form.goalType,
        workoutLevel: form.workoutLevel,
        targetWeight: Number(form.targetWeight),
        workoutDaysPerWeek: Number(form.workoutDaysPerWeek),
        preferredWorkoutMinutes: Number(form.preferredWorkoutMinutes),
        branchType: form.branchType,
      });
      setMessage('목표 설정이 저장되었습니다.');
      setTimeout(() => navigate('/mypage'), 350);
    } catch (error) {
      setMessage(error.message || '목표 설정을 저장하지 못했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AppLayout title="목표 설정" subtitle="운동 목적과 목표 체중을 저장하면 홈/식단 기준 영양소가 맞춰집니다." showBottomNav={false}>
      <Card>
        <form className={styles.form} onSubmit={handleSubmit}>
          {loading ? <p className={screen.muted}>목표 설정을 불러오는 중...</p> : null}
          <label>목표 체중(kg)<input className={screen.input} type="number" min="30" max="160" step="0.1" value={form.targetWeight} onChange={(event) => setField('targetWeight', event.target.value)} required /></label>
          <label>주 운동 횟수<input className={screen.input} type="number" min="1" max="7" value={form.workoutDaysPerWeek} onChange={(event) => setField('workoutDaysPerWeek', event.target.value)} required /></label>
          <label>선호 운동 시간(분)<input className={screen.input} type="number" min="10" max="180" value={form.preferredWorkoutMinutes} onChange={(event) => setField('preferredWorkoutMinutes', event.target.value)} required /></label>
          <div className={styles.optionGrid}>
            {goals.map((goal) => <button key={goal.value} type="button" className={form.goalType === goal.value ? styles.active : ''} onClick={() => setField('goalType', goal.value)}>{goal.label}</button>)}
          </div>
          <div className={styles.optionGrid}>
            {levels.map((level) => <button key={level.value} type="button" className={form.workoutLevel === level.value ? styles.active : ''} onClick={() => setField('workoutLevel', level.value)}>{level.label}</button>)}
          </div>
          <p className={styles.autoRecommendationNote}>
            저장하면 선택한 목표·숙련도·주 운동 횟수에 맞춰 오늘 운동 루틴이 자동으로 추천됩니다.
          </p>
          {message ? <p className={styles.muted}>{message}</p> : null}
          <button type="submit" className={screen.primaryButton} disabled={submitting}>{submitting ? '저장 중...' : '저장하기'}</button>
        </form>
      </Card>
    </AppLayout>
  );
}

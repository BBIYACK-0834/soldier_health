import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MobileShell from '../components/layout/MobileShell';
import { getMe } from '../api/authApi';
import { updateGoals, updateProfile } from '../api/userApi';
import {
  GOAL_TYPE_LABELS,
  WORKOUT_LEVEL_LABELS,
  BRANCH_TYPE_LABELS,
  toLabel,
} from '../constants/labels';
import styles from '../features/onboarding/OnboardingForm.module.css';

export default function ProfilePage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    heightCm: 0,
    weightKg: 0,
    goalType: 'GENERAL_FITNESS',
    workoutLevel: 'BEGINNER',
    workoutDaysPerWeek: 3,
    preferredWorkoutMinutes: 50,
    branchType: 'ARMY',
  });
  const [loading, setLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [notice, setNotice] = useState('');

  const goalOptions = useMemo(() => Object.keys(GOAL_TYPE_LABELS), []);
  const levelOptions = useMemo(() => Object.keys(WORKOUT_LEVEL_LABELS), []);
  const branchOptions = useMemo(() => Object.keys(BRANCH_TYPE_LABELS), []);

  useEffect(() => {
    (async () => {
      try {
        const me = await getMe();
        setForm({
          heightCm: me.heightCm ?? 175,
          weightKg: me.weightKg ?? 70,
          goalType: me.goalType || 'GENERAL_FITNESS',
          workoutLevel: me.workoutLevel || 'BEGINNER',
          workoutDaysPerWeek: me.workoutDaysPerWeek ?? 3,
          preferredWorkoutMinutes: me.preferredWorkoutMinutes ?? 50,
          branchType: me.branchType || 'ARMY',
        });
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const onSave = async (e) => {
    e.preventDefault();
    setIsSaving(true);
    setNotice('');
    try {
      await updateProfile({
        heightCm: Number(form.heightCm),
        weightKg: Number(form.weightKg),
      });

      await updateGoals({
        goalType: form.goalType,
        workoutLevel: form.workoutLevel,
        branchType: form.branchType,
        workoutDaysPerWeek: Number(form.workoutDaysPerWeek),
        preferredWorkoutMinutes: Number(form.preferredWorkoutMinutes),
      });

      setNotice('프로필이 저장되었습니다. 홈에서 바로 반영됩니다.');
    } finally {
      setIsSaving(false);
    }
  };

  if (loading) {
    return (
      <MobileShell title="내 프로필 수정">
        <p className={styles.info}>프로필을 불러오는 중입니다.</p>
      </MobileShell>
    );
  }

  return (
    <MobileShell
      title="내 프로필 수정"
      actions={(
        <button className={styles.chip} type="button" onClick={() => navigate('/', { replace: true })}>
          홈으로
        </button>
      )}
    >
      <form className={styles.section} onSubmit={onSave}>
        <h2 className={styles.title}>신체 정보</h2>
        <div className={styles.row}>
          <input
            className={styles.input}
            type="number"
            value={form.heightCm}
            onChange={(e) => setForm((prev) => ({ ...prev, heightCm: e.target.value }))}
            placeholder="키(cm)"
          />
          <input
            className={styles.input}
            type="number"
            value={form.weightKg}
            onChange={(e) => setForm((prev) => ({ ...prev, weightKg: e.target.value }))}
            placeholder="몸무게(kg)"
          />
        </div>

        <h2 className={styles.title}>운동 목표</h2>
        <div className={styles.row}>
          <select
            className={styles.select}
            value={form.goalType}
            onChange={(e) => setForm((prev) => ({ ...prev, goalType: e.target.value }))}
          >
            {goalOptions.map((goal) => (
              <option key={goal} value={goal}>
                {toLabel(GOAL_TYPE_LABELS, goal)}
              </option>
            ))}
          </select>
          <select
            className={styles.select}
            value={form.workoutLevel}
            onChange={(e) => setForm((prev) => ({ ...prev, workoutLevel: e.target.value }))}
          >
            {levelOptions.map((level) => (
              <option key={level} value={level}>
                {toLabel(WORKOUT_LEVEL_LABELS, level)}
              </option>
            ))}
          </select>
        </div>

        <div className={styles.row}>
          <input
            className={styles.input}
            type="number"
            min="1"
            max="7"
            value={form.workoutDaysPerWeek}
            onChange={(e) => setForm((prev) => ({ ...prev, workoutDaysPerWeek: e.target.value }))}
            placeholder="주당 운동 횟수"
          />
          <input
            className={styles.input}
            type="number"
            min="10"
            max="180"
            value={form.preferredWorkoutMinutes}
            onChange={(e) => setForm((prev) => ({ ...prev, preferredWorkoutMinutes: e.target.value }))}
            placeholder="운동 시간(분)"
          />
        </div>

        <h2 className={styles.title}>군 분류</h2>
        <select
          className={styles.select}
          value={form.branchType}
          onChange={(e) => setForm((prev) => ({ ...prev, branchType: e.target.value }))}
        >
          {branchOptions.map((branch) => (
            <option key={branch} value={branch}>
              {toLabel(BRANCH_TYPE_LABELS, branch)}
            </option>
          ))}
        </select>

        <button className={styles.submit} type="submit" disabled={isSaving}>
          {isSaving ? '저장 중입니다...' : '프로필 저장'}
        </button>

        {notice ? <p className={styles.info}>{notice}</p> : null}
      </form>
    </MobileShell>
  );
}

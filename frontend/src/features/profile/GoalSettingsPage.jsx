import { useState } from 'react';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { emptyUser } from '../../constants/defaultData';
import screen from '../../components/ui/Screen.module.css';
import styles from './MyPage.module.css';

const goals = [
  { value: 'diet', label: '다이어트' },
  { value: 'bulkUp', label: '벌크업' },
  { value: 'health', label: '건강 관리' },
];

export default function GoalSettingsPage() {
  const [goalType, setGoalType] = useState(emptyUser.goalType);
  return (
    <AppLayout title="목표 설정" showBottomNav={false}>
      <Card>
        <div className={styles.form}>
          <label>목표 체중<input className={screen.input} type="number" defaultValue={emptyUser.targetWeight} /></label>
          <label>목표 기간<input className={screen.input} type="date" defaultValue={emptyUser.targetDate} /></label>
          <div className={styles.optionGrid}>
            {goals.map((goal) => <button key={goal.value} type="button" className={goalType === goal.value ? styles.active : ''} onClick={() => setGoalType(goal.value)}>{goal.label}</button>)}
          </div>
          <button type="button" className={screen.primaryButton}>저장하기</button>
        </div>
      </Card>
    </AppLayout>
  );
}

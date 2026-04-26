import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import styles from '../features/design/SetupPage.module.css';

const goals = ['특급전사', '다이어트', '벌크업', '린매스업', '건강 관리'];
const levels = ['운동 초보', '초급', '중급', '고급'];

export default function ProfileSetupPage() {
  const navigate = useNavigate();
  const [goal, setGoal] = useState('');
  const [level, setLevel] = useState('');

  return (
    <AppLayout title="나의 상태 설정" subtitle="현재 상태를 입력해주세요." showBottomNav={false}>
      <Card>
        <h3>1. 키와 몸무게</h3>
        <div className={styles.inlineTwo}><input placeholder="키(cm)" /><input placeholder="몸무게(kg)" /></div>
      </Card>
      <Card>
        <h3>2. 목표</h3>
        <div className={styles.chips}>
          {goals.map((item) => <button key={item} type="button" className={goal === item ? styles.activeChip : ''} onClick={() => setGoal(item)}>{item}</button>)}
        </div>
      </Card>
      <Card>
        <h3>3. 운동 경력</h3>
        <div className={styles.chips}>
          {levels.map((item) => <button key={item} type="button" className={level === item ? styles.activeChip : ''} onClick={() => setLevel(item)}>{item}</button>)}
        </div>
        <h3>4. 주당 운동 빈도</h3>
        <select className={styles.select}><option>선택 안 함</option><option>주 4~5회 / 1회 60분</option><option>주 2~3회 / 1회 40분</option></select>
      </Card>
      <button type="button" className={styles.primary} onClick={() => navigate('/home')}>설정 완료</button>
    </AppLayout>
  );
}

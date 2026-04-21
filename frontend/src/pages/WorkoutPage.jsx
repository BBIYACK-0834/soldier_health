import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import styles from '../features/design/WorkoutPage.module.css';

const routines = [
  { name: 'PT 체조', detail: '20분 · 150 kcal' },
  { name: '근력 운동 (팔굽혀펴기)', detail: '3세트 · 90 kcal' },
  { name: '러닝', detail: '30분 · 180 kcal' },
];

export default function WorkoutPage() {
  const navigate = useNavigate();
  return (
    <AppLayout title="운동 기록" subtitle="2024.06.01 (토)" headerAction={<span className={styles.calendar}>🗓️</span>}>
      <Card>
        <div className={styles.row}><span>총 소모 칼로리</span><button type="button" onClick={() => navigate('/workout/edit')}>운동 수정</button></div>
        <p className={styles.kcal}>420 kcal</p>
      </Card>
      <Card>
        <h3 className={styles.title}>운동 루틴</h3>
        <ul className={styles.list}>
          {routines.map((routine) => <li key={routine.name}><strong>{routine.name}</strong><span>{routine.detail}</span></li>)}
        </ul>
      </Card>
    </AppLayout>
  );
}

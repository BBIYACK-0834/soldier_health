import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import styles from '../features/design/WorkoutPage.module.css';

const routines = [
  'PT 체조 · 20분 · 150 kcal',
  '근력 운동(팔굽혀펴기) · 3세트 · 90 kcal',
  '러닝 · 30분 · 180 kcal',
];

export default function WorkoutPage() {
  const navigate = useNavigate();
  return (
    <AppLayout title="운동 기록">
      <Card>
        <div className={styles.row}><span>총 소모 칼로리</span><button type="button" onClick={() => navigate('/workout/edit')}>운동 수정</button></div>
        <p className={styles.kcal}>420 kcal</p>
      </Card>
      <Card>
        <h3>운동 루틴 리스트</h3>
        <ul className={styles.list}>
          {routines.map((routine) => <li key={routine}>{routine}</li>)}
        </ul>
      </Card>
    </AppLayout>
  );
}

import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { mockUnits } from '../../constants/mockData';
import screen from '../../components/ui/Screen.module.css';
import styles from './SetupPage.module.css';

export default function UnitCompletePage() {
  const navigate = useNavigate();
  const storedId = Number(localStorage.getItem('tg_mock_unit_id')) || mockUnits[0].id;
  const unit = mockUnits.find((item) => item.id === storedId) || mockUnits[0];

  return (
    <AppLayout showBottomNav={false}>
      <Card className={styles.completeCard}>
        <div className={styles.checkIcon}>✓</div>
        <h2>{unit.unitName}이<br />선택되었습니다!</h2>
        <p>이제 식단, 운동, 커뮤니티 관리가 시작됩니다.</p>
        <button type="button" className={screen.primaryButton} onClick={() => navigate('/guide')}>시작하기</button>
      </Card>
    </AppLayout>
  );
}

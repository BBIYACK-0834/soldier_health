import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { getMyUnit } from '../../api/unitApi';
import screen from '../../components/ui/Screen.module.css';
import styles from './SetupPage.module.css';

function readStoredUnit() {
  try {
    return JSON.parse(localStorage.getItem('tg_selected_unit'));
  } catch {
    return null;
  }
}

export default function UnitCompletePage() {
  const navigate = useNavigate();
  const [unit, setUnit] = useState(readStoredUnit());

  useEffect(() => {
    let mounted = true;
    async function loadUnit() {
      try {
        const currentUnit = await getMyUnit();
        if (mounted && currentUnit) setUnit(currentUnit);
      } catch {
        if (mounted) setUnit(readStoredUnit());
      }
    }
    loadUnit();
    return () => { mounted = false; };
  }, []);

  const unitName = unit?.unitName || '부대';

  return (
    <AppLayout showBottomNav={false}>
      <Card className={styles.completeCard}>
        <div className={styles.checkIcon}>✓</div>
        <h2>{unitName}이<br />선택되었습니다!</h2>
        <p>선택한 부대의 식단, 운동, 커뮤니티 관리가 시작됩니다.</p>
        <button type="button" className={screen.primaryButton} onClick={() => navigate('/guide')}>시작하기</button>
      </Card>
    </AppLayout>
  );
}

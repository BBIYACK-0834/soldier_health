import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import screen from '../../components/ui/Screen.module.css';
import styles from './GuidePage.module.css';

const steps = [
  { icon: '🍽️', title: '1. 식단', text: '부대 식단 기반으로 오늘 섭취량을 자동 확인하고 부족한 영양을 채우세요.' },
  { icon: '🏋️', title: '2. 운동', text: '보유 기구와 부대 데이터셋을 기반으로 운동 루틴을 기록하세요.' },
  { icon: '👥', title: '3. 커뮤니티', text: '우리 부대원과 식단·운동 팁을 공유하세요.' },
];

export default function GuidePage() {
  const navigate = useNavigate();
  return (
    <AppLayout title="앱 사용 가이드" showBottomNav={false}>
      {steps.map((step) => (
        <Card key={step.title}>
          <div className={styles.step}>
            <span>{step.icon}</span>
            <div><h3>{step.title}</h3><p>{step.text}</p></div>
          </div>
        </Card>
      ))}
      <button type="button" className={screen.primaryButton} onClick={() => navigate('/home')}>다음</button>
    </AppLayout>
  );
}

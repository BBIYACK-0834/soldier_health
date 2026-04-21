import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import ProgressBar from '../components/ui/ProgressBar';
import MacroBox from '../components/ui/MacroBox';
import WorkoutCheckCircle from '../components/ui/WorkoutCheckCircle';
import styles from '../features/design/HomePage.module.css';

export default function HomePage() {
  const navigate = useNavigate();

  return (
    <AppLayout title="이병 김다이어트님," subtitle="오늘도 수고 많으셨습니다!">
      <Card className={styles.mainDietCard}>
        <div className={styles.rowBetween}><span className={styles.pill}>오늘의 식단</span><button type="button" className={styles.smallBtn} onClick={() => navigate('/diet')}>식단 보기</button></div>
        <p className={styles.title}>총 섭취 칼로리</p>
        <p className={styles.kcal}>1,520 <span>/ 2,000 kcal</span></p>
        <ProgressBar value={1520} max={2000} />
        <div className={styles.macroGrid}>
          <MacroBox label="탄수화물" intake={180} target={300} color="#50739a" tone="#dfe5ef" />
          <MacroBox label="단백질" intake={88} target={120} color="#6f8f55" tone="#e4e9de" />
          <MacroBox label="지방" intake={38} target={60} color="#d28a2c" tone="#efe2cf" />
        </div>
      </Card>

      <Card>
        <div className={styles.rowBetween}><h3>운동</h3><button type="button" className={styles.smallBtn} onClick={() => navigate('/workout')}>운동 기록</button></div>
        <p className={styles.meta}>이번 주 운동 3회 / 목표 4회</p>
        <div className={styles.checkGrid}>
          {[1, 2, 3, 4].map((i) => <WorkoutCheckCircle key={i} checked={i <= 3} label={`${i}회차`} />)}
        </div>
      </Card>

      <Card className={styles.tipCard}>
        <div className={styles.avatar}>🪖</div>
        <div>
          <strong>오늘의 팁</strong>
          <p>수분 섭취는 군 생활의 기본!</p>
        </div>
      </Card>
    </AppLayout>
  );
}

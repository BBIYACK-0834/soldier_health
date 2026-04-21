import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import styles from '../features/design/OnboardingPage.module.css';

export default function OnboardingPage() {
  const navigate = useNavigate();
  return (
    <AppLayout showBottomNav={false}>
      <section className={styles.hero}>
        <div className={styles.overlay}>
          <h1>강한 몸이 강한 임무를 만듭니다.</h1>
          <p>군인을 위한 맞춤 다이어트, 체력관리 앱</p>
          <button type="button" onClick={() => navigate('/login')}>시작하기</button>
        </div>
      </section>
    </AppLayout>
  );
}

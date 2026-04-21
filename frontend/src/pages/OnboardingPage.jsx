import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import styles from '../features/design/OnboardingPage.module.css';

export default function OnboardingPage() {
  const navigate = useNavigate();
  return (
    <AppLayout showBottomNav={false} showStatusBar={false}>
      <section className={styles.hero}>
        <div className={styles.overlay}>
          <h1>강한 몸이<br />강한 임무를 만듭니다.</h1>
          <p>군인을 위한 맞춤 다이어트로
            <br />체력도, 자신감도 함께 키워보세요.</p>
          <div className={styles.dotWrap}><span className={styles.on} /><span /><span /><span /></div>
          <button type="button" onClick={() => navigate('/login')}>시작하기</button>
          <button type="button" className={styles.link} onClick={() => navigate('/login')}>로그인</button>
        </div>
      </section>
    </AppLayout>
  );
}

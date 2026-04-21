import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import styles from '../features/design/AuthPage.module.css';

export default function SignupPage() {
  const navigate = useNavigate();
  return (
    <AppLayout title="회원가입" subtitle="더 강한 변화를 만듭니다." showBottomNav={false}>
      <form className={styles.form} onSubmit={(e) => { e.preventDefault(); navigate('/login'); }}>
        <label>이메일<input type="email" placeholder="example@army.mil" required /></label>
        <label>비밀번호<input type="password" required /></label>
        <label>비밀번호 확인<input type="password" required /></label>
        <label>닉네임<input type="text" required /></label>
        <button type="submit">회원가입 완료</button>
      </form>
    </AppLayout>
  );
}

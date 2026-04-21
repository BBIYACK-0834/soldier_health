import { Link, useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import styles from '../features/design/AuthPage.module.css';

export default function LoginPage() {
  const navigate = useNavigate();
  return (
    <AppLayout title="로그인" subtitle="기록에서 시작됩니다." showBottomNav={false}>
      <form className={styles.form} onSubmit={(e) => { e.preventDefault(); navigate('/home'); }}>
        <label>이메일<input type="email" placeholder="example@army.mil" required /></label>
        <label>비밀번호<input type="password" placeholder="비밀번호 입력" required /></label>
        <label className={styles.check}><input type="checkbox" /> 로그인 유지</label>
        <button type="submit">로그인</button>
        <p>계정이 없으신가요? <Link to="/signup">회원가입</Link></p>
      </form>
    </AppLayout>
  );
}

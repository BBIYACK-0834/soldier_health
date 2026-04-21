import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import styles from '../features/design/AuthPage.module.css';

export default function SignupPage() {
  const navigate = useNavigate();
  return (
    <AppLayout showBottomNav={false} title="함께하는 기록이" subtitle="더 강한 변화를 만듭니다.">
      <form className={styles.form} onSubmit={(e) => { e.preventDefault(); navigate('/setup/unit'); }}>
        <label>이메일<input type="email" placeholder="example@army.mil" required /></label>
        <label>비밀번호<input type="password" placeholder="비밀번호를 입력하세요" required /></label>
        <label>비밀번호 확인<input type="password" placeholder="비밀번호를 다시 입력하세요" required /></label>
        <label>닉네임<input type="text" placeholder="닉네임을 입력하세요" required /></label>
        <label className={styles.check}><input type="checkbox" required /> 이용약관 및 개인정보처리방침에 동의합니다.</label>
        <button type="submit">회원가입 완료</button>
      </form>
    </AppLayout>
  );
}

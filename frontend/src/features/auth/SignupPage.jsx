import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppContext } from '../../app/AppContext';
import AppLayout from '../../components/layout/AppLayout';
import { login, signup } from '../../api/authApi';
import { ACCESS_TOKEN_KEY } from '../../api/httpClient';
import styles from './AuthPage.module.css';

const defaultProfileImageUrl = 'https://api.dicebear.com/9.x/thumbs/svg?seed=soldier-default';

export default function SignupPage() {
  const navigate = useNavigate();
  const { actions } = useAppContext();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [nickname, setNickname] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!email || !password || !passwordConfirm || !nickname.trim()) {
      setErrorMessage('회원가입 정보를 모두 입력해주세요.');
      return;
    }

    if (password !== passwordConfirm) {
      setErrorMessage('비밀번호가 일치하지 않습니다.');
      return;
    }

    try {
      setSubmitting(true);
      await signup({ email, password, nickname: nickname.trim(), profileImageUrl: defaultProfileImageUrl });
      const loginData = await login({ email, password });
      const accessToken = loginData?.accessToken;
      if (accessToken) {
        localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
      }
      actions.setAuth(loginData);
      navigate('/unit/setup');
    } catch (error) {
      setErrorMessage(error?.response?.data?.message || error?.message || '회원가입에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AppLayout showBottomNav={false}>
      <section className={styles.signupCard}>
        <h1>회원가입</h1>
        <p>계정 생성에 필요한 정보만 입력해주세요. 프로필 사진과 군 생활 정보는 다음 단계에서 설정합니다.</p>
        <form className={styles.form} onSubmit={handleSubmit}>
          <label>이메일<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required /></label>
          <label>닉네임<input value={nickname} onChange={(e) => setNickname(e.target.value)} required /></label>
          <label>비밀번호<input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required /></label>
          <label>비밀번호 확인<input type="password" value={passwordConfirm} onChange={(e) => setPasswordConfirm(e.target.value)} required /></label>
          <small className={styles.termsText}>이용약관 및 개인정보처리방침에 동의합니다.</small>
          <button type="submit" disabled={submitting}>{submitting ? '가입 중...' : '회원가입'}</button>
        </form>
        {errorMessage ? <p className={styles.error}>{errorMessage}</p> : null}
        <button type="button" className={styles.linkButton} onClick={() => navigate('/login')}>이미 계정이 있어요</button>
      </section>
    </AppLayout>
  );
}

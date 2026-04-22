import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import AppLayout from '../components/layout/AppLayout';
import { login, signup } from '../api/authApi';
import { ACCESS_TOKEN_KEY } from '../api/httpClient';
import styles from '../features/design/AuthPage.module.css';

export default function SignupPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [nickname, setNickname] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (password !== passwordConfirm) {
      setErrorMessage('비밀번호가 일치하지 않습니다.');
      return;
    }

    try {
      setSubmitting(true);
      await signup({ email, password, nickname });
      const loginData = await login({ email, password });
      const accessToken = loginData?.accessToken;

      if (!accessToken) {
        throw new Error('회원가입 후 로그인 토큰을 받지 못했습니다.');
      }

      localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
      navigate('/setup/unit');
    } catch (error) {
      setErrorMessage(error.message || '회원가입에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AppLayout showBottomNav={false} title="함께하는 기록이" subtitle="더 강한 변화를 만듭니다.">
      <form className={styles.form} onSubmit={handleSubmit}>
        <label>이메일<input type="email" placeholder="example@army.mil" value={email} onChange={(e) => setEmail(e.target.value)} required /></label>
        <label>비밀번호<input type="password" placeholder="비밀번호를 입력하세요" value={password} onChange={(e) => setPassword(e.target.value)} required /></label>
        <label>비밀번호 확인<input type="password" placeholder="비밀번호를 다시 입력하세요" value={passwordConfirm} onChange={(e) => setPasswordConfirm(e.target.value)} required /></label>
        <label>닉네임<input type="text" placeholder="닉네임을 입력하세요" value={nickname} onChange={(e) => setNickname(e.target.value)} required /></label>
        <label className={styles.check}><input type="checkbox" required /> 이용약관 및 개인정보처리방침에 동의합니다.</label>
        {errorMessage ? <p>{errorMessage}</p> : null}
        <button type="submit" disabled={submitting}>{submitting ? '가입 중...' : '회원가입 완료'}</button>
      </form>
    </AppLayout>
  );
}

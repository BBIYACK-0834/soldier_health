import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { login } from '../api/authApi';
import { useAppContext } from '../app/AppContext';
import MobileShell from '../components/layout/MobileShell';
import styles from '../features/auth/AuthForm.module.css';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const { actions } = useAppContext();

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const data = await login({ email, password });
      actions.setAuth(data.accessToken, { id: data.userId, email: data.email, nickname: data.nickname });
      navigate('/onboarding');
    } catch (err) {
      setError(err.response?.data?.error || '로그인에 실패했습니다.');
    }
  };

  return (
    <MobileShell title="특급전사 로그인">
      <form className={styles.container} onSubmit={onSubmit}>
        <input className={styles.input} value={email} onChange={(e) => setEmail(e.target.value)} placeholder="이메일" />
        <input
          className={styles.input}
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="비밀번호"
        />
        {error ? <p className={styles.error}>{error}</p> : null}
        <button className={styles.button} type="submit">
          로그인
        </button>
      </form>
      <Link to="/signup" className={styles.subButton}>
        계정이 없다면 회원가입
      </Link>
    </MobileShell>
  );
}

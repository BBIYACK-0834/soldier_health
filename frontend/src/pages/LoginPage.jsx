import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { login } from '../api/authApi';
import { useAppContext } from '../app/AppContext';
import MobileShell from '../components/layout/MobileShell';
import styles from '../features/auth/AuthForm.module.css';

function getLoginErrorMessage(error) {
  if (!error) return '로그인에 실패했습니다.';

  if (error.code === 'NETWORK_ERROR') {
    return '백엔드 서버에 연결하지 못했습니다. 서버 실행 상태와 CORS 설정을 확인해주세요.';
  }

  if (error.status === 401 || error.status === 403) {
    return '이메일 또는 비밀번호가 올바르지 않습니다.';
  }

  return error.message || '로그인에 실패했습니다.';
}

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { actions } = useAppContext();

  const onSubmit = async (e) => {
    e.preventDefault();

    if (!email || !password) {
      setError('이메일과 비밀번호를 모두 입력해주세요.');
      return;
    }

    setError('');
    setIsSubmitting(true);

    try {
      const data = await login({ email: email.trim(), password });
      actions.setAuth(data);
      navigate('/');
    } catch (err) {
      setError(getLoginErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <MobileShell title="특급전사 로그인">
      <form className={styles.container} onSubmit={onSubmit}>
        {location.state?.message ? <p>{location.state.message}</p> : null}
        <input
          className={styles.input}
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="이메일"
          autoComplete="email"
        />
        <input
          className={styles.input}
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="비밀번호"
          autoComplete="current-password"
        />
        {error ? <p className={styles.error}>{error}</p> : null}
        <button className={styles.button} type="submit" disabled={isSubmitting}>
          {isSubmitting ? '로그인 중...' : '로그인'}
        </button>
      </form>
      <Link to="/signup" className={styles.subButton}>
        계정이 없다면 회원가입
      </Link>
    </MobileShell>
  );
}

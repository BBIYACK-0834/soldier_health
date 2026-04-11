import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { signup } from '../api/authApi';
import MobileShell from '../components/layout/MobileShell';
import styles from '../features/auth/AuthForm.module.css';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function getSignupErrorMessage(error) {
  if (!error) return '회원가입에 실패했습니다.';

  if (error.code === 'NETWORK_ERROR') {
    return '서버 연결에 실패했습니다. 백엔드 실행 상태/CORS/API 주소를 확인해주세요.';
  }

  if (error.status === 400 && error.message?.includes('사용 중')) {
    return '이미 사용 중인 이메일입니다. 다른 이메일을 사용해주세요.';
  }

  return error.message || '회원가입에 실패했습니다.';
}

export default function SignupPage() {
  const [form, setForm] = useState({
    email: '',
    password: '',
    nickname: '',
  });
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();

  const update = (key, value) => setForm((prev) => ({ ...prev, [key]: value }));

  const onSubmit = async (e) => {
    e.preventDefault();

    const email = form.email.trim();
    const nickname = form.nickname.trim();

    if (!email || !form.password || !nickname) {
      setError('이메일, 비밀번호, 닉네임을 모두 입력해주세요.');
      return;
    }

    if (!EMAIL_REGEX.test(email)) {
      setError('올바른 이메일 형식을 입력해주세요.');
      return;
    }

    if (form.password.length < 4) {
      setError('비밀번호는 최소 4자 이상 입력해주세요.');
      return;
    }

    setError('');
    setIsSubmitting(true);

    try {
      await signup({ email, password: form.password, nickname });
      navigate('/login', {
        replace: true,
        state: { message: '회원가입이 완료되었습니다. 로그인 후 초기 설정을 진행해주세요.' },
      });
    } catch (err) {
      setError(getSignupErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <MobileShell title="특급전사 회원가입">
      <form className={styles.container} onSubmit={onSubmit}>
        <p className={styles.guide}>가입은 1분이면 끝납니다. 기본 정보만 먼저 입력해주세요.</p>
        <input className={styles.input} type="email" placeholder="이메일(아이디)" value={form.email} onChange={(e) => update('email', e.target.value)} />
        <input
          className={styles.input}
          type="password"
          placeholder="비밀번호"
          value={form.password}
          onChange={(e) => update('password', e.target.value)}
        />
        <input
          className={styles.input}
          placeholder="닉네임"
          value={form.nickname}
          onChange={(e) => update('nickname', e.target.value)}
        />
        {error ? <p className={styles.error}>{error}</p> : null}
        <button className={styles.button} type="submit" disabled={isSubmitting}>
          {isSubmitting ? '가입 중입니다...' : '회원가입'}
        </button>
      </form>
      <Link to="/login" className={styles.subButton}>
        이미 계정이 있다면 로그인하기
      </Link>
    </MobileShell>
  );
}

import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { signup } from '../api/authApi';
import MobileShell from '../components/layout/MobileShell';
import styles from '../features/auth/AuthForm.module.css';

const goalOptions = ['BULK', 'CUT', 'MAINTAIN', 'FITNESS_TEST', 'GENERAL_FITNESS'];
const workoutLevels = ['BEGINNER', 'NOVICE', 'INTERMEDIATE'];
const branches = ['ARMY', 'NAVY', 'AIR_FORCE', 'MARINES', 'ETC'];

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function getSignupErrorMessage(error) {
  if (!error) return '회원가입에 실패했습니다.';

  if (error.code === 'NETWORK_ERROR') {
    return '서버 연결에 실패했습니다. 백엔드 실행 상태/CORS/API 주소를 확인해주세요.';
  }

  if (error.status === 400 && error.message?.includes('이메일')) {
    return error.message;
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
    goalType: 'GENERAL_FITNESS',
    workoutLevel: 'BEGINNER',
    branchType: 'ARMY',
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
      await signup({ ...form, email, nickname });
      navigate('/login', {
        replace: true,
        state: { message: '회원가입이 완료되었습니다. 로그인해주세요.' },
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
        <input className={styles.input} type="email" placeholder="이메일" value={form.email} onChange={(e) => update('email', e.target.value)} />
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
        <select className={styles.select} value={form.goalType} onChange={(e) => update('goalType', e.target.value)}>
          {goalOptions.map((goal) => (
            <option key={goal}>{goal}</option>
          ))}
        </select>
        <select className={styles.select} value={form.workoutLevel} onChange={(e) => update('workoutLevel', e.target.value)}>
          {workoutLevels.map((level) => (
            <option key={level}>{level}</option>
          ))}
        </select>
        <select className={styles.select} value={form.branchType} onChange={(e) => update('branchType', e.target.value)}>
          {branches.map((branch) => (
            <option key={branch}>{branch}</option>
          ))}
        </select>
        {error ? <p className={styles.error}>{error}</p> : null}
        <button className={styles.button} type="submit" disabled={isSubmitting}>
          {isSubmitting ? '가입 중...' : '가입하고 시작하기'}
        </button>
      </form>
      <Link to="/login" className={styles.subButton}>
        이미 계정이 있다면 로그인
      </Link>
    </MobileShell>
  );
}

import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { signup } from '../api/authApi';
import { useAppContext } from '../app/AppContext';
import MobileShell from '../components/layout/MobileShell';
import styles from '../features/auth/AuthForm.module.css';

const goalOptions = ['BULK', 'CUT', 'MAINTAIN', 'FITNESS_TEST', 'GENERAL_FITNESS'];
const workoutLevels = ['BEGINNER', 'NOVICE', 'INTERMEDIATE'];
const branches = ['ARMY', 'NAVY', 'AIR_FORCE', 'MARINES', 'ETC'];

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
  const { actions } = useAppContext();
  const navigate = useNavigate();

  const update = (key, value) => setForm((prev) => ({ ...prev, [key]: value }));

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const data = await signup(form);
      actions.setAuth(data.accessToken, { id: data.userId, email: data.email, nickname: data.nickname });
      navigate('/onboarding');
    } catch (err) {
      setError(err.response?.data?.error || '회원가입에 실패했습니다.');
    }
  };

  return (
    <MobileShell title="특급전사 회원가입">
      <form className={styles.container} onSubmit={onSubmit}>
        <input className={styles.input} placeholder="이메일" value={form.email} onChange={(e) => update('email', e.target.value)} />
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
        <button className={styles.button} type="submit">
          가입하고 시작하기
        </button>
      </form>
      <Link to="/login" className={styles.subButton}>
        이미 계정이 있다면 로그인
      </Link>
    </MobileShell>
  );
}

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppContext } from '../../app/AppContext';
import { login, signup } from '../../api/authApi';
import { ACCESS_TOKEN_KEY } from '../../api/httpClient';
import styles from './AuthPage.module.css';

const profilePresets = [
  'https://api.dicebear.com/9.x/thumbs/svg?seed=soldier-1',
  'https://api.dicebear.com/9.x/thumbs/svg?seed=soldier-2',
  'https://api.dicebear.com/9.x/thumbs/svg?seed=soldier-3',
];

export default function SignupPage() {
  const navigate = useNavigate();
  const { actions } = useAppContext();
  const [step, setStep] = useState('account');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [nickname, setNickname] = useState('');
  const [profileImageUrl, setProfileImageUrl] = useState(profilePresets[0]);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const goProfileStep = () => {
    setErrorMessage('');
    if (!email || !password || !passwordConfirm) {
      setErrorMessage('계정 정보를 모두 입력해주세요.');
      return;
    }
    if (password !== passwordConfirm) {
      setErrorMessage('비밀번호가 일치하지 않습니다.');
      return;
    }
    setStep('profile');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!nickname.trim()) {
      setErrorMessage('닉네임을 입력해주세요.');
      return;
    }

    try {
      setSubmitting(true);
      await signup({ email, password, nickname: nickname.trim(), profileImageUrl });
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
    <main className={styles.authShell}>
      <div className={styles.authCard}>
        <h1>회원가입</h1>
        <p>군 생활 건강 관리를 시작할 계정과 프로필을 만들어주세요.</p>
        <div className={styles.stepTabs}>
          <button type="button" className={step === 'account' ? styles.activeStep : ''} onClick={() => setStep('account')}>계정 정보</button>
          <button type="button" className={step === 'profile' ? styles.activeStep : ''} onClick={goProfileStep}>프로필</button>
        </div>
        <form onSubmit={handleSubmit}>
          {step === 'account' ? (
            <>
              <label>이메일<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required /></label>
              <label>비밀번호<input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required /></label>
              <label>비밀번호 확인<input type="password" value={passwordConfirm} onChange={(e) => setPasswordConfirm(e.target.value)} required /></label>
              <button type="button" onClick={goProfileStep}>다음: 프로필 설정</button>
            </>
          ) : (
            <>
              <div className={styles.profilePreview}>{profileImageUrl ? <img src={profileImageUrl} alt="프로필 미리보기" /> : '🪖'}</div>
              <label>닉네임<input value={nickname} onChange={(e) => setNickname(e.target.value)} required /></label>
              <div className={styles.presetGrid}>
                {profilePresets.map((preset) => (
                  <button key={preset} type="button" className={profileImageUrl === preset ? styles.selectedPreset : ''} onClick={() => setProfileImageUrl(preset)}>
                    <img src={preset} alt="프로필 프리셋" />
                  </button>
                ))}
              </div>
              <label>프로필 사진 URL<input value={profileImageUrl} onChange={(e) => setProfileImageUrl(e.target.value)} /></label>
              <small>이용약관 및 개인정보처리방침에 동의합니다.</small>
              <button type="submit" disabled={submitting}>{submitting ? '가입 중...' : '회원가입 완료'}</button>
            </>
          )}
        </form>
        {errorMessage ? <p className={styles.error}>{errorMessage}</p> : null}
        <button type="button" className={styles.linkButton} onClick={() => navigate('/login')}>이미 계정이 있어요</button>
      </div>
    </main>
  );
}

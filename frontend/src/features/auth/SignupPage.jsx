import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import AppLayout from '../../components/layout/AppLayout';
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

      if (!accessToken) {
        throw new Error('회원가입 후 로그인 토큰을 받지 못했습니다.');
      }

      localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
      navigate('/unit/setup');
    } catch (error) {
      if (error.code === 'NETWORK_ERROR') {
        localStorage.setItem(ACCESS_TOKEN_KEY, 'mock-access-token');
        setErrorMessage('서버 연결 전이라 예시 회원가입으로 진행합니다.');
        navigate('/unit/setup');
        return;
      }
      setErrorMessage(error.message || '회원가입에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AppLayout showBottomNav={false} title="함께하는 기록이" subtitle="더 강한 변화를 만듭니다.">
      <div className={styles.stepTabs}>
        <button type="button" className={step === 'account' ? styles.activeStep : ''} onClick={() => setStep('account')}>계정 정보</button>
        <button type="button" className={step === 'profile' ? styles.activeStep : ''} onClick={goProfileStep}>프로필</button>
      </div>
      <form className={styles.form} onSubmit={handleSubmit}>
        {step === 'account' ? (
          <>
            <label>이메일<input type="email" placeholder="example@army.mil" value={email} onChange={(e) => setEmail(e.target.value)} required /></label>
            <label>비밀번호<input type="password" placeholder="비밀번호를 입력하세요" value={password} onChange={(e) => setPassword(e.target.value)} required /></label>
            <label>비밀번호 확인<input type="password" placeholder="비밀번호를 다시 입력하세요" value={passwordConfirm} onChange={(e) => setPasswordConfirm(e.target.value)} required /></label>
            <button type="button" onClick={goProfileStep}>다음: 프로필 설정</button>
          </>
        ) : (
          <>
            <div className={styles.profilePreview}>{profileImageUrl ? <img src={profileImageUrl} alt="프로필 미리보기" /> : <span>🪖</span>}</div>
            <label>닉네임<input type="text" placeholder="닉네임을 입력하세요" value={nickname} onChange={(e) => setNickname(e.target.value)} required /></label>
            <div className={styles.presetGrid}>
              {profilePresets.map((preset) => (
                <button key={preset} type="button" className={profileImageUrl === preset ? styles.selectedPreset : ''} onClick={() => setProfileImageUrl(preset)}>
                  <img src={preset} alt="프로필 선택" />
                </button>
              ))}
            </div>
            <label>프로필 사진 URL<input type="url" placeholder="이미지 URL을 입력하세요" value={profileImageUrl} onChange={(e) => setProfileImageUrl(e.target.value)} /></label>
            <label className={styles.check}><input type="checkbox" required /> 이용약관 및 개인정보처리방침에 동의합니다.</label>
            <button type="submit" disabled={submitting}>{submitting ? '가입 중...' : '회원가입 완료'}</button>
          </>
        )}
        {errorMessage ? <p>{errorMessage}</p> : null}
      </form>
    </AppLayout>
  );
}

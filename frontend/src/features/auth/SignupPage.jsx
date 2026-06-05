import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppContext } from '../../app/AppContext';
import AppLayout from '../../components/layout/AppLayout';
import { login, signup } from '../../api/authApi';
import { ACCESS_TOKEN_KEY } from '../../api/httpClient';
import styles from './AuthPage.module.css';

const profilePresets = [
  'https://api.dicebear.com/9.x/thumbs/svg?seed=soldier-default',
  'https://api.dicebear.com/9.x/thumbs/svg?seed=soldier-1',
  'https://api.dicebear.com/9.x/thumbs/svg?seed=soldier-2',
];

export default function SignupPage() {
  const navigate = useNavigate();
  const { actions } = useAppContext();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [nickname, setNickname] = useState('');
  const [profileImageUrl, setProfileImageUrl] = useState(profilePresets[0]);
  const [profileImageFile, setProfileImageFile] = useState(null);
  const [profilePreviewUrl, setProfilePreviewUrl] = useState(profilePresets[0]);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    if (!profileImageFile) {
      setProfilePreviewUrl(profileImageUrl);
      return undefined;
    }

    const objectUrl = URL.createObjectURL(profileImageFile);
    setProfilePreviewUrl(objectUrl);
    return () => URL.revokeObjectURL(objectUrl);
  }, [profileImageFile, profileImageUrl]);

  const selectPreset = (imageUrl) => {
    setProfileImageFile(null);
    setProfileImageUrl(imageUrl);
  };

  const handleProfileFileChange = (event) => {
    const file = event.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      setErrorMessage('프로필 이미지는 이미지 파일만 업로드할 수 있습니다.');
      event.target.value = '';
      return;
    }

    setErrorMessage('');
    setProfileImageFile(file);
  };

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
      await signup({
        email,
        password,
        nickname: nickname.trim(),
        profileImageUrl,
        profileImageFile,
      });
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
        <p>계정 정보와 사용할 프로필 사진을 설정해주세요. 군 생활 정보는 다음 단계에서 입력합니다.</p>
        <form className={styles.form} onSubmit={handleSubmit}>
          <label>이메일<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required /></label>
          <label>닉네임<input value={nickname} onChange={(e) => setNickname(e.target.value)} required /></label>
          <label>비밀번호<input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required /></label>
          <label>비밀번호 확인<input type="password" value={passwordConfirm} onChange={(e) => setPasswordConfirm(e.target.value)} required /></label>

          <div className={styles.signupProfileBox}>
            <div className={styles.profilePreview}>{profilePreviewUrl ? <img src={profilePreviewUrl} alt="프로필 미리보기" /> : '🪖'}</div>
            <div className={styles.presetGrid}>
              {profilePresets.map((preset) => (
                <button key={preset} type="button" className={!profileImageFile && profileImageUrl === preset ? styles.selectedPreset : ''} onClick={() => selectPreset(preset)}>
                  <img src={preset} alt="기본 프로필" />
                </button>
              ))}
            </div>
            <label className={styles.uploadLabel}>
              {profileImageFile ? profileImageFile.name : '프로필 이미지 업로드'}
              <input type="file" accept="image/*" onChange={handleProfileFileChange} />
            </label>
          </div>

          <small className={styles.termsText}>이용약관 및 개인정보처리방침에 동의합니다.</small>
          <button type="submit" disabled={submitting}>{submitting ? '가입 중...' : '회원가입'}</button>
        </form>
        {errorMessage ? <p className={styles.error}>{errorMessage}</p> : null}
        <button type="button" className={styles.linkButton} onClick={() => navigate('/login')}>이미 계정이 있어요</button>
      </section>
    </AppLayout>
  );
}

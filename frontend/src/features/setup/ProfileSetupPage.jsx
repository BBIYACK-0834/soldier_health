import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { getMyProfile, updateProfile } from '../../api/userApi';
import { calculateMilitaryService } from '../../utils/militaryService';
import styles from './SetupPage.module.css';

const goals = ['특급전사', '다이어트', '벌크업', '린매스업', '건강 관리'];
const levels = ['운동 초보', '초급', '중급', '고급'];

export default function ProfileSetupPage() {
  const navigate = useNavigate();
  const [nickname, setNickname] = useState('');
  const [profileImageUrl, setProfileImageUrl] = useState('');
  const [heightCm, setHeightCm] = useState('');
  const [weightKg, setWeightKg] = useState('');
  const [enlistmentDate, setEnlistmentDate] = useState('');
  const [goal, setGoal] = useState('');
  const [level, setLevel] = useState('');
  const [saving, setSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const serviceInfo = useMemo(() => calculateMilitaryService(enlistmentDate), [enlistmentDate]);

  useEffect(() => {
    let mounted = true;
    async function loadProfile() {
      try {
        const data = await getMyProfile();
        if (!mounted || !data) return;
        setNickname(data.nickname || '');
        setProfileImageUrl(data.profileImageUrl || '');
        setHeightCm(data.heightCm ? String(data.heightCm) : '');
        setWeightKg(data.weightKg ? String(data.weightKg) : '');
        setEnlistmentDate(data.enlistmentDate || '');
      } catch {
        if (!mounted) return;
      }
    }

    loadProfile();
    return () => {
      mounted = false;
    };
  }, []);

  const save = async () => {
    setErrorMessage('');


    try {
      setSaving(true);
      await updateProfile({
        nickname: nickname.trim() || null,
        profileImageUrl: profileImageUrl.trim() || null,
        heightCm: heightCm ? Number(heightCm) : null,
        weightKg: weightKg ? Number(weightKg) : null,
        enlistmentDate: enlistmentDate || null,
      });
      navigate('/home');
    } catch (error) {
      if (error.code === 'NETWORK_ERROR') {
        setErrorMessage('서버 연결 전이라 입력값은 저장하지 않고 다음 단계로 이동합니다.');
        navigate('/home');
        return;
      }
      setErrorMessage(error.message || '프로필 저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <AppLayout title="나의 상태 설정" subtitle="닉네임, 프로필 사진, 입대일을 관리합니다." showBottomNav={false}>
      <Card>
        <h3>1. 프로필</h3>
        <div className={styles.profileEdit}>
          <div className={styles.profilePreview}>{profileImageUrl ? <img src={profileImageUrl} alt="프로필 미리보기" /> : <span>🪖</span>}</div>
          <label>닉네임<input placeholder="닉네임" type="text" value={nickname} onChange={(e) => setNickname(e.target.value)} /></label>
          <label>프로필 사진 URL<input placeholder="이미지 URL" type="url" value={profileImageUrl} onChange={(e) => setProfileImageUrl(e.target.value)} /></label>
        </div>
      </Card>
      <Card>
        <h3>2. 키와 몸무게</h3>
        <div className={styles.inlineTwo}>
          <input placeholder="키(cm)" type="number" value={heightCm} onChange={(e) => setHeightCm(e.target.value)} />
          <input placeholder="몸무게(kg)" type="number" value={weightKg} onChange={(e) => setWeightKg(e.target.value)} />
        </div>
      </Card>
      <Card>
        <h3>3. 목표</h3>
        <div className={styles.chips}>
          {goals.map((item) => <button key={item} type="button" className={goal === item ? styles.activeChip : ''} onClick={() => setGoal(item)}>{item}</button>)}
        </div>
      </Card>
      <Card>
        <h3>4. 운동 경력</h3>
        <div className={styles.chips}>
          {levels.map((item) => <button key={item} type="button" className={level === item ? styles.activeChip : ''} onClick={() => setLevel(item)}>{item}</button>)}
        </div>
        <h3>5. 주당 운동 빈도</h3>
        <select className={styles.select}><option>선택 안 함</option><option>주 4~5회 / 1회 60분</option><option>주 2~3회 / 1회 40분</option></select>
      </Card>
      <Card>
        <h3>6. 입대일</h3>
        <input className={styles.search} type="date" value={enlistmentDate} onChange={(e) => setEnlistmentDate(e.target.value)} />
        <p className={styles.infoText}>육군 기준 18개월 복무로 전역일을 계산하고, 이병→일병 3개월·일병→상병 6개월·상병→병장 3개월 기준으로 계급을 자동 산정합니다.</p>
        {serviceInfo ? (
          <div className={styles.servicePreview}>
            <span>현재 계급 <strong>{serviceInfo.rank}</strong></span>
            <span>전역 예정일 <strong>{serviceInfo.dischargeDate}</strong></span>
            <span>다음 진급일 <strong>{serviceInfo.nextPromotionDate || '진급 완료'}</strong></span>
          </div>
        ) : null}
      </Card>
      {errorMessage ? <p className={styles.errorText}>{errorMessage}</p> : null}
      <button type="button" className={styles.primary} onClick={save} disabled={saving}>{saving ? '저장 중...' : '설정 완료'}</button>
    </AppLayout>
  );
}

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { getMyProfile } from '../../api/userApi';
import { emptyUser } from '../../constants/defaultData';
import MilitaryProfileForm from '../setup/MilitaryProfileForm';
import styles from './MyPage.module.css';

export default function ProfileSettingsPage() {
  const navigate = useNavigate();
  const [profile, setProfile] = useState(emptyUser);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let mounted = true;
    async function loadProfile() {
      try {
        const data = await getMyProfile();
        if (mounted) setProfile(data ? { ...emptyUser, ...data } : emptyUser);
      } catch {
        if (mounted) setProfile(emptyUser);
      } finally {
        if (mounted) setLoading(false);
      }
    }
    loadProfile();
    return () => { mounted = false; };
  }, []);

  return (
    <AppLayout title="내 정보 설정" subtitle="부대와 군 생활 정보를 수정할 수 있어요." showBottomNav={false}>
      <Card>
        <div className={styles.settingSummary}>
          <div>
            <p>현재 선택 부대</p>
            <strong>{profile?.unitName || '선택된 부대 없음'}</strong>
            <small>식단 탭과 동일한 부대 정보로 연동됩니다.</small>
          </div>
          <button type="button" onClick={() => navigate('/unit/setup?next=/mypage/settings')}>부대 변경</button>
        </div>
      </Card>

      {loading ? <p className={styles.muted}>내 정보를 불러오는 중...</p> : null}
      <MilitaryProfileForm initialProfile={profile} submitLabel="군 생활 정보 저장" onSaved={(savedProfile) => setProfile({ ...emptyUser, ...savedProfile })} />
    </AppLayout>
  );
}

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import { getMyProfile } from '../../api/userApi';
import MilitaryProfileForm from './MilitaryProfileForm';

export default function ProfileSetupPage() {
  const navigate = useNavigate();
  const [initialProfile, setInitialProfile] = useState(null);
  const [message, setMessage] = useState('');

  useEffect(() => {
    let mounted = true;

    getMyProfile()
      .then((profile) => {
        if (mounted) setInitialProfile(profile);
      })
      .catch(() => {
        if (mounted) setMessage('기존 군 생활 정보를 불러오지 못했습니다. 저장하면 새 설정으로 반영됩니다.');
      });

    return () => {
      mounted = false;
    };
  }, []);

  return (
    <AppLayout title="나의 군 생활 설정" subtitle="회원가입 때 설정한 프로필은 유지하고, 군 생활 정보만 입력합니다." showBottomNav={false}>
      {message ? <p>{message}</p> : null}
      <MilitaryProfileForm initialProfile={initialProfile} submitLabel="설정 완료" showProfileImage={false} onSaved={() => navigate('/unit/complete')} />
    </AppLayout>
  );
}

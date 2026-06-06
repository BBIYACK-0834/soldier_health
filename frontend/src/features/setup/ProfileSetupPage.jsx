import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import MilitaryProfileForm from './MilitaryProfileForm';

export default function ProfileSetupPage() {
  const navigate = useNavigate();

  return (
    <AppLayout title="나의 군 생활 설정" subtitle="회원가입 때 설정한 프로필은 유지하고, 군 생활 정보만 입력합니다." showBottomNav={false}>
      <MilitaryProfileForm submitLabel="설정 완료" showProfileImage={false} onSaved={() => navigate('/unit/complete')} />
    </AppLayout>
  );
}

import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import MilitaryProfileForm from './MilitaryProfileForm';

export default function ProfileSetupPage() {
  const navigate = useNavigate();

  return (
    <AppLayout title="나의 군 생활 설정" subtitle="계급과 전역·진급 일정을 입력해주세요." showBottomNav={false}>
      <MilitaryProfileForm submitLabel="설정 완료" onSaved={() => navigate('/unit/complete')} />
    </AppLayout>
  );
}

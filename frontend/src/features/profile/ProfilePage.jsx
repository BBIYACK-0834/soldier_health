import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { useAppContext } from '../../app/AppContext';
import { getMyProfile } from '../../api/userApi';
import { mockUser } from '../../constants/mockData';
import { calculateMilitaryService } from '../../utils/militaryService';
import styles from './MyPage.module.css';

const menus = [
  { label: '프로필 설정', path: '/setup/profile', icon: '🪖' },
  { label: '부대 설정', path: '/unit/setup', icon: '🏕️' },
  { label: '운동 기구 설정', path: '/setup/equipment', icon: '🏋️' },
  { label: '목표 설정', path: '/mypage/goal', icon: '🎯' },
  { label: '알림 설정', path: '/mypage/notifications', icon: '🔔' },
  { label: '데이터 관리', path: '/mypage/data', icon: '💾' },
  { label: '내 게시글', path: '/mypage/posts', icon: '📝' },
];

export default function ProfilePage() {
  const navigate = useNavigate();
  const { actions } = useAppContext();
  const [profile, setProfile] = useState(mockUser);
  const [showSettingsMenu, setShowSettingsMenu] = useState(false);

  useEffect(() => {
    let mounted = true;
    async function load() {
      try {
        const data = await getMyProfile();
        if (!mounted) return;
        setProfile(data ? { ...mockUser, ...data } : mockUser);
      } catch {
        if (!mounted) return;
        setProfile(mockUser);
      }
    }
    load();
    return () => {
      mounted = false;
    };
  }, []);

  const serviceInfo = calculateMilitaryService(profile?.enlistmentDate) ?? profile;

  const handleLogout = () => {
    actions.logout();
    navigate('/login');
  };

  return (
    <AppLayout title="마이 페이지">
      <Card className={styles.profileTop}>
        <div className={styles.avatar}>{profile?.profileImageUrl ? <img src={profile.profileImageUrl} alt="프로필" /> : <span>🪖</span>}</div>
        <div>
          <h2>{profile?.nickname || '사용자'}</h2>
          <p>{serviceInfo?.rank || '이병'} · {profile?.unitName || '선택된 부대 없음'}</p>
          <small>전역 예정일 {serviceInfo?.dischargeDate || '입대일 미설정'}</small>
        </div>
        <button type="button" className={styles.settings} aria-expanded={showSettingsMenu} onClick={() => setShowSettingsMenu((prev) => !prev)}>⚙️</button>
      </Card>
      <div className={styles.stats}>
        <Card><p>연속 기록</p><strong>{profile?.streakDays ?? 0}일</strong></Card>
        <Card><p>전역까지</p><strong>D-{serviceInfo?.daysUntilDischarge ?? '?'}</strong></Card>
      </div>
      {showSettingsMenu ? (
        <Card>
          <h3 className={styles.sectionTitle}>설정</h3>
          <ul className={styles.menu}>
            {menus.map((item) => <li key={item.label} onClick={() => navigate(item.path)}><span>{item.icon} {item.label}</span><b>수정</b></li>)}
          </ul>
        </Card>
      ) : null}
      <button type="button" className={styles.logoutButton} onClick={handleLogout}>로그아웃</button>
    </AppLayout>
  );
}

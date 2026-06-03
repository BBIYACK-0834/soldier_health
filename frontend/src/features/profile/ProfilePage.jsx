import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { useAppContext } from '../../app/AppContext';
import { getMyProfile } from '../../api/userApi';
import { emptyUser } from '../../constants/defaultData';
import styles from './MyPage.module.css';

const menus = [
  { label: '내 정보 설정', path: '/mypage/settings', icon: '⚙️' },
  { label: '목표 설정', path: '/mypage/goal', icon: '🎯' },
  { label: '알림 설정', path: '/mypage/notifications', icon: '🔔' },
  { label: '데이터 관리', path: '/mypage/data', icon: '💾' },
  { label: '내 게시글', path: '/mypage/posts', icon: '📝' },
];

function getRemainingDays(dateString) {
  if (!dateString) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const target = new Date(dateString);
  if (Number.isNaN(target.getTime())) return null;
  target.setHours(0, 0, 0, 0);
  return Math.ceil((target.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
}

function formatDday(label, dateString) {
  const days = getRemainingDays(dateString);
  if (days === null) return null;
  if (days < 0) return `${label} D+${Math.abs(days)}`;
  if (days === 0) return `${label} D-Day`;
  return `${label} D-${days}`;
}

export default function ProfilePage() {
  const navigate = useNavigate();
  const { actions } = useAppContext();
  const [profile, setProfile] = useState(emptyUser);

  useEffect(() => {
    let mounted = true;
    async function load() {
      try {
        const data = await getMyProfile();
        if (!mounted) return;
        setProfile(data ? { ...emptyUser, ...data } : emptyUser);
      } catch {
        if (!mounted) return;
        setProfile(emptyUser);
      }
    }
    load();
    return () => {
      mounted = false;
    };
  }, []);

  const dischargeDday = formatDday('전역', profile?.dischargeDate);
  const promotionDday = formatDday('진급', profile?.promotionDate);

  const handleLogout = () => {
    actions.logout();
    navigate('/login');
  };

  return (
    <AppLayout title="마이 페이지">
      <Card className={styles.profileTop}>
        <div className={styles.avatar}>🪖</div>
        <div>
          <h2>{profile?.nickname || '사용자'}</h2>
          <p>{profile?.rank || '계급 미설정'} · {profile?.unitName || '선택된 부대 없음'}</p>
          <div className={styles.militaryMeta}>
            {dischargeDday ? <span>{dischargeDday}</span> : null}
            {promotionDday ? <span>{promotionDday}</span> : null}
          </div>
        </div>
        <button type="button" className={styles.settings} onClick={() => navigate('/mypage/settings')} aria-label="내 정보 설정">⚙️</button>
      </Card>
      <div className={styles.stats}>
        <Card><p>연속 기록</p><strong>{profile?.streakDays ?? 0}일</strong></Card>
        <Card><p>총 감량</p><strong>{profile?.totalWeightLoss ?? 0} kg</strong></Card>
      </div>
      <Card>
        <ul className={styles.menu}>
          {menus.map((item) => <li key={item.label} onClick={() => navigate(item.path)}><span>{item.icon} {item.label}</span><b>›</b></li>)}
        </ul>
      </Card>
      <button type="button" className={styles.logoutButton} onClick={handleLogout}>로그아웃</button>
    </AppLayout>
  );
}

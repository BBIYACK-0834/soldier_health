import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import { useAppContext } from '../app/AppContext';
import { getMyProfile } from '../api/userApi';
import styles from '../features/design/MyPage.module.css';

const menus = ['목표 설정', '알림 설정', '데이터 관리', '문의하기'];

export default function ProfilePage() {
  const navigate = useNavigate();
  const { actions } = useAppContext();
  const [profile, setProfile] = useState(null);

  useEffect(() => {
    let mounted = true;
    async function load() {
      try {
        const data = await getMyProfile();
        if (!mounted) return;
        setProfile(data ?? null);
      } catch {
        if (!mounted) return;
        setProfile(null);
      }
    }
    load();
    return () => {
      mounted = false;
    };
  }, []);

  const handleLogout = () => {
    actions.logout();
    navigate('/login');
  };

  return (
    <AppLayout>
      <Card className={styles.profileTop}>
        <div className={styles.avatar}>🪖</div>
        <h2>{profile?.nickname || '사용자'}</h2>
        <p>{profile?.unitName || '선택된 부대 없음'}</p>
      </Card>
      <div className={styles.stats}>
        <Card><p>목표</p><strong>{profile?.goalType || '데이터 없음'}</strong></Card>
        <Card><p>운동 수준</p><strong>{profile?.workoutLevel || '데이터 없음'}</strong></Card>
      </div>
      <Card>
        <h3 className={styles.sectionTitle}>내 게시글</h3>
        <p>등록된 게시글이 없습니다.</p>
      </Card>
      <Card>
        <ul className={styles.menu}>
          {menus.map((item) => <li key={item}>{item}<span>›</span></li>)}
        </ul>
      </Card>
      <button type="button" className={styles.logoutButton} onClick={handleLogout}>로그아웃</button>
    </AppLayout>
  );
}

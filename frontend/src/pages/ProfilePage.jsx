import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import { useAppContext } from '../app/AppContext';
import styles from '../features/design/MyPage.module.css';

const menus = ['목표 설정', '알림 설정', '데이터 관리', '문의하기'];
const myPosts = ['다음 PT 몇 세트 하세요?', '부대 식단으로 -3kg 성공!'];

export default function ProfilePage() {
  const navigate = useNavigate();
  const { actions } = useAppContext();

  const handleLogout = () => {
    actions.logout();
    navigate('/login');
  };

  return (
    <AppLayout>
      <Card className={styles.profileTop}>
        <div className={styles.avatar}>🪖</div>
        <h2>이병 김다이어트</h2>
        <p>D + 120</p>
      </Card>
      <div className={styles.stats}>
        <Card><p>연속 기록</p><strong>🏅 14일</strong></Card>
        <Card><p>총 감량</p><strong>🧳 -1.8 kg</strong></Card>
      </div>
      <Card>
        <h3 className={styles.sectionTitle}>내 게시글</h3>
        <ul className={styles.postList}>{myPosts.map((post) => <li key={post}>{post}</li>)}</ul>
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

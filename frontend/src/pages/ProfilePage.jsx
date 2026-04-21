import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import styles from '../features/design/MyPage.module.css';

const menus = ['목표 설정', '알림 설정', '데이터 관리', '내 게시글'];

export default function ProfilePage() {
  return (
    <AppLayout title="마이페이지">
      <Card className={styles.profileTop}>
        <div className={styles.avatar}>🙂</div>
        <h2>이병 김다이어트</h2>
        <p>D + 120</p>
      </Card>
      <div className={styles.stats}>
        <Card><p>연속 기록</p><strong>14일</strong></Card>
        <Card><p>총 감량</p><strong>-1.8 kg</strong></Card>
      </div>
      <Card>
        <ul className={styles.menu}>
          {menus.map((item) => <li key={item}>{item}<span>›</span></li>)}
        </ul>
      </Card>
    </AppLayout>
  );
}

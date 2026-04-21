import { useState } from 'react';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import styles from '../features/design/CommunityPage.module.css';

const tabs = [
  { value: 'all', label: '전체' },
  { value: 'popular', label: '인기' },
  { value: 'unit', label: '우리 부대' },
];

const posts = [
  { user: '훈련병_123', time: '2시간 전', content: '다음 PT 몇 세트 하세요? 저는 상체 3세트로 시작했는데 조금 부담됩니다!', like: 12, comment: 4 },
  { user: '이병_파이팅', time: '5시간 전', content: '부대 식단으로 -3kg 성공! 꿀팁 공유합니다.', like: 24, comment: 8 },
];

export default function CommunityPage() {
  const [tab, setTab] = useState('all');
  return (
    <AppLayout title="커뮤니티" headerAction={<span className={styles.edit}>✏️</span>}>
      <div className={styles.tabWrap}>
        {tabs.map((item) => (
          <button key={item.value} type="button" className={`${styles.tab} ${tab === item.value ? styles.active : ''}`} onClick={() => setTab(item.value)}>{item.label}</button>
        ))}
      </div>
      <Card className={styles.notice}><strong>🔥 오늘의 인증</strong><p>훈련 전 30분 러닝 완료! 꾸준함이 가장 강한 무기입니다.</p></Card>
      {posts.map((post) => (
        <Card key={`${post.user}-${post.content}`}>
          <p className={styles.user}>{post.user} <span>{post.time}</span></p>
          <p className={styles.content}>{post.content}</p>
          <p className={styles.meta}>♡ {post.like} &nbsp;&nbsp; 댓글 {post.comment}</p>
        </Card>
      ))}
    </AppLayout>
  );
}

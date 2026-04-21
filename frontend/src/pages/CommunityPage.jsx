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
  { user: '훈련병_123', content: '오늘 PT 30분 러닝 완료! 꾸준함이 가장 강한 무기입니다.', like: 12, comment: 4 },
  { user: '이병_파이팅', content: '부대 식단으로 -3kg 성공! 끝까지 유지합니다.', like: 24, comment: 8 },
];

export default function CommunityPage() {
  const [tab, setTab] = useState('all');
  return (
    <AppLayout title="커뮤니티">
      <div className={styles.tabWrap}>
        {tabs.map((item) => (
          <button key={item.value} type="button" className={`${styles.tab} ${tab === item.value ? styles.active : ''}`} onClick={() => setTab(item.value)}>{item.label}</button>
        ))}
      </div>
      {posts.map((post) => (
        <Card key={`${post.user}-${post.content}`}>
          <p className={styles.user}>{post.user}</p>
          <p>{post.content}</p>
          <p className={styles.meta}>♡ {post.like} · 댓글 {post.comment}</p>
        </Card>
      ))}
    </AppLayout>
  );
}

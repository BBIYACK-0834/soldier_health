import { useEffect, useState } from 'react';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import { getCommunityPosts } from '../api/communityApi';
import styles from '../features/design/CommunityPage.module.css';

const tabs = [
  { value: 'ALL', label: '전체' },
  { value: 'POPULAR', label: '인기' },
  { value: 'UNIT', label: '우리 부대' },
];

export default function CommunityPage() {
  const [tab, setTab] = useState('ALL');
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    let mounted = true;
    async function load() {
      try {
        setLoading(true);
        const list = await getCommunityPosts(tab);
        if (!mounted) return;
        setPosts(list ?? []);
      } catch (error) {
        if (!mounted) return;
        setErrorMessage(error.message || '커뮤니티 글을 불러오지 못했습니다.');
        setPosts([]);
      } finally {
        if (mounted) setLoading(false);
      }
    }

    load();
    return () => {
      mounted = false;
    };
  }, [tab]);

  return (
    <AppLayout title="커뮤니티" headerAction={<span className={styles.edit}>✏️</span>}>
      <div className={styles.tabWrap}>
        {tabs.map((item) => (
          <button key={item.value} type="button" className={`${styles.tab} ${tab === item.value ? styles.active : ''}`} onClick={() => setTab(item.value)}>{item.label}</button>
        ))}
      </div>
      {loading ? <Card><p>불러오는 중...</p></Card> : null}
      {!loading && posts.length === 0 ? <Card><p>등록된 게시글이 없습니다.</p></Card> : null}
      {posts.map((post) => (
        <Card key={post.id}>
          <p className={styles.user}>{post.authorNickname || '익명'} <span>{post.createdAt || ''}</span></p>
          <p className={styles.content}>{post.content || ''}</p>
          <p className={styles.meta}>댓글 {post.commentCount ?? 0}</p>
        </Card>
      ))}
      {errorMessage ? <Card><p>{errorMessage}</p></Card> : null}
    </AppLayout>
  );
}

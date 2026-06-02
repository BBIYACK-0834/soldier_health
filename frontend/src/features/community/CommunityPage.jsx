import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { getCommunityPosts } from '../../api/communityApi';
import { mockPosts, mockUser } from '../../constants/mockData';
import styles from './CommunityPage.module.css';

const tabs = [
  { value: 'ALL', label: '전체', path: '/community' },
  { value: 'POPULAR', label: '인기', path: '/community/popular' },
  { value: 'UNIT', label: '우리 부대', path: '/community/unit' },
];

function tabFromPath(pathname) {
  if (pathname.includes('/popular')) return 'POPULAR';
  if (pathname.includes('/unit')) return 'UNIT';
  return 'ALL';
}

export default function CommunityPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const [tab, setTab] = useState(tabFromPath(location.pathname));
  const [posts, setPosts] = useState(mockPosts);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    setTab(tabFromPath(location.pathname));
  }, [location.pathname]);

  useEffect(() => {
    let mounted = true;
    async function load() {
      try {
        setLoading(true);
        const list = await getCommunityPosts(tab === 'POPULAR' ? 'ALL' : tab);
        if (!mounted) return;
        setPosts(list?.length ? list : mockPosts);
      } catch (error) {
        if (!mounted) return;
        setErrorMessage('서버 연결 전이라 예시 게시글로 표시합니다.');
        setPosts(mockPosts);
      } finally {
        if (mounted) setLoading(false);
      }
    }

    load();
    return () => {
      mounted = false;
    };
  }, [tab]);

  const visiblePosts = useMemo(() => {
    if (tab === 'POPULAR') return [...posts].sort((a, b) => (b.likeCount ?? 0) - (a.likeCount ?? 0));
    if (tab === 'UNIT') return posts.filter((post) => !post.unitName || post.unitName === mockUser.unitName);
    return posts;
  }, [posts, tab]);

  const handleTab = (item) => {
    setTab(item.value);
    navigate(item.path);
  };

  return (
    <AppLayout title={tab === 'UNIT' ? `${mockUser.unitName} 게시판` : '커뮤니티'} headerAction={<span className={styles.edit}>✏️</span>}>
      <div className={styles.tabWrap}>
        {tabs.map((item) => (
          <button key={item.value} type="button" className={`${styles.tab} ${tab === item.value ? styles.active : ''}`} onClick={() => handleTab(item)}>{item.label}</button>
        ))}
      </div>
      {loading ? <Card><p>불러오는 중...</p></Card> : null}
      {!loading && visiblePosts.length === 0 ? <Card><p>등록된 게시글이 없습니다.</p></Card> : null}
      {visiblePosts.map((post, index) => (
        <Card key={post.id}>
          <div className={styles.postHead}>
            {tab === 'POPULAR' ? <strong className={styles.rank}>{index + 1}</strong> : <span className={styles.avatar}>🪖</span>}
            <p className={styles.user}>{post.authorNickname || '익명'} <span>{post.unitName || '전 부대'} · {post.createdAt || ''}</span></p>
          </div>
          <h3 className={styles.title}>{post.title || '제목 없음'}</h3>
          <p className={styles.content}>{post.content || ''}</p>
          <p className={styles.meta}>♡ {post.likeCount ?? 0} · 댓글 {post.commentCount ?? 0}</p>
        </Card>
      ))}
      <button type="button" className={styles.fab}>+</button>
      {errorMessage ? <Card><p>{errorMessage}</p></Card> : null}
    </AppLayout>
  );
}

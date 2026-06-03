import { useCallback, useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { createCommunityPost, getCommunityPosts } from '../../api/communityApi';
import { useAppContext } from '../../app/AppContext';
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
  const { state } = useAppContext();
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [showComposer, setShowComposer] = useState(false);
  const [postTitle, setPostTitle] = useState('');
  const [postContent, setPostContent] = useState('');
  const [postCategory, setPostCategory] = useState(tab === 'UNIT' ? 'UNIT' : 'ALL');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setTab(tabFromPath(location.pathname));
  }, [location.pathname]);

  const loadPosts = useCallback(async (isMounted = () => true) => {
    try {
      setLoading(true);
      const list = await getCommunityPosts(tab === 'POPULAR' ? 'ALL' : tab);
      if (!isMounted()) return;
      setPosts(list ?? []);
      setErrorMessage('');
    } catch (error) {
      if (!isMounted()) return;
      setErrorMessage('게시글 데이터를 불러오지 못했습니다.');
      setPosts([]);
    } finally {
      if (isMounted()) setLoading(false);
    }
  }, [tab]);

  useEffect(() => {
    let mounted = true;
    loadPosts(() => mounted);
    return () => {
      mounted = false;
    };
  }, [loadPosts]);

  useEffect(() => {
    setPostCategory(tab === 'UNIT' ? 'UNIT' : 'ALL');
  }, [tab]);

  const visiblePosts = useMemo(() => {
    if (tab === 'POPULAR') return [...posts].sort((a, b) => (b.likeCount ?? 0) - (a.likeCount ?? 0));
    if (tab === 'UNIT') return posts.filter((post) => !state.user?.unitName || !post.unitName || post.unitName === state.user.unitName);
    return posts;
  }, [posts, state.user?.unitName, tab]);

  const handleTab = (item) => {
    setTab(item.value);
    navigate(item.path);
  };

  const openComposer = () => {
    setPostCategory(tab === 'UNIT' ? 'UNIT' : 'ALL');
    setShowComposer(true);
  };

  const closeComposer = () => {
    if (submitting) return;
    setShowComposer(false);
    setPostTitle('');
    setPostContent('');
  };

  const handleSubmitPost = async (event) => {
    event.preventDefault();
    setErrorMessage('');

    if (!postTitle.trim() || !postContent.trim()) {
      setErrorMessage('제목과 내용을 모두 입력해주세요.');
      return;
    }

    try {
      setSubmitting(true);
      const created = await createCommunityPost({
        category: postCategory,
        title: postTitle.trim(),
        content: postContent.trim(),
      });
      setPosts((prev) => [created, ...prev]);
      setShowComposer(false);
      setPostTitle('');
      setPostContent('');
      if (postCategory === 'UNIT' && tab !== 'UNIT') {
        navigate('/community/unit');
      }
    } catch (error) {
      setErrorMessage(error.message || '게시글 작성에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AppLayout title={tab === 'UNIT' ? `${state.user?.unitName || '우리 부대'} 게시판` : '커뮤니티'} headerAction={<button type="button" className={styles.edit} onClick={openComposer} aria-label="게시글 작성">✏️</button>}>
      <div className={styles.tabWrap}>
        {tabs.map((item) => (
          <button key={item.value} type="button" className={`${styles.tab} ${tab === item.value ? styles.active : ''}`} onClick={() => handleTab(item)}>{item.label}</button>
        ))}
      </div>
      {showComposer ? (
        <Card className={styles.composerCard}>
          <form className={styles.composerForm} onSubmit={handleSubmitPost}>
            <div className={styles.composerHead}>
              <h3>게시글 작성</h3>
              <button type="button" onClick={closeComposer} aria-label="작성 취소">×</button>
            </div>
            <div className={styles.categorySelect}>
              <button type="button" className={postCategory === 'ALL' ? styles.selectedCategory : ''} onClick={() => setPostCategory('ALL')}>전체</button>
              <button type="button" className={postCategory === 'UNIT' ? styles.selectedCategory : ''} onClick={() => setPostCategory('UNIT')}>우리 부대</button>
            </div>
            <input value={postTitle} onChange={(event) => setPostTitle(event.target.value)} placeholder="제목을 입력하세요" maxLength={80} />
            <textarea value={postContent} onChange={(event) => setPostContent(event.target.value)} placeholder="공유하고 싶은 내용을 작성하세요" rows={5} />
            <button type="submit" className={styles.submitButton} disabled={submitting}>{submitting ? '작성 중...' : '게시글 등록'}</button>
          </form>
        </Card>
      ) : null}
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
      <button type="button" className={styles.fab} onClick={openComposer} aria-label="게시글 작성">+</button>
      {errorMessage ? <Card><p>{errorMessage}</p></Card> : null}
    </AppLayout>
  );
}

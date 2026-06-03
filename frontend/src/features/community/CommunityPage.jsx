import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { createCommunityComment, createCommunityPost, getCommunityPostDetail, getCommunityPosts, likeCommunityPost } from '../../api/communityApi';
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

function formatCommunityTime(value) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date);
}

function createMockComment(content) {
  return {
    id: `comment-${Date.now()}`,
    authorNickname: mockUser.nickname,
    content,
    createdAt: new Date().toISOString(),
  };
}

export default function CommunityPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { postId } = useParams();
  const [tab, setTab] = useState(tabFromPath(location.pathname));
  const [posts, setPosts] = useState(mockPosts);
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [showComposer, setShowComposer] = useState(false);
  const [newPost, setNewPost] = useState({ title: '', content: '', category: 'ALL' });
  const [commentText, setCommentText] = useState('');
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

    if (!postId) load();
    return () => {
      mounted = false;
    };
  }, [tab, postId]);

  useEffect(() => {
    let mounted = true;
    async function loadDetail() {
      if (!postId) {
        setDetail(null);
        return;
      }

      try {
        setDetailLoading(true);
        const data = await getCommunityPostDetail(postId);
        if (!mounted) return;
        setDetail(data);
      } catch (error) {
        if (!mounted) return;
        const mockPost = location.state?.post ?? posts.find((post) => String(post.id) === String(postId)) ?? mockPosts.find((post) => String(post.id) === String(postId));
        setDetail(mockPost ? { post: mockPost, comments: [] } : null);
        setErrorMessage('서버 연결 전이라 예시 게시글 상세로 표시합니다.');
      } finally {
        if (mounted) setDetailLoading(false);
      }
    }

    loadDetail();
    return () => {
      mounted = false;
    };
  }, [location.state, postId, posts]);

  const visiblePosts = useMemo(() => {
    if (tab === 'POPULAR') return [...posts].sort((a, b) => (b.likeCount ?? 0) - (a.likeCount ?? 0));
    if (tab === 'UNIT') return posts.filter((post) => !post.unitName || post.unitName === mockUser.unitName);
    return posts;
  }, [posts, tab]);

  const handleTab = (item) => {
    setTab(item.value);
    navigate(item.path);
  };

  const openPost = (post) => {
    navigate(`/community/posts/${post.id}`, { state: { post } });
  };

  const submitPost = async (event) => {
    event.preventDefault();
    const title = newPost.title.trim();
    const content = newPost.content.trim();
    if (!title || !content) return;

    try {
      const created = await createCommunityPost({ ...newPost, title, content });
      setPosts((prev) => [created, ...prev]);
      setShowComposer(false);
      setNewPost({ title: '', content: '', category: 'ALL' });
      navigate(`/community/posts/${created.id}`, { state: { post: created } });
    } catch (error) {
      const created = {
        id: `local-${Date.now()}`,
        ...newPost,
        title,
        content,
        authorNickname: mockUser.nickname,
        createdAt: new Date().toISOString(),
        likeCount: 0,
        commentCount: 0,
      };
      setPosts((prev) => [created, ...prev]);
      setShowComposer(false);
      setNewPost({ title: '', content: '', category: 'ALL' });
      setErrorMessage('서버 연결 전이라 작성한 게시글을 화면에만 추가했습니다.');
      navigate(`/community/posts/${created.id}`, { state: { post: created } });
    }
  };

  const handleLike = async (post) => {
    if (!post) return;
    const optimisticPost = { ...post, likeCount: (post.likeCount ?? 0) + 1 };
    setDetail((prev) => (prev ? { ...prev, post: optimisticPost } : prev));
    setPosts((prev) => prev.map((item) => (String(item.id) === String(post.id) ? optimisticPost : item)));

    try {
      const liked = await likeCommunityPost(post.id);
      setDetail((prev) => (prev ? { ...prev, post: liked } : prev));
      setPosts((prev) => prev.map((item) => (String(item.id) === String(liked.id) ? liked : item)));
    } catch {
      setErrorMessage('서버 연결 전이라 좋아요를 화면에만 반영했습니다.');
    }
  };

  const submitComment = async (event) => {
    event.preventDefault();
    const content = commentText.trim();
    if (!content || !detail?.post) return;

    try {
      const created = await createCommunityComment(detail.post.id, { content });
      setDetail((prev) => ({
        ...prev,
        post: { ...prev.post, commentCount: (prev.post.commentCount ?? 0) + 1 },
        comments: [...(prev.comments ?? []), created],
      }));
      setCommentText('');
    } catch {
      const created = createMockComment(content);
      setDetail((prev) => ({
        ...prev,
        post: { ...prev.post, commentCount: (prev.post.commentCount ?? 0) + 1 },
        comments: [...(prev.comments ?? []), created],
      }));
      setCommentText('');
      setErrorMessage('서버 연결 전이라 댓글을 화면에만 추가했습니다.');
    }
  };

  if (postId) {
    const post = detail?.post;
    const comments = detail?.comments ?? [];

    return (
      <AppLayout title="게시글" headerAction={<button type="button" className={styles.backBtn} onClick={() => navigate(-1)}>뒤로</button>}>
        {detailLoading ? <Card><p>불러오는 중...</p></Card> : null}
        {!detailLoading && !post ? <Card><p>게시글을 찾을 수 없습니다.</p></Card> : null}
        {post ? (
          <>
            <Card>
              <div className={styles.postHead}>
                <span className={styles.avatar}>🪖</span>
                <p className={styles.user}>{post.authorNickname || '익명'} <span>{formatCommunityTime(post.createdAt)}</span></p>
              </div>
              <h3 className={styles.detailTitle}>{post.title || '제목 없음'}</h3>
              <p className={styles.detailContent}>{post.content || ''}</p>
              <div className={styles.actionRow}>
                <button type="button" onClick={() => handleLike(post)}>♡ 좋아요 {post.likeCount ?? 0}</button>
                <span>댓글 {post.commentCount ?? comments.length}</span>
              </div>
            </Card>
            <Card>
              <h3 className={styles.commentTitle}>댓글</h3>
              <form className={styles.commentForm} onSubmit={submitComment}>
                <input value={commentText} onChange={(event) => setCommentText(event.target.value)} placeholder="댓글을 입력하세요" />
                <button type="submit">등록</button>
              </form>
              <div className={styles.commentList}>
                {comments.length === 0 ? <p className={styles.emptyComment}>아직 댓글이 없습니다.</p> : null}
                {comments.map((comment) => (
                  <div key={comment.id} className={styles.commentItem}>
                    <strong>{comment.authorNickname || '익명'}</strong>
                    <span>{formatCommunityTime(comment.createdAt)}</span>
                    <p>{comment.content}</p>
                  </div>
                ))}
              </div>
            </Card>
          </>
        ) : null}
        {errorMessage ? <Card><p>{errorMessage}</p></Card> : null}
      </AppLayout>
    );
  }

  return (
    <AppLayout title={tab === 'UNIT' ? '우리 부대 게시판' : '커뮤니티'} headerAction={<button type="button" className={styles.edit} onClick={() => setShowComposer((prev) => !prev)}>✏️</button>}>
      <div className={styles.tabWrap}>
        {tabs.map((item) => (
          <button key={item.value} type="button" className={`${styles.tab} ${tab === item.value ? styles.active : ''}`} onClick={() => handleTab(item)}>{item.label}</button>
        ))}
      </div>
      {showComposer ? (
        <Card>
          <form className={styles.composer} onSubmit={submitPost}>
            <select value={newPost.category} onChange={(event) => setNewPost((prev) => ({ ...prev, category: event.target.value }))}>
              <option value="ALL">전체</option>
              <option value="UNIT">우리 부대</option>
            </select>
            <input value={newPost.title} onChange={(event) => setNewPost((prev) => ({ ...prev, title: event.target.value }))} placeholder="제목" />
            <textarea value={newPost.content} onChange={(event) => setNewPost((prev) => ({ ...prev, content: event.target.value }))} placeholder="내용을 입력하세요" rows={4} />
            <button type="submit">게시하기</button>
          </form>
        </Card>
      ) : null}
      {loading ? <Card><p>불러오는 중...</p></Card> : null}
      {!loading && visiblePosts.length === 0 ? <Card><p>등록된 게시글이 없습니다.</p></Card> : null}
      {visiblePosts.map((post, index) => (
        <Card key={post.id} className={styles.postCard} onClick={() => openPost(post)} role="button" tabIndex={0} onKeyDown={(event) => { if (event.key === 'Enter') openPost(post); }}>
          <div className={styles.postHead}>
            {tab === 'POPULAR' ? <strong className={styles.rank}>{index + 1}</strong> : <span className={styles.avatar}>🪖</span>}
            <p className={styles.user}>{post.authorNickname || '익명'} <span>{formatCommunityTime(post.createdAt)}</span></p>
          </div>
          <h3 className={styles.title}>{post.title || '제목 없음'}</h3>
          <p className={styles.content}>{post.content || ''}</p>
          <p className={styles.meta}>♡ {post.likeCount ?? 0} · 댓글 {post.commentCount ?? 0}</p>
        </Card>
      ))}
      <button type="button" className={styles.fab} onClick={() => setShowComposer((prev) => !prev)}>+</button>
      {errorMessage ? <Card><p>{errorMessage}</p></Card> : null}
    </AppLayout>
  );
}

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { createCommunityComment, createCommunityPost, getCommunityPostDetail, getCommunityPosts, likeCommunityPost } from '../../api/communityApi';
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
  const { postId } = useParams();
  const [tab, setTab] = useState(tabFromPath(location.pathname));
  const { state } = useAppContext();
  const [posts, setPosts] = useState([]);
  const [postDetail, setPostDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [showComposer, setShowComposer] = useState(false);
  const [postTitle, setPostTitle] = useState('');
  const [postContent, setPostContent] = useState('');
  const [postImageUrl, setPostImageUrl] = useState('');
  const [routineText, setRoutineText] = useState('');
  const [postCategory, setPostCategory] = useState(tab === 'UNIT' ? 'UNIT' : 'ALL');
  const [commentContent, setCommentContent] = useState('');
  const [suggestedRoutineText, setSuggestedRoutineText] = useState('');
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
    } catch {
      if (!isMounted()) return;
      setErrorMessage('게시글 데이터를 불러오지 못했습니다.');
      setPosts([]);
    } finally {
      if (isMounted()) setLoading(false);
    }
  }, [tab]);

  useEffect(() => {
    let mounted = true;
    if (postId) {
      setLoading(true);
      getCommunityPostDetail(postId)
        .then((detail) => {
          if (!mounted) return;
          setPostDetail(detail);
          setErrorMessage('');
        })
        .catch(() => {
          if (!mounted) return;
          setErrorMessage('게시글 상세를 불러오지 못했습니다.');
          setPostDetail(null);
        })
        .finally(() => mounted && setLoading(false));
    } else {
      setPostDetail(null);
      loadPosts(() => mounted);
    }
    return () => {
      mounted = false;
    };
  }, [loadPosts, postId]);

  useEffect(() => {
    setPostCategory(tab === 'UNIT' ? 'UNIT' : 'ALL');
  }, [tab]);

  const visiblePosts = useMemo(() => {
    if (tab === 'POPULAR') return [...posts].sort((a, b) => (b.likeCount ?? 0) - (a.likeCount ?? 0));
    if (tab === 'UNIT') return posts.filter((post) => !state.user?.unitName || !post.unitName || post.unitName === state.user.unitName);
    return posts;
  }, [posts, state.user?.unitName, tab]);

  const handleLike = async (targetPostId) => {
    const optimistic = (post) => post.id === targetPostId ? { ...post, likeCount: (post.likeCount ?? 0) + 1 } : post;
    setPosts((prev) => prev.map(optimistic));
    if (postDetail?.post?.id === targetPostId) {
      setPostDetail((prev) => ({ ...prev, post: optimistic(prev.post) }));
    }
    try {
      const updated = await likeCommunityPost(targetPostId);
      setPosts((prev) => prev.map((post) => (post.id === targetPostId ? updated : post)));
      if (postDetail?.post?.id === targetPostId) {
        setPostDetail((prev) => ({ ...prev, post: updated }));
      }
    } catch {
      setErrorMessage('좋아요 반영에 실패했습니다.');
    }
  };

  const handleTab = (item) => {
    setTab(item.value);
    navigate(item.path);
  };

  const closeComposer = () => {
    if (submitting) return;
    setShowComposer(false);
    setPostTitle('');
    setPostContent('');
    setPostImageUrl('');
    setRoutineText('');
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
        imageUrl: postImageUrl.trim() || null,
        routineText: routineText.trim() || null,
      });
      setPosts((prev) => [created, ...prev]);
      closeComposer();
      if (postCategory === 'UNIT' && tab !== 'UNIT') {
        navigate('/community/unit');
      }
    } catch (error) {
      setErrorMessage(error.message || '게시글 작성에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleSubmitComment = async (event) => {
    event.preventDefault();
    if (!postId || (!commentContent.trim() && !suggestedRoutineText.trim())) return;
    try {
      setSubmitting(true);
      const created = await createCommunityComment(postId, {
        content: commentContent.trim(),
        suggestedRoutineText: suggestedRoutineText.trim() || null,
      });
      setPostDetail((prev) => ({
        ...prev,
        post: { ...prev.post, commentCount: (prev.post.commentCount ?? 0) + 1 },
        comments: [...(prev.comments ?? []), created],
      }));
      setCommentContent('');
      setSuggestedRoutineText('');
    } catch (error) {
      setErrorMessage(error.message || '댓글 작성에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const renderPost = (post, index, detail = false) => (
    <Card key={post.id} className={detail ? styles.detailCard : ''} onClick={detail ? undefined : () => navigate(`/community/posts/${post.id}`)}>
      <div className={styles.postHead}>
        {tab === 'POPULAR' && !detail ? <strong className={styles.rank}>{index + 1}</strong> : <span className={styles.avatar}>🪖</span>}
        <p className={styles.user}>{post.authorNickname || '익명'} <span>{post.unitName || '전 부대'} · {post.createdAt || ''}</span></p>
      </div>
      <h3 className={styles.title}>{post.title || '제목 없음'}</h3>
      <p className={styles.content}>{post.content || ''}</p>
      {post.imageUrl ? <img className={styles.postImage} src={post.imageUrl} alt="게시글 이미지" /> : null}
      {post.routineText ? <pre className={styles.routineBox}>{post.routineText}</pre> : null}
      <div className={styles.metaRow}>
        <button type="button" onClick={(event) => { event.stopPropagation(); handleLike(post.id); }}>♡ {post.likeCount ?? 0}</button>
        <span>댓글 {post.commentCount ?? 0}</span>
      </div>
    </Card>
  );

  return (
    <AppLayout title={postId ? '게시글 상세' : (tab === 'UNIT' ? `${state.user?.unitName || '우리 부대'} 게시판` : '커뮤니티')} headerAction={!postId ? <button type="button" className={styles.edit} onClick={() => setShowComposer(true)} aria-label="게시글 작성">✏️</button> : <button type="button" className={styles.edit} onClick={() => navigate('/community')}>←</button>}>
      {!postId ? (
        <div className={styles.tabWrap}>
          {tabs.map((item) => (
            <button key={item.value} type="button" className={`${styles.tab} ${tab === item.value ? styles.active : ''}`} onClick={() => handleTab(item)}>{item.label}</button>
          ))}
        </div>
      ) : null}
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
            <input value={postImageUrl} onChange={(event) => setPostImageUrl(event.target.value)} placeholder="이미지 URL (선택)" />
            <textarea value={routineText} onChange={(event) => setRoutineText(event.target.value)} placeholder="공유할 운동 루틴 (선택)" rows={3} />
            <button type="submit" className={styles.submitButton} disabled={submitting}>{submitting ? '작성 중...' : '게시글 등록'}</button>
          </form>
        </Card>
      ) : null}
      {loading ? <Card><p>불러오는 중...</p></Card> : null}
      {postId && postDetail?.post ? (
        <>
          {renderPost(postDetail.post, 0, true)}
          <Card>
            <h3 className={styles.title}>댓글</h3>
            <form className={styles.composerForm} onSubmit={handleSubmitComment}>
              <textarea value={commentContent} onChange={(event) => setCommentContent(event.target.value)} placeholder="댓글을 입력하세요" rows={3} />
              <textarea value={suggestedRoutineText} onChange={(event) => setSuggestedRoutineText(event.target.value)} placeholder="추천 루틴 (선택)" rows={2} />
              <button type="submit" className={styles.submitButton} disabled={submitting}>댓글 등록</button>
            </form>
            <div className={styles.commentList}>
              {(postDetail.comments ?? []).map((comment) => (
                <article key={comment.id} className={styles.commentItem}>
                  <strong>{comment.authorNickname || '익명'}</strong>
                  <p>{comment.content}</p>
                  {comment.suggestedRoutineText ? <pre className={styles.routineBox}>{comment.suggestedRoutineText}</pre> : null}
                </article>
              ))}
            </div>
          </Card>
        </>
      ) : null}
      {!postId && !loading && visiblePosts.length === 0 ? <Card><p>등록된 게시글이 없습니다.</p></Card> : null}
      {!postId ? visiblePosts.map((post, index) => renderPost(post, index)) : null}
      {!postId ? <button type="button" className={styles.fab} onClick={() => setShowComposer(true)} aria-label="게시글 작성">+</button> : null}
      {errorMessage ? <Card><p>{errorMessage}</p></Card> : null}
    </AppLayout>
  );
}

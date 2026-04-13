import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MobileShell from '../components/layout/MobileShell';
import {
  createCommunityComment,
  createCommunityPost,
  getCommunityPostDetail,
  getCommunityPosts,
} from '../api/communityApi';
import { getTodayWorkoutRecommendation } from '../api/workoutApi';
import styles from '../features/home/HomeCards.module.css';

function categoryLabel(category) {
  return category === 'UNIT' ? '부대별' : '전체';
}

export default function CommunityPage() {
  const navigate = useNavigate();
  const [category, setCategory] = useState('ALL');
  const [posts, setPosts] = useState([]);
  const [selectedPost, setSelectedPost] = useState(null);
  const [routineOptions, setRoutineOptions] = useState([]);
  const [postForm, setPostForm] = useState({
    title: '',
    content: '',
    imageUrl: '',
    routineText: '',
  });
  const [commentForm, setCommentForm] = useState({
    content: '',
    suggestedRoutineText: '',
  });

  const routineTemplate = useMemo(() => {
    if (!routineOptions.length) return '';
    return routineOptions
      .map((exercise, idx) => `${idx + 1}. ${exercise.name} ${exercise.sets}세트 ${exercise.reps}`)
      .join('\n');
  }, [routineOptions]);

  const loadPosts = async (currentCategory) => {
    const rows = await getCommunityPosts(currentCategory).catch(() => []);
    setPosts(rows || []);
    if ((rows || []).length) {
      const detail = await getCommunityPostDetail(rows[0].id).catch(() => null);
      setSelectedPost(detail);
    } else {
      setSelectedPost(null);
    }
  };

  useEffect(() => {
    loadPosts(category);
  }, [category]);

  useEffect(() => {
    (async () => {
      const todayWorkout = await getTodayWorkoutRecommendation().catch(() => null);
      setRoutineOptions(todayWorkout?.exercises || []);
    })();
  }, []);

  const onCreatePost = async (e) => {
    e.preventDefault();
    await createCommunityPost({
      category,
      title: postForm.title,
      content: postForm.content,
      imageUrl: postForm.imageUrl,
      routineText: postForm.routineText,
    });
    setPostForm({ title: '', content: '', imageUrl: '', routineText: '' });
    await loadPosts(category);
  };

  const onSelectPost = async (postId) => {
    const detail = await getCommunityPostDetail(postId).catch(() => null);
    setSelectedPost(detail);
  };

  const onCreateComment = async (e) => {
    e.preventDefault();
    if (!selectedPost?.post?.id) return;
    await createCommunityComment(selectedPost.post.id, commentForm);
    setCommentForm({ content: '', suggestedRoutineText: '' });
    await onSelectPost(selectedPost.post.id);
  };

  return (
    <MobileShell
      title="커뮤니티"
      actions={<button className={styles.profileButton} onClick={() => navigate('/')}>홈</button>}
    >
      <section className={styles.card}>
        <div className={styles.headerActions}>
          <button className={category === 'ALL' ? styles.doneButton : styles.todoButton} onClick={() => setCategory('ALL')}>전체</button>
          <button className={category === 'UNIT' ? styles.doneButton : styles.todoButton} onClick={() => setCategory('UNIT')}>부대별</button>
        </div>
        <p className={styles.muted}>현재 카테고리: {categoryLabel(category)}</p>
      </section>

      <section className={styles.card}>
        <h3>게시글 작성</h3>
        <form onSubmit={onCreatePost}>
          <input className={styles.dateButton} style={{ width: '100%', marginBottom: 8 }} placeholder="제목" value={postForm.title} onChange={(e) => setPostForm((prev) => ({ ...prev, title: e.target.value }))} />
          <textarea className={styles.dateButton} style={{ width: '100%', minHeight: 80, marginBottom: 8 }} placeholder="내용" value={postForm.content} onChange={(e) => setPostForm((prev) => ({ ...prev, content: e.target.value }))} />
          <input className={styles.dateButton} style={{ width: '100%', marginBottom: 8 }} placeholder="이미지 URL" value={postForm.imageUrl} onChange={(e) => setPostForm((prev) => ({ ...prev, imageUrl: e.target.value }))} />
          <textarea className={styles.dateButton} style={{ width: '100%', minHeight: 80, marginBottom: 8 }} placeholder="운동루틴 텍스트" value={postForm.routineText} onChange={(e) => setPostForm((prev) => ({ ...prev, routineText: e.target.value }))} />
          <button type="button" className={styles.startButton} onClick={() => setPostForm((prev) => ({ ...prev, routineText: routineTemplate }))}>오늘 루틴 불러오기</button>
          <button type="submit" className={styles.submitButton || styles.startButton} style={{ marginLeft: 8 }}>게시글 올리기</button>
        </form>
      </section>

      <section className={styles.card}>
        <h3>게시글 목록</h3>
        <div className={styles.exerciseList}>
          {posts.map((post) => (
            <article key={post.id} className={styles.exerciseCard} onClick={() => onSelectPost(post.id)}>
              <div>
                <p className={styles.exerciseTitle}>[{categoryLabel(post.category)}] {post.title}</p>
                <p className={styles.exerciseMeta}>{post.authorNickname} · 댓글 {post.commentCount}</p>
              </div>
            </article>
          ))}
        </div>
      </section>

      {selectedPost ? (
        <section className={styles.card}>
          <h3>{selectedPost.post.title}</h3>
          <p className={styles.item}>{selectedPost.post.content}</p>
          {selectedPost.post.imageUrl ? <p className={styles.muted}>이미지: {selectedPost.post.imageUrl}</p> : null}
          {selectedPost.post.routineText ? <pre className={styles.subInfoBox}>{selectedPost.post.routineText}</pre> : null}

          <div className={styles.subInfoBox}>
            <p className={styles.subInfoTitle}>루틴 피드백 댓글</p>
            {(selectedPost.comments || []).map((comment) => (
              <article key={comment.id} className={styles.item}>
                <strong>{comment.authorNickname}</strong>: {comment.content}
                {comment.suggestedRoutineText ? <pre className={styles.muted}>{comment.suggestedRoutineText}</pre> : null}
              </article>
            ))}

            <form onSubmit={onCreateComment}>
              <textarea className={styles.dateButton} style={{ width: '100%', minHeight: 70, marginBottom: 8 }} placeholder="피드백 댓글" value={commentForm.content} onChange={(e) => setCommentForm((prev) => ({ ...prev, content: e.target.value }))} />
              <textarea className={styles.dateButton} style={{ width: '100%', minHeight: 70, marginBottom: 8 }} placeholder="추천 루틴(선택)" value={commentForm.suggestedRoutineText} onChange={(e) => setCommentForm((prev) => ({ ...prev, suggestedRoutineText: e.target.value }))} />
              <button type="button" className={styles.startButton} onClick={() => setCommentForm((prev) => ({ ...prev, suggestedRoutineText: routineTemplate }))}>오늘 루틴 추천으로 채우기</button>
              <button type="submit" className={styles.startButton} style={{ marginLeft: 8 }}>댓글 등록</button>
            </form>
          </div>
        </section>
      ) : null}
    </MobileShell>
  );
}

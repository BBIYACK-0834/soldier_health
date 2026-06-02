import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { mockPosts, mockUser } from '../../constants/mockData';
import styles from './MyPage.module.css';

export default function MyPostsPage() {
  const posts = mockPosts.filter((post) => post.authorId !== mockUser.id).slice(0, 2);
  return (
    <AppLayout title="내 게시글" showBottomNav={false}>
      {posts.map((post) => (
        <Card key={post.id}>
          <h3 className={styles.sectionTitle}>{post.title}</h3>
          <p>{post.createdAt} · ♡ {post.likeCount} · 댓글 {post.commentCount}</p>
        </Card>
      ))}
    </AppLayout>
  );
}

import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';

export default function MyPostsPage() {
  const posts = [];
  return (
    <AppLayout title="내 게시글" showBottomNav={false}>
      {posts.length === 0 ? (
        <Card><p>작성한 게시글이 없습니다.</p></Card>
      ) : posts.map((post) => (
        <Card key={post.id}>
          <h3>{post.title}</h3>
          <p>{post.createdAt || ''} · ♡ {post.likeCount ?? 0} · 댓글 {post.commentCount ?? 0}</p>
        </Card>
      ))}
    </AppLayout>
  );
}

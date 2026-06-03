import httpClient, { unwrap } from './httpClient';

export async function getCommunityPosts(category = 'ALL') {
  const response = await httpClient.get('/api/community/posts', {
    params: { category },
  });
  return unwrap(response);
}

export async function createCommunityPost(payload) {
  const response = await httpClient.post('/api/community/posts', payload);
  return unwrap(response);
}

export async function getCommunityPostDetail(postId) {
  const response = await httpClient.get(`/api/community/posts/${postId}`);
  return unwrap(response);
}

export async function createCommunityComment(postId, payload) {
  const response = await httpClient.post(`/api/community/posts/${postId}/comments`, payload);
  return unwrap(response);
}

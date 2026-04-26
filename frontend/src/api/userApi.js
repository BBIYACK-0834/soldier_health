import httpClient, { unwrap } from './httpClient';

export async function getMyProfile() {
  const response = await httpClient.get('/api/users/me');
  return unwrap(response);
}

export async function updateProfile(payload) {
  const response = await httpClient.put('/api/users/me/profile', payload);
  return unwrap(response);
}

export async function updateGoals(payload) {
  const response = await httpClient.put('/api/users/me/goals', payload);
  return unwrap(response);
}

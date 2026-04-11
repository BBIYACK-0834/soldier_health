import httpClient, { unwrap } from './httpClient';

export async function updateProfile(payload) {
  const response = await httpClient.put('/api/users/me/profile', payload);
  return unwrap(response);
}

export async function updateGoals(payload) {
  const response = await httpClient.put('/api/users/me/goals', payload);
  return unwrap(response);
}

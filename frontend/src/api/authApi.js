import httpClient, { unwrap } from './httpClient';

export async function signup(payload) {
  const response = await httpClient.post('/api/auth/signup', payload);
  return unwrap(response);
}

export async function login(payload) {
  const response = await httpClient.post('/api/auth/login', payload);
  return unwrap(response);
}

export async function getMe() {
  const response = await httpClient.get('/api/auth/me');
  return unwrap(response);
}

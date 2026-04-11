import httpClient, { unwrap } from './httpClient';

export async function signup(payload) {
  const response = await httpClient.post('/api/auth/signup', {
    email: payload.email,
    password: payload.password,
    nickname: payload.nickname,
    goalType: payload.goalType,
    workoutLevel: payload.workoutLevel,
    branchType: payload.branchType,
  });

  return unwrap(response);
}

export async function login(payload) {
  const response = await httpClient.post('/api/auth/login', {
    email: payload.email,
    password: payload.password,
  });

  return unwrap(response);
}

export async function getMe() {
  const response = await httpClient.get('/api/auth/me');
  return unwrap(response);
}

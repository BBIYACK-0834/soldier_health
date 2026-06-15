import httpClient, { unwrap } from './httpClient';

export async function signup(payload) {
  if (payload.profileImageFile) {
    const formData = new FormData();
    formData.append('email', payload.email.trim());
    formData.append('password', payload.password);
    formData.append('nickname', payload.nickname);
    formData.append('profileImageUrl', payload.profileImageUrl || '');
    formData.append('profileImageFile', payload.profileImageFile);

    const response = await httpClient.post('/api/auth/signup', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    return unwrap(response);
  }

  const response = await httpClient.post('/api/auth/signup', {
    email: payload.email.trim(),
    password: payload.password,
    nickname: payload.nickname,
    profileImageUrl: payload.profileImageUrl,
  });

  return unwrap(response);
}

export async function login(payload) {
  const response = await httpClient.post('/api/auth/login', {
    email: payload.email.trim(),
    password: payload.password,
  });

  return unwrap(response);
}

export async function getMe() {
  const response = await httpClient.get('/api/auth/me');
  return unwrap(response);
}

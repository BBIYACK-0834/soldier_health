import httpClient, { unwrap } from './httpClient';

export async function getMyProfile() {
  const response = await httpClient.get('/api/users/me');
  return unwrap(response);
}

export async function updateProfile(payload) {
  const response = await httpClient.put('/api/users/me/profile', payload);
  return unwrap(response);
}

export async function uploadProfileImage(file) {
  const formData = new FormData();
  formData.append('file', file);

  const response = await httpClient.post('/api/users/me/profile-image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });

  return unwrap(response);
}

export async function updateGoals(payload) {
  const response = await httpClient.put('/api/users/me/goals', payload);
  return unwrap(response);
}

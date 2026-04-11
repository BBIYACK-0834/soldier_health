import httpClient, { unwrap } from './httpClient';

export async function getEquipments() {
  const response = await httpClient.get('/api/equipments');
  return unwrap(response);
}

export async function saveMyEquipments(payload) {
  const response = await httpClient.post('/api/users/me/equipments', payload);
  return unwrap(response);
}

export async function getMyEquipments() {
  const response = await httpClient.get('/api/users/me/equipments');
  return unwrap(response);
}

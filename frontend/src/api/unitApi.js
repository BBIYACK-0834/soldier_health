import httpClient, { unwrap } from './httpClient';

export async function getUnits() {
  const response = await httpClient.get('/api/units');
  return unwrap(response);
}

export async function setMyUnit(unitId) {
  const response = await httpClient.post('/api/users/me/unit', { unitId });
  return unwrap(response);
}

export async function getMyUnit() {
  const response = await httpClient.get('/api/users/me/unit');
  return unwrap(response);
}

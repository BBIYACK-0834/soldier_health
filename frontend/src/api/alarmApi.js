import httpClient, { unwrap } from './httpClient';

export async function getMyAlarms() {
  const response = await httpClient.get('/api/alarms/me');
  return unwrap(response);
}

export async function createAlarm(payload) {
  const response = await httpClient.post('/api/alarms', payload);
  return unwrap(response);
}

export async function updateAlarm(id, payload) {
  const response = await httpClient.put(`/api/alarms/${id}`, payload);
  return unwrap(response);
}

export async function deleteAlarm(id) {
  const response = await httpClient.delete(`/api/alarms/${id}`);
  return unwrap(response);
}

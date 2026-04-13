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

export async function getUnitGymDatasets(unitId) {
  const response = await httpClient.get(`/api/units/${unitId}/gym-datasets`);
  return unwrap(response);
}

export async function createUnitGymDataset(unitId, payload) {
  const response = await httpClient.post(`/api/units/${unitId}/gym-datasets`, payload);
  return unwrap(response);
}

export async function applyGymDataset(datasetId) {
  const response = await httpClient.post(`/api/users/me/equipments/apply-dataset/${datasetId}`);
  return unwrap(response);
}

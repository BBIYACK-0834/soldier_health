import httpClient, { unwrap } from './httpClient';

export async function getUnits() {
  const response = await httpClient.get('/api/units');
  return unwrap(response);
}

export async function searchUnits(keyword) {
  const response = await httpClient.get('/api/units/search', {
    params: { keyword },
  });
  return unwrap(response);
}

export async function findUnitsByMeal(payload) {
  const response = await httpClient.post('/api/units/match-by-meal', payload);
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


export async function getMealOptionsByDate({ date, mealType, keyword }) {
  const response = await httpClient.get('/api/units/meal-options', {
    params: { date, mealType, keyword },
  });
  return unwrap(response);
}

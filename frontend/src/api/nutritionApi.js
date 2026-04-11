import httpClient, { unwrap } from './httpClient';

export async function getTodayNutrition() {
  const response = await httpClient.get('/api/nutrition/today');
  return unwrap(response);
}

export async function getTodayNutritionRecommendation() {
  const response = await httpClient.get('/api/nutrition/recommendation/today');
  return unwrap(response);
}

export async function getOwnedFoods() {
  const response = await httpClient.get('/api/users/me/owned-foods');
  return unwrap(response);
}

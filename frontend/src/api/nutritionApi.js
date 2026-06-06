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

export async function getTodayMealNutritionDetails() {
  const response = await httpClient.get('/api/nutrition/today/meals');
  return unwrap(response);
}

export async function searchFoods(query) {
  const response = await httpClient.get('/api/foods/search', { params: { q: query } });
  return unwrap(response);
}

export async function addMealFoods(mealType, foodIds) {
  const response = await httpClient.post('/api/users/me/meal-foods', { mealType, foodIds });
  return unwrap(response);
}

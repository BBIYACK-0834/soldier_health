import httpClient, { unwrap } from './httpClient';

let todayOverviewRequest = null;

export async function getTodayNutrition() {
  const response = await httpClient.get('/api/nutrition/today');
  return unwrap(response);
}

export async function getTodayNutritionOverview() {
  if (!todayOverviewRequest) {
    todayOverviewRequest = httpClient.get('/api/nutrition/today/overview')
      .then(unwrap)
      .finally(() => {
        todayOverviewRequest = null;
      });
  }
  return todayOverviewRequest;
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

export async function saveTodayMealConsumption(mealType, portionMultiplier) {
  const response = await httpClient.put('/api/users/me/meal-consumption/today', { mealType, portionMultiplier });
  return unwrap(response);
}

export async function searchFoods(query) {
  const response = await httpClient.get('/api/foods/search', { params: { q: query } });
  return unwrap(response);
}

export async function addMealFoods(mealType, foodIds, servingGramByFoodId = {}) {
  const response = await httpClient.post('/api/users/me/meal-foods', { mealType, foodIds, servingGramByFoodId });
  return unwrap(response);
}

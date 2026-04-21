import httpClient, { unwrap } from './httpClient';

export async function getTodayMeal() {
  const response = await httpClient.get('/api/user/meals/today');
  return unwrap(response);
}

export async function getMealByDate(date) {
  const response = await httpClient.get('/api/user/meals', {
    params: { date },
  });
  return unwrap(response);
}

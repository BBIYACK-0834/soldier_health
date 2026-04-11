import httpClient, { unwrap } from './httpClient';

export async function getTodayMeal() {
  const response = await httpClient.get('/api/meals/today');
  return unwrap(response);
}

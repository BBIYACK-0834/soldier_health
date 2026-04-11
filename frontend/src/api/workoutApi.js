import httpClient, { unwrap } from './httpClient';

export async function getTodayWorkoutRecommendation() {
  const response = await httpClient.get('/api/workouts/recommendation/today');
  return unwrap(response);
}

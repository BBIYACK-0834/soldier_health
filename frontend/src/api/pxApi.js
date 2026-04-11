import httpClient, { unwrap } from './httpClient';

export async function getPxProducts() {
  const response = await httpClient.get('/api/px-products');
  return unwrap(response);
}

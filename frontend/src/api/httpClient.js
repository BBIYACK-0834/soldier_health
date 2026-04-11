import axios from 'axios';

const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 10000,
});

httpClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('tg_access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export function unwrap(response) {
  return response.data?.data;
}

export default httpClient;

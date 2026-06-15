import axios from 'axios';

export const ACCESS_TOKEN_KEY = 'tg_access_token';

const baseURL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const authPaths = ['/api/auth/login', '/api/auth/signup'];

function isAuthRequest(url = '') {
  return authPaths.some((path) => url.endsWith(path));
}

const httpClient = axios.create({
  baseURL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

httpClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);

  if (isAuthRequest(config.url)) {
    if (typeof config.headers?.delete === 'function') {
      config.headers.delete('Authorization');
    } else if (config.headers) {
      delete config.headers.Authorization;
    }
    return config;
  }

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

function mapHttpError(error) {
  if (error.response) {
    const status = error.response.status;
    const apiMessage = error.response.data?.error || error.response.data?.message;
    const fallbackMessage = status === 401
      ? '로그인이 필요하거나 세션이 만료되었습니다. 다시 로그인해주세요.'
      : `요청 처리 중 오류가 발생했습니다. (HTTP ${status})`;

    return {
      status,
      message: apiMessage || fallbackMessage,
      code: 'HTTP_ERROR',
      details: error.response.data,
    };
  }

  if (error.request) {
    return {
      status: null,
      message: '서버에 연결할 수 없습니다. 백엔드 실행/CORS/API 주소를 확인해주세요.',
      code: 'NETWORK_ERROR',
      details: null,
    };
  }

  return {
    status: null,
    message: error.message || '알 수 없는 오류가 발생했습니다.',
    code: 'UNKNOWN_ERROR',
    details: null,
  };
}

httpClient.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(mapHttpError(error))
);

export function unwrap(response) {
  if (response.data?.success === false) {
    const error = {
      status: response.status,
      message: response.data?.error || '요청이 실패했습니다.',
      code: 'API_ERROR',
      details: response.data,
    };

    throw error;
  }

  return response.data?.data;
}

export default httpClient;

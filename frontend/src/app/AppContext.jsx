import { createContext, useContext, useEffect, useMemo, useReducer } from 'react';
import { ACCESS_TOKEN_KEY } from '../api/httpClient';

const AppContext = createContext(null);

const initialState = {
  token: localStorage.getItem(ACCESS_TOKEN_KEY) || null,
  user: null,
};

function reducer(state, action) {
  switch (action.type) {
    case 'SET_AUTH':
      return {
        ...state,
        token: action.payload.token,
        user: action.payload.user,
      };
    case 'SET_USER':
      return {
        ...state,
        user: action.payload,
      };
    case 'LOGOUT':
      return {
        token: null,
        user: null,
      };
    default:
      return state;
  }
}

function normalizeAuthPayload(data) {
  if (!data) {
    return { token: null, user: null };
  }

  const token = data.accessToken || data.token || null;
  const rawUser = data.user || data;

  const user = rawUser
    ? {
        id: rawUser.id ?? data.userId ?? null,
        email: rawUser.email ?? null,
        nickname: rawUser.nickname ?? null,
      }
    : null;

  return { token, user };
}

export function AppProvider({ children }) {
  const [state, dispatch] = useReducer(reducer, initialState);

  useEffect(() => {
    if (state.token) {
      localStorage.setItem(ACCESS_TOKEN_KEY, state.token);
      return;
    }

    localStorage.removeItem(ACCESS_TOKEN_KEY);
  }, [state.token]);

  const actions = useMemo(
    () => ({
      setAuth: (authData) => {
        const normalized = normalizeAuthPayload(authData);
        dispatch({ type: 'SET_AUTH', payload: normalized });
      },
      setUser: (user) => dispatch({ type: 'SET_USER', payload: user }),
      logout: () => dispatch({ type: 'LOGOUT' }),
    }),
    []
  );

  return <AppContext.Provider value={{ state, actions }}>{children}</AppContext.Provider>;
}

export function useAppContext() {
  const context = useContext(AppContext);
  if (!context) throw new Error('useAppContext must be used within AppProvider');
  return context;
}

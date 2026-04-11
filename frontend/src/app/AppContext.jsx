import { createContext, useContext, useEffect, useMemo, useReducer } from 'react';

const AppContext = createContext(null);

const initialState = {
  token: localStorage.getItem('tg_access_token') || null,
  user: null,
};

function reducer(state, action) {
  switch (action.type) {
    case 'SET_AUTH':
      return { ...state, token: action.payload.token, user: action.payload.user };
    case 'SET_USER':
      return { ...state, user: action.payload };
    case 'LOGOUT':
      return { token: null, user: null };
    default:
      return state;
  }
}

export function AppProvider({ children }) {
  const [state, dispatch] = useReducer(reducer, initialState);

  useEffect(() => {
    if (state.token) {
      localStorage.setItem('tg_access_token', state.token);
    } else {
      localStorage.removeItem('tg_access_token');
    }
  }, [state.token]);

  const actions = useMemo(
    () => ({
      setAuth: (token, user) => dispatch({ type: 'SET_AUTH', payload: { token, user } }),
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

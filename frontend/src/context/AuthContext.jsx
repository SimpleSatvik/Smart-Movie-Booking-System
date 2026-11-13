import React, { createContext, useContext, useState, useEffect } from 'react';
import { api } from '../services/api';

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('cinesync_user');
    if (saved) {
      try { return JSON.parse(saved); } catch (e) { /* ignore */ }
    }
    return null;
  });

  const [token, setToken] = useState(() => localStorage.getItem('cinesync_token') || '');

  useEffect(() => {
    if (user) {
      localStorage.setItem('cinesync_user', JSON.stringify(user));
    } else {
      localStorage.removeItem('cinesync_user');
    }
  }, [user]);

  useEffect(() => {
    if (token) {
      localStorage.setItem('cinesync_token', token);
    } else {
      localStorage.removeItem('cinesync_token');
    }
  }, [token]);

  // Initial sync: if user exists but has a placeholder token, acquire genuine backend token
  useEffect(() => {
    const syncRealToken = async () => {
      if (!user) return;
      const currentToken = localStorage.getItem('cinesync_token');
      if (!currentToken || currentToken.startsWith('demo_') || currentToken === 'jwt_token') {
        try {
          const res = await api.demoLogin(user?.role || 'ROLE_USER');
          if (res && res.token) {
            setToken(res.token);
            localStorage.setItem('cinesync_token', res.token);
          }
        } catch (e) {
          // Keep local fallback in case backend is offline
        }
      }
    };
    syncRealToken();
  }, [user]);

  const loginUser = (authData) => {
    const normalizedUser = {
      id: authData.id || authData.userId || 1,
      userId: authData.userId || authData.id || 1,
      name: authData.name || 'User',
      email: authData.email || authData.gmail || 'user@example.com',
      gmail: authData.gmail || authData.email || 'user@example.com',
      role: authData.role || 'ROLE_USER',
    };
    setUser(normalizedUser);
    setToken(authData.token || 'jwt_token');
  };

  const loginWithToken = (jwtToken, profile) => {
    const normalizedUser = {
      id: profile.id || profile.userId || 1,
      userId: profile.userId || profile.id || 1,
      name: profile.name || 'User',
      email: profile.email || profile.gmail || 'user@example.com',
      gmail: profile.gmail || profile.email || 'user@example.com',
      role: profile.role?.startsWith('ROLE_') ? profile.role : `ROLE_${profile.role || 'USER'}`,
    };
    setUser(normalizedUser);
    setToken(jwtToken);
  };

  const logout = () => {
    setUser(null);
    setToken('');
    localStorage.removeItem('cinesync_user');
    localStorage.removeItem('cinesync_token');
  };

  // Authenticate as a demo profile (makes real backend API call to retrieve genuine signed token)
  const authenticateAsDemo = async (role) => {
    try {
      const res = await api.demoLogin(role);
      if (res && res.token) {
        loginWithToken(res.token, {
          id: res.userId,
          name: res.name,
          email: res.gmail,
          role: res.role,
        });
        return res;
      }
    } catch (err) {
      console.warn('Backend demo-login sync notice:', err);
    }
  };

  return (
    <AuthContext.Provider value={{ 
      user, 
      token, 
      loginUser, 
      loginWithToken, 
      logout, 
      authenticateAsDemo,
      isAuthenticated: !!token 
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

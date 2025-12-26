import type { ReactNode } from 'react';
import { createContext, useContext, useState, useEffect } from 'react';
import type { User, UserSession, LoginRequest, RegisterRequest, LoginResponse, RegisterResponse, GoogleAuthResponse, GoogleCompleteRegistrationRequest } from '../types';
import apiClient from '../api/client';

/**
 * Extracts minimal non-sensitive session data from a User object.
 * Only this data should be stored in localStorage to avoid PII exposure.
 */
function toUserSession(user: User): UserSession {
  return {
    id: user.id,
    role: user.role,
    profileComplete: user.profileComplete,
    googleLinked: user.googleLinked,
  };
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<RegisterResponse>;
  logout: () => Promise<void>;
  loginWithGoogle: (idToken: string) => Promise<GoogleAuthResponse>;
  completeGoogleRegistration: (data: GoogleCompleteRegistrationRequest) => Promise<void>;
  linkGoogleAccount: (idToken: string) => Promise<void>;
  unlinkGoogleAccount: () => Promise<void>;
  refreshUser: () => Promise<void>;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const validateSession = async () => {
      // Migration: remove old 'user' key (contained PII like email/name)
      if (localStorage.getItem('user')) {
        localStorage.removeItem('user');
      }

      const token = localStorage.getItem('accessToken');
      const storedSession = localStorage.getItem('userSession');

      if (!token || !storedSession) {
        setLoading(false);
        return;
      }

      try {
        // Validate the token by fetching current user info (full data in memory only)
        const response = await apiClient.get<User>('/auth/me');
        setUser(response.data);
        // Update stored session with minimal non-sensitive data only
        localStorage.setItem('userSession', JSON.stringify(toUserSession(response.data)));
      } catch {
        // Token is invalid or expired, clear local storage
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userSession');
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    validateSession();
  }, []);

  const login = async (data: LoginRequest) => {
    const response = await apiClient.post<LoginResponse>('/auth/login', data);
    const { accessToken, refreshToken, user } = response.data;
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    // Store only minimal non-sensitive session data (no email/name)
    localStorage.setItem('userSession', JSON.stringify(toUserSession(user)));
    setUser(user);
  };

  const register = async (data: RegisterRequest): Promise<RegisterResponse> => {
    const response = await apiClient.post<RegisterResponse>('/auth/register', data);
    return response.data;
  };

  const logout = async () => {
    const refreshToken = localStorage.getItem('refreshToken');
    if (refreshToken) {
      try {
        await apiClient.post('/auth/logout', { refreshToken });
      } catch {
        // Ignore errors on logout
      }
    }
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userSession');
    setUser(null);
  };

  const loginWithGoogle = async (idToken: string): Promise<GoogleAuthResponse> => {
    const response = await apiClient.post<GoogleAuthResponse>('/auth/google', { idToken });
    const data = response.data;

    // If existing user, store tokens and set user
    if (!data.newUser && data.accessToken && data.refreshToken && data.user) {
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);
      // Store only minimal non-sensitive session data (no email/name)
      localStorage.setItem('userSession', JSON.stringify(toUserSession(data.user)));
      setUser(data.user);
    }

    return data;
  };

  const completeGoogleRegistration = async (data: GoogleCompleteRegistrationRequest): Promise<void> => {
    const response = await apiClient.post<LoginResponse>('/auth/google/complete-registration', data);
    const { accessToken, refreshToken, user } = response.data;
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    // Store only minimal non-sensitive session data (no email/name)
    localStorage.setItem('userSession', JSON.stringify(toUserSession(user)));
    setUser(user);
  };

  const linkGoogleAccount = async (idToken: string): Promise<void> => {
    await apiClient.post('/auth/google/link', { idToken });
    // Refresh user data to get updated googleLinked status
    await refreshUser();
  };

  const unlinkGoogleAccount = async (): Promise<void> => {
    await apiClient.delete('/auth/google/unlink');
    // Refresh user data to get updated googleLinked status
    await refreshUser();
  };

  const refreshUser = async (): Promise<void> => {
    try {
      const response = await apiClient.get<User>('/auth/me');
      setUser(response.data);
      // Store only minimal non-sensitive session data (no email/name)
      localStorage.setItem('userSession', JSON.stringify(toUserSession(response.data)));
    } catch {
      // Ignore errors
    }
  };

  return (
    <AuthContext.Provider value={{
      user,
      loading,
      login,
      register,
      logout,
      loginWithGoogle,
      completeGoogleRegistration,
      linkGoogleAccount,
      unlinkGoogleAccount,
      refreshUser,
      isAuthenticated: !!user,
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

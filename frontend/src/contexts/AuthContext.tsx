import type { ReactNode } from 'react';
import { createContext, useContext, useState, useEffect } from 'react';
import type { User, LoginRequest, RegisterRequest, LoginResponse, RegisterResponse, GoogleAuthResponse, GoogleCompleteRegistrationRequest } from '../types';
import apiClient from '../api/client';

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
      const token = localStorage.getItem('accessToken');
      const storedUser = localStorage.getItem('user');

      if (!token || !storedUser) {
        setLoading(false);
        return;
      }

      try {
        // Validate the token by fetching current user info
        const response = await apiClient.get<User>('/auth/me');
        setUser(response.data);
        // Update stored user in case it changed
        localStorage.setItem('user', JSON.stringify(response.data));
      } catch {
        // Token is invalid or expired, clear local storage
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
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
    localStorage.setItem('user', JSON.stringify(user));
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
    localStorage.removeItem('user');
    setUser(null);
  };

  const loginWithGoogle = async (idToken: string): Promise<GoogleAuthResponse> => {
    const response = await apiClient.post<GoogleAuthResponse>('/auth/google', { idToken });
    const data = response.data;

    // If existing user, store tokens and set user
    if (!data.newUser && data.accessToken && data.refreshToken && data.user) {
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);
      localStorage.setItem('user', JSON.stringify(data.user));
      setUser(data.user);
    }

    return data;
  };

  const completeGoogleRegistration = async (data: GoogleCompleteRegistrationRequest): Promise<void> => {
    const response = await apiClient.post<LoginResponse>('/auth/google/complete-registration', data);
    const { accessToken, refreshToken, user } = response.data;
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('user', JSON.stringify(user));
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
      localStorage.setItem('user', JSON.stringify(response.data));
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

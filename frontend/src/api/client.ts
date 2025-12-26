import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const apiClient = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  // Enable sending cookies with requests for httpOnly token authentication
  withCredentials: true,
});

// Request interceptor - tokens are now in httpOnly cookies, no need to add Authorization header
apiClient.interceptors.request.use((config) => {
  // Backward compatibility: if localStorage token exists (migration period), use it
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // Try to refresh using cookies (preferred) - cookies are sent automatically with withCredentials
        // Also check localStorage for backward compatibility during migration
        const refreshToken = localStorage.getItem('refreshToken');
        const refreshPayload = refreshToken ? { refreshToken } : undefined;

        const response = await axios.post(
          `${API_URL}/auth/refresh`,
          refreshPayload,
          { withCredentials: true }
        );

        // If localStorage tokens exist (migration period), update them
        if (refreshToken && response.data.accessToken) {
          localStorage.setItem('accessToken', response.data.accessToken);
          if (response.data.refreshToken) {
            localStorage.setItem('refreshToken', response.data.refreshToken);
          }
          originalRequest.headers.Authorization = `Bearer ${response.data.accessToken}`;
        }

        return apiClient(originalRequest);
      } catch {
        // Clear any localStorage session data
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userSession');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;

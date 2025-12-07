import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { Header } from './components';
import { HomePage, LoginPage, RegisterPage, PetListPage, PetDetailPage, RescuesPage, ForgotPasswordPage, HelpCenterPage, ContactPage, PrivacyPolicyPage } from './pages';
import {
  FosterDashboard,
  AdopterDashboard,
  RescueDashboard,
  VetDashboard,
  AdminDashboard,
} from './pages/dashboards';
import type { ReactNode } from 'react';

interface ProtectedRouteProps {
  children: ReactNode;
  allowedRoles?: string[];
}

function ProtectedRoute({ children, allowedRoles }: ProtectedRouteProps) {
  const { isAuthenticated, user, loading } = useAuth();

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && user && !allowedRoles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}

function AppRoutes() {
  return (
    <div className="min-h-screen flex flex-col">
      <Header />
      <main className="flex-1">
        <Routes>
          {/* Public Routes */}
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/pets" element={<PetListPage />} />
          <Route path="/pets/:id" element={<PetDetailPage />} />
          <Route path="/rescues" element={<RescuesPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/help" element={<HelpCenterPage />} />
          <Route path="/contact" element={<ContactPage />} />
          <Route path="/privacy" element={<PrivacyPolicyPage />} />

          {/* Foster Routes */}
          <Route
            path="/foster/dashboard"
            element={
              <ProtectedRoute allowedRoles={['FOSTER']}>
                <FosterDashboard />
              </ProtectedRoute>
            }
          />

          {/* Adopter Routes */}
          <Route
            path="/adopter/dashboard"
            element={
              <ProtectedRoute allowedRoles={['ADOPTER']}>
                <AdopterDashboard />
              </ProtectedRoute>
            }
          />

          {/* Rescue Organization Routes */}
          <Route
            path="/rescue/dashboard"
            element={
              <ProtectedRoute allowedRoles={['RESCUE_ORG']}>
                <RescueDashboard />
              </ProtectedRoute>
            }
          />

          {/* Vet Routes */}
          <Route
            path="/vet/dashboard"
            element={
              <ProtectedRoute allowedRoles={['VET']}>
                <VetDashboard />
              </ProtectedRoute>
            }
          />

          {/* Admin Routes */}
          <Route
            path="/admin/dashboard"
            element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <AdminDashboard />
              </ProtectedRoute>
            }
          />

          {/* 404 */}
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
    </div>
  );
}

function NotFoundPage() {
  return (
    <div className="container-app py-16 text-center">
      <div className="text-6xl mb-4">🔍</div>
      <h1 className="text-3xl font-bold text-gray-900 mb-2">Page Not Found</h1>
      <p className="text-gray-600 mb-8">
        The page you're looking for doesn't exist or has been moved.
      </p>
      <a href="/" className="text-primary-500 hover:underline">
        Go back home
      </a>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;

import { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { Header, ErrorBoundary } from './components';
import { HomePage, LoginPage, RegisterPage, PetListPage, PetDetailPage, PetFormPage, PetSubmitPage, RescuesPage, RescueVetManagement, ForgotPasswordPage, ResetPasswordPage, HelpCenterPage, ContactPage, PrivacyPolicyPage, VetSignOffHistoryPage, SettingsPage, NotificationsPage, RescueOrgProfilePage, VetsPage, VetProfilePage, RescueOrgSettingsPage, VetSettingsPage, VerifyEmailPage, VetRequestApprovalPage, ProfilePage, FaqPage, AboutPage, SwipeModePage } from './pages';
import {
  FosterDashboard,
  AdopterDashboard,
  RescueDashboard,
  VetDashboard,
} from './pages/dashboards';
// Note: Admin features have been moved to moderation-service (port 8081)
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

function ConstructionBanner() {
  const [dismissed, setDismissed] = useState(() => {
    return localStorage.getItem('construction-banner-dismissed') === 'true';
  });

  const handleDismiss = () => {
    localStorage.setItem('construction-banner-dismissed', 'true');
    setDismissed(true);
  };

  if (dismissed) return null;

  return (
    <div className="bg-amber-500 text-white py-2 px-4 text-center font-medium relative">
      <span className="inline-flex items-center gap-2">
        <span>🚧</span>
        <span>Site Under Construction - Coming Soon</span>
        <span>🚧</span>
      </span>
      <button
        onClick={handleDismiss}
        className="absolute right-2 top-1/2 -translate-y-1/2 p-1 hover:bg-amber-600 rounded transition-colors"
        aria-label="Dismiss banner"
      >
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>
  );
}

function AppRoutes() {
  return (
    <div className="min-h-screen flex flex-col">
      <ConstructionBanner />
      <Header />
      <main className="flex-1">
        <Routes>
          {/* Public Routes */}
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/pets" element={<PetListPage />} />
          <Route path="/pets/swipe" element={<SwipeModePage />} />
          <Route path="/pets/:id" element={<PetDetailPage />} />
          <Route path="/rescues" element={<RescuesPage />} />
          <Route path="/rescues/:id" element={<RescueOrgProfilePage />} />
          <Route path="/vets" element={<VetsPage />} />
          <Route path="/vets/:id" element={<VetProfilePage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/verify-email/:token" element={<VerifyEmailPage />} />
          <Route path="/help" element={<HelpCenterPage />} />
          <Route path="/faq" element={<FaqPage />} />
          <Route path="/contact" element={<ContactPage />} />
          <Route path="/privacy" element={<PrivacyPolicyPage />} />
          <Route path="/about" element={<AboutPage />} />

          {/* Foster Routes */}
          <Route
            path="/foster/dashboard"
            element={
              <ProtectedRoute allowedRoles={['FOSTER']}>
                <FosterDashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/foster/pets/new"
            element={
              <ProtectedRoute allowedRoles={['FOSTER']}>
                <PetFormPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/foster/pets/:id/edit"
            element={
              <ProtectedRoute allowedRoles={['FOSTER']}>
                <PetFormPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/foster/pets/:id/submit"
            element={
              <ProtectedRoute allowedRoles={['FOSTER']}>
                <PetSubmitPage />
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
          <Route
            path="/rescue/vets"
            element={
              <ProtectedRoute allowedRoles={['RESCUE_ORG']}>
                <RescueVetManagement />
              </ProtectedRoute>
            }
          />
          <Route
            path="/rescue/settings"
            element={
              <ProtectedRoute allowedRoles={['RESCUE_ORG']}>
                <RescueOrgSettingsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/rescue/pets/new"
            element={
              <ProtectedRoute allowedRoles={['RESCUE_ORG']}>
                <PetFormPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/rescue/pets/:id/edit"
            element={
              <ProtectedRoute allowedRoles={['RESCUE_ORG']}>
                <PetFormPage />
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
          <Route
            path="/vet/history"
            element={
              <ProtectedRoute allowedRoles={['VET']}>
                <VetSignOffHistoryPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/vet/approvals"
            element={
              <ProtectedRoute allowedRoles={['VET']}>
                <VetRequestApprovalPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/vet/settings"
            element={
              <ProtectedRoute allowedRoles={['VET']}>
                <VetSettingsPage />
              </ProtectedRoute>
            }
          />

          {/* Admin Routes - Moved to moderation-service (port 8081) */}

          {/* Settings Route - Available to all authenticated users */}
          <Route
            path="/settings"
            element={
              <ProtectedRoute>
                <SettingsPage />
              </ProtectedRoute>
            }
          />

          {/* Notifications Route - Available to all authenticated users */}
          <Route
            path="/notifications"
            element={
              <ProtectedRoute>
                <NotificationsPage />
              </ProtectedRoute>
            }
          />

          {/* Profile Route - Available to all authenticated users */}
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <ProfilePage />
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
    <ErrorBoundary>
      <BrowserRouter>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </BrowserRouter>
    </ErrorBoundary>
  );
}

export default App;

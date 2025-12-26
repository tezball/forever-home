import { useState } from 'react';
import { Link, useNavigate, useLocation, Navigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Button } from '../components';
import type { UserRole } from '../types';

interface LocationState {
  email: string;
  name: string;
  googleId: string;
}

const ROLES: { value: UserRole; label: string; description: string; icon: string }[] = [
  {
    value: 'ADOPTER',
    label: 'Adopter',
    description: 'Looking to adopt a pet and give them a forever home',
    icon: '❤️',
  },
  {
    value: 'FOSTER',
    label: 'Foster',
    description: 'Rehoming a pet and looking for their new family',
    icon: '🏠',
  },
  {
    value: 'VET',
    label: 'Veterinarian',
    description: 'Veterinary professional who will sign off on pet health',
    icon: '🩺',
  },
  {
    value: 'RESCUE_ORG',
    label: 'Rescue Organization',
    description: 'Organization that facilitates pet adoptions',
    icon: '🐾',
  },
];

export function GoogleRoleSelectionPage() {
  const [selectedRole, setSelectedRole] = useState<UserRole | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { completeGoogleRegistration, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // Get the Google auth data from navigation state
  const state = location.state as LocationState | null;

  // If already authenticated, redirect to home
  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  // If no state, redirect to login
  if (!state?.email || !state?.name || !state?.googleId) {
    return <Navigate to="/login" replace />;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!selectedRole) {
      setError('Please select a role to continue');
      return;
    }

    setError('');
    setLoading(true);

    try {
      await completeGoogleRegistration({
        googleId: state.googleId,
        email: state.email,
        name: state.name,
        role: selectedRole,
      });
      navigate('/');
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const response = (err as { response?: { data?: { message?: string } } }).response;
        setError(response?.data?.message || 'Registration failed. Please try again.');
      } else {
        setError('Registration failed. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center py-12 px-4">
      <div className="max-w-md w-full">
        <div className="text-center mb-8">
          <Link to="/" className="inline-flex items-center gap-2 mb-6">
            <span className="text-3xl">🏠</span>
            <span className="font-serif text-2xl font-semibold text-primary-500">Forever Home</span>
          </Link>
          <h1 className="text-2xl font-bold text-gray-900">Welcome, {state.name}!</h1>
          <p className="text-gray-600 mt-2">Choose how you'll use Forever Home</p>
        </div>

        <div className="card p-8">
          <form onSubmit={handleSubmit} className="space-y-6">
            {error && (
              <div className="bg-error-50 border border-error-200 text-error-700 px-4 py-3 rounded">
                {error}
              </div>
            )}

            <div className="space-y-3">
              {ROLES.map((role) => (
                <label
                  key={role.value}
                  className={`block p-4 border-2 rounded-lg cursor-pointer transition-all ${
                    selectedRole === role.value
                      ? 'border-primary-500 bg-primary-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <input
                    type="radio"
                    name="role"
                    value={role.value}
                    checked={selectedRole === role.value}
                    onChange={() => setSelectedRole(role.value)}
                    className="sr-only"
                  />
                  <div className="flex items-start gap-3">
                    <span className="text-2xl">{role.icon}</span>
                    <div>
                      <div className="font-medium text-gray-900">{role.label}</div>
                      <div className="text-sm text-gray-500">{role.description}</div>
                    </div>
                    {selectedRole === role.value && (
                      <svg
                        className="w-5 h-5 text-primary-500 ml-auto flex-shrink-0"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                          clipRule="evenodd"
                        />
                      </svg>
                    )}
                  </div>
                </label>
              ))}
            </div>

            <Button
              type="submit"
              variant="primary"
              loading={loading}
              disabled={!selectedRole}
              className="w-full"
            >
              Complete Registration
            </Button>
          </form>
        </div>

        <p className="text-center mt-6 text-gray-600">
          Already have an account?{' '}
          <Link to="/login" className="text-primary-500 font-medium hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}

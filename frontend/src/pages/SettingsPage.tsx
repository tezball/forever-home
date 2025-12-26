import { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { Button, Breadcrumb, GoogleLoginButton } from '../components';
import apiClient from '../api/client';

interface NotificationPreferences {
  emailStatusChanges: boolean;
  emailNewApplications: boolean;
  emailFavoriteUpdates: boolean;
  inAppEnabled: boolean;
}

export function SettingsPage() {
  const { user, linkGoogleAccount, unlinkGoogleAccount } = useAuth();
  const [preferences, setPreferences] = useState<NotificationPreferences>({
    emailStatusChanges: true,
    emailNewApplications: true,
    emailFavoriteUpdates: true,
    inAppEnabled: true,
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [googleLinking, setGoogleLinking] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [error, setError] = useState('');

  const handleGoogleLink = async (idToken: string) => {
    setError('');
    setSuccessMessage('');
    setGoogleLinking(true);

    try {
      await linkGoogleAccount(idToken);
      setSuccessMessage('Google account connected successfully');
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const response = (err as { response?: { data?: { message?: string } } }).response;
        setError(response?.data?.message || 'Failed to connect Google account');
      } else {
        setError('Failed to connect Google account');
      }
    } finally {
      setGoogleLinking(false);
    }
  };

  const handleGoogleUnlink = async () => {
    setError('');
    setSuccessMessage('');
    setGoogleLinking(true);

    try {
      await unlinkGoogleAccount();
      setSuccessMessage('Google account disconnected successfully');
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const response = (err as { response?: { data?: { message?: string } } }).response;
        setError(response?.data?.message || 'Failed to disconnect Google account');
      } else {
        setError('Failed to disconnect Google account');
      }
    } finally {
      setGoogleLinking(false);
    }
  };

  const handleGoogleError = (errorMessage: string) => {
    setError(errorMessage);
  };

  useEffect(() => {
    fetchPreferences();
  }, []);

  const fetchPreferences = async () => {
    try {
      const res = await apiClient.get<NotificationPreferences>('/profile/notifications');
      setPreferences(res.data);
    } catch (err) {
      console.error('Failed to fetch notification preferences:', err);
      // Use defaults if fetch fails
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    setSaving(true);
    setError('');
    setSuccessMessage('');

    try {
      await apiClient.put('/profile/notifications', preferences);
      setSuccessMessage('Your notification preferences have been saved.');
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const response = (err as { response?: { data?: { message?: string } } }).response;
        setError(response?.data?.message || 'Failed to save preferences');
      } else {
        setError('Failed to save preferences');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleToggle = (key: keyof NotificationPreferences) => {
    setPreferences((prev) => ({
      ...prev,
      [key]: !prev[key],
    }));
    // Clear messages when user makes changes
    setSuccessMessage('');
    setError('');
  };

  if (loading) {
    return (
      <div className="container-app py-8">
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent" />
        </div>
      </div>
    );
  }

  return (
    <div className="container-app py-8">
      <Breadcrumb items={[{ label: 'Settings' }]} />
      <div className="max-w-2xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Settings</h1>
        <p className="text-gray-600 mb-8">Manage your account preferences</p>

        {/* Success Message */}
        {successMessage && (
          <div className="mb-6 bg-success-50 border border-success-200 text-success-700 px-4 py-3 rounded flex justify-between items-center">
            <span>{successMessage}</span>
            <button onClick={() => setSuccessMessage('')} className="text-success-700 hover:text-success-900">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        )}

        {/* Error Message */}
        {error && (
          <div className="mb-6 bg-error-50 border border-error-200 text-error-700 px-4 py-3 rounded flex justify-between items-center">
            <span>{error}</span>
            <button onClick={() => setError('')} className="text-error-700 hover:text-error-900">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        )}

        {/* Account Info */}
        <section className="card p-6 mb-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Account Information</h2>
          <div className="space-y-3">
            <div>
              <label className="text-sm text-gray-500">Name</label>
              <p className="text-gray-900">{user?.name || 'Not set'}</p>
            </div>
            <div>
              <label className="text-sm text-gray-500">Email</label>
              <p className="text-gray-900">{user?.email}</p>
            </div>
            <div>
              <label className="text-sm text-gray-500">Role</label>
              <p className="text-gray-900 capitalize">{user?.role?.toLowerCase().replace('_', ' ')}</p>
            </div>
          </div>
        </section>

        {/* Connected Accounts */}
        <section className="card p-6 mb-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Connected Accounts</h2>
          <p className="text-sm text-gray-600 mb-6">
            Connect your accounts for easier sign-in options.
          </p>

          <div className="flex items-center justify-between p-4 border border-gray-200 rounded-lg">
            <div className="flex items-center gap-3">
              <svg className="w-6 h-6" viewBox="0 0 24 24">
                <path
                  fill="#4285F4"
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                />
                <path
                  fill="#34A853"
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                />
                <path
                  fill="#FBBC05"
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                />
                <path
                  fill="#EA4335"
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                />
              </svg>
              <div>
                <div className="font-medium text-gray-900">Google</div>
                <div className="text-sm text-gray-500">
                  {user?.googleLinked ? 'Connected' : 'Not connected'}
                </div>
              </div>
            </div>

            {user?.googleLinked ? (
              <Button
                variant="secondary"
                onClick={handleGoogleUnlink}
                loading={googleLinking}
                className="text-error-600 hover:text-error-700"
              >
                Disconnect
              </Button>
            ) : (
              <GoogleLoginButton
                onSuccess={handleGoogleLink}
                onError={handleGoogleError}
                loading={googleLinking}
                variant="link"
              />
            )}
          </div>
        </section>

        {/* Notification Preferences */}
        <section className="card p-6 mb-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Notification Preferences</h2>
          <p className="text-sm text-gray-600 mb-6">
            Choose how you want to be notified about activity on your account.
          </p>

          <div className="space-y-4">
            {/* Email Notifications Section */}
            <div className="border-b border-gray-200 pb-4">
              <h3 className="text-sm font-medium text-gray-700 mb-3">Email Notifications</h3>

              <div className="space-y-3">
                <label className="flex items-center justify-between cursor-pointer">
                  <div>
                    <span className="text-gray-900">Status Changes</span>
                    <p className="text-sm text-gray-500">
                      Get notified when your pet's status changes or applications are updated
                    </p>
                  </div>
                  <button
                    type="button"
                    role="switch"
                    aria-checked={preferences.emailStatusChanges}
                    onClick={() => handleToggle('emailStatusChanges')}
                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                      preferences.emailStatusChanges ? 'bg-primary-500' : 'bg-gray-300'
                    }`}
                  >
                    <span
                      className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                        preferences.emailStatusChanges ? 'translate-x-6' : 'translate-x-1'
                      }`}
                    />
                  </button>
                </label>

                <label className="flex items-center justify-between cursor-pointer">
                  <div>
                    <span className="text-gray-900">New Applications</span>
                    <p className="text-sm text-gray-500">
                      Get notified when new adoption applications are submitted for your pets
                    </p>
                  </div>
                  <button
                    type="button"
                    role="switch"
                    aria-checked={preferences.emailNewApplications}
                    onClick={() => handleToggle('emailNewApplications')}
                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                      preferences.emailNewApplications ? 'bg-primary-500' : 'bg-gray-300'
                    }`}
                  >
                    <span
                      className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                        preferences.emailNewApplications ? 'translate-x-6' : 'translate-x-1'
                      }`}
                    />
                  </button>
                </label>

                <label className="flex items-center justify-between cursor-pointer">
                  <div>
                    <span className="text-gray-900">Favorite Updates</span>
                    <p className="text-sm text-gray-500">
                      Get notified when pets you've favorited become available
                    </p>
                  </div>
                  <button
                    type="button"
                    role="switch"
                    aria-checked={preferences.emailFavoriteUpdates}
                    onClick={() => handleToggle('emailFavoriteUpdates')}
                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                      preferences.emailFavoriteUpdates ? 'bg-primary-500' : 'bg-gray-300'
                    }`}
                  >
                    <span
                      className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                        preferences.emailFavoriteUpdates ? 'translate-x-6' : 'translate-x-1'
                      }`}
                    />
                  </button>
                </label>
              </div>
            </div>

            {/* In-App Notifications Section */}
            <div>
              <h3 className="text-sm font-medium text-gray-700 mb-3">In-App Notifications</h3>

              <label className="flex items-center justify-between cursor-pointer">
                <div>
                  <span className="text-gray-900">Enable In-App Notifications</span>
                  <p className="text-sm text-gray-500">
                    Show notification badge and alerts within the application
                  </p>
                </div>
                <button
                  type="button"
                  role="switch"
                  aria-checked={preferences.inAppEnabled}
                  onClick={() => handleToggle('inAppEnabled')}
                  className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                    preferences.inAppEnabled ? 'bg-primary-500' : 'bg-gray-300'
                  }`}
                >
                  <span
                    className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                      preferences.inAppEnabled ? 'translate-x-6' : 'translate-x-1'
                    }`}
                  />
                </button>
              </label>
            </div>
          </div>

          <div className="mt-6 pt-4 border-t border-gray-200">
            <Button variant="primary" onClick={handleSave} loading={saving}>
              Save Preferences
            </Button>
          </div>
        </section>
      </div>
    </div>
  );
}

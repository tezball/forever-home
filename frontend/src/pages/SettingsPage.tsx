import { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { Button } from '../components';
import apiClient from '../api/client';

interface NotificationPreferences {
  emailStatusChanges: boolean;
  emailNewApplications: boolean;
  emailFavoriteUpdates: boolean;
  inAppEnabled: boolean;
}

export function SettingsPage() {
  const { user } = useAuth();
  const [preferences, setPreferences] = useState<NotificationPreferences>({
    emailStatusChanges: true,
    emailNewApplications: true,
    emailFavoriteUpdates: true,
    inAppEnabled: true,
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [error, setError] = useState('');

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

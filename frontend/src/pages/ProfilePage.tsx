import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Button, Input } from '../components';
import apiClient from '../api/client';

interface Address {
  street?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
}

interface FosterProfile {
  firstName: string | null;
  lastName: string | null;
  phone: string | null;
  address: Address | null;
  isComplete: boolean;
}

interface AdopterProfile {
  firstName: string | null;
  lastName: string | null;
  phone: string | null;
  livingSituation: string | null;
  petExperience: string | null;
  address: Address | null;
  isComplete: boolean;
}

export function ProfilePage() {
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Form fields for Foster/Adopter
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [phone, setPhone] = useState('');
  const [street, setStreet] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [postalCode, setPostalCode] = useState('');

  // Adopter-specific fields
  const [livingSituation, setLivingSituation] = useState('');
  const [petExperience, setPetExperience] = useState('');

  const getDashboardLink = () => {
    if (!user) return '/';
    switch (user.role) {
      case 'FOSTER': return '/foster/dashboard';
      case 'ADOPTER': return '/adopter/dashboard';
      case 'VET': return '/vet/settings';
      case 'RESCUE_ORG': return '/rescue/settings';
      case 'ADMIN': return '/admin/dashboard';
      default: return '/';
    }
  };

  useEffect(() => {
    fetchProfile();
  }, [user?.role]);

  const fetchProfile = async () => {
    if (!user) return;

    try {
      if (user.role === 'FOSTER') {
        const response = await apiClient.get<FosterProfile>('/profile/foster');
        const data = response.data;
        setFirstName(data.firstName || '');
        setLastName(data.lastName || '');
        setPhone(data.phone || '');
        setStreet(data.address?.street || '');
        setCity(data.address?.city || '');
        setState(data.address?.state || '');
        setPostalCode(data.address?.postalCode || '');
      } else if (user.role === 'ADOPTER') {
        const response = await apiClient.get<AdopterProfile>('/profile/adopter');
        const data = response.data;
        setFirstName(data.firstName || '');
        setLastName(data.lastName || '');
        setPhone(data.phone || '');
        setLivingSituation(data.livingSituation || '');
        setPetExperience(data.petExperience || '');
        setStreet(data.address?.street || '');
        setCity(data.address?.city || '');
        setState(data.address?.state || '');
        setPostalCode(data.address?.postalCode || '');
      }
      // VET and RESCUE_ORG have their own dedicated settings pages
    } catch {
      // Profile may not exist yet, that's okay
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    setSuccess('');

    try {
      const address = {
        street: street || null,
        city: city || null,
        state: state || null,
        postalCode: postalCode || null,
        country: 'USA',
      };

      if (user?.role === 'FOSTER') {
        await apiClient.post('/profile/foster', {
          firstName,
          lastName,
          phone: phone || null,
          address,
        });
      } else if (user?.role === 'ADOPTER') {
        await apiClient.post('/profile/adopter', {
          firstName,
          lastName,
          phone: phone || null,
          livingSituation: livingSituation || null,
          petExperience: petExperience || null,
          address,
        });
      }

      setSuccess('Profile updated successfully');
    } catch {
      setError('Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  // Redirect VET and RESCUE_ORG users to their dedicated settings pages
  if (user?.role === 'VET') {
    return (
      <div className="container-app py-8">
        <div className="max-w-2xl mx-auto text-center">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">Profile Settings</h1>
          <p className="text-gray-600 mb-6">
            Veterinarians can manage their clinic profile on the dedicated settings page.
          </p>
          <Link to="/vet/settings">
            <Button variant="primary">Go to Clinic Settings</Button>
          </Link>
        </div>
      </div>
    );
  }

  if (user?.role === 'RESCUE_ORG') {
    return (
      <div className="container-app py-8">
        <div className="max-w-2xl mx-auto text-center">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">Profile Settings</h1>
          <p className="text-gray-600 mb-6">
            Rescue organizations can manage their organization profile on the dedicated settings page.
          </p>
          <Link to="/rescue/settings">
            <Button variant="primary">Go to Organization Settings</Button>
          </Link>
        </div>
      </div>
    );
  }

  if (user?.role === 'ADMIN') {
    return (
      <div className="container-app py-8">
        <div className="max-w-2xl mx-auto text-center">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">Admin Profile</h1>
          <p className="text-gray-600 mb-6">
            Admin account settings are managed through the admin dashboard.
          </p>
          <Link to="/admin/dashboard">
            <Button variant="primary">Go to Admin Dashboard</Button>
          </Link>
        </div>
      </div>
    );
  }

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
      {/* Breadcrumb */}
      <nav className="text-sm text-gray-500 mb-6">
        <Link to={getDashboardLink()} className="hover:text-primary-500">Dashboard</Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">Profile</span>
      </nav>

      <div className="max-w-2xl">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">
          {user?.role === 'FOSTER' ? 'Foster Profile' : 'Adopter Profile'}
        </h1>

        {error && (
          <div className="mb-6 p-4 bg-error-50 border border-error-200 rounded-lg text-error-700">
            {error}
          </div>
        )}

        {success && (
          <div className="mb-6 p-4 bg-success-50 border border-success-200 rounded-lg text-success-700">
            {success}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Personal Info */}
          <div className="card p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Personal Information</h2>
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <Input
                  label="First Name"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  required
                />
                <Input
                  label="Last Name"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  required
                />
              </div>
              <Input
                label="Phone"
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="(555) 123-4567"
              />
            </div>
          </div>

          {/* Adopter-specific fields */}
          {user?.role === 'ADOPTER' && (
            <div className="card p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">Adoption Preferences</h2>
              <p className="text-sm text-gray-600 mb-4">
                This information helps rescue organizations match you with the right pet.
              </p>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Living Situation</label>
                  <textarea
                    value={livingSituation}
                    onChange={(e) => setLivingSituation(e.target.value)}
                    className="input-field min-h-[80px]"
                    placeholder="Describe your home (house, apartment, yard, etc.)"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Pet Experience</label>
                  <textarea
                    value={petExperience}
                    onChange={(e) => setPetExperience(e.target.value)}
                    className="input-field min-h-[80px]"
                    placeholder="Describe your experience with pets"
                  />
                </div>
              </div>
            </div>
          )}

          {/* Address */}
          <div className="card p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Address</h2>
            <div className="space-y-4">
              <Input
                label="Street Address"
                value={street}
                onChange={(e) => setStreet(e.target.value)}
              />
              <div className="grid grid-cols-3 gap-4">
                <Input
                  label="City"
                  value={city}
                  onChange={(e) => setCity(e.target.value)}
                />
                <Input
                  label="State"
                  value={state}
                  onChange={(e) => setState(e.target.value)}
                />
                <Input
                  label="Postal Code"
                  value={postalCode}
                  onChange={(e) => setPostalCode(e.target.value)}
                />
              </div>
            </div>
          </div>

          {/* Account Info (read-only) */}
          <div className="card p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Account Information</h2>
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
          </div>

          {/* Submit */}
          <div className="flex gap-4">
            <Button type="submit" variant="primary" disabled={saving}>
              {saving ? 'Saving...' : 'Save Changes'}
            </Button>
            <Link to={getDashboardLink()}>
              <Button type="button" variant="outline">
                Cancel
              </Button>
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
}

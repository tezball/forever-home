import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Button, Input } from '../components';
import apiClient from '../api/client';

interface Address {
  street?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
}

interface VetProfile {
  clinicName: string;
  licenseNumber: string;
  phone?: string;
  website?: string;
  description?: string;
  address?: Address;
  verified: boolean;
  isComplete: boolean;
}

export function VetSettingsPage() {
  const [profile, setProfile] = useState<VetProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Form fields
  const [clinicName, setClinicName] = useState('');
  const [licenseNumber, setLicenseNumber] = useState('');
  const [phone, setPhone] = useState('');
  const [website, setWebsite] = useState('');
  const [description, setDescription] = useState('');
  const [street, setStreet] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [postalCode, setPostalCode] = useState('');

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      const response = await apiClient.get<VetProfile>('/profile/vet');
      const data = response.data;
      setProfile(data);

      // Populate form fields
      setClinicName(data.clinicName || '');
      setLicenseNumber(data.licenseNumber || '');
      setPhone(data.phone || '');
      setWebsite(data.website || '');
      setDescription(data.description || '');
      setStreet(data.address?.street || '');
      setCity(data.address?.city || '');
      setState(data.address?.state || '');
      setPostalCode(data.address?.postalCode || '');
    } catch {
      setError('Failed to load profile');
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
      const payload = {
        clinicName,
        licenseNumber,
        phone: phone || null,
        website: website || null,
        description: description || null,
        address: {
          street: street || null,
          city: city || null,
          state: state || null,
          postalCode: postalCode || null,
          country: 'USA',
        },
      };

      await apiClient.post('/profile/vet', payload);
      setSuccess('Profile updated successfully');
      fetchProfile();
    } catch {
      setError('Failed to update profile');
    } finally {
      setSaving(false);
    }
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
      {/* Breadcrumb */}
      <nav className="text-sm text-gray-500 mb-6">
        <Link to="/vet/dashboard" className="hover:text-primary-500">Dashboard</Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">Clinic Settings</span>
      </nav>

      <div className="max-w-2xl">
        <div className="flex items-center gap-3 mb-6">
          <h1 className="text-2xl font-bold text-gray-900">Clinic Settings</h1>
          {profile?.verified && (
            <span className="bg-success-100 text-success-700 text-xs font-medium px-2 py-1 rounded">
              Verified
            </span>
          )}
        </div>

        {!profile?.verified && (
          <div className="mb-6 p-4 bg-warning-50 border border-warning-200 rounded-lg text-warning-700">
            Your clinic is pending verification. You can still update your profile while waiting.
          </div>
        )}

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
          {/* Basic Info */}
          <div className="card p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Clinic Information</h2>
            <div className="space-y-4">
              <Input
                label="Clinic Name"
                value={clinicName}
                onChange={(e) => setClinicName(e.target.value)}
                required
              />
              <div>
                <Input
                  label="License Number"
                  value={licenseNumber}
                  onChange={(e) => setLicenseNumber(e.target.value)}
                  required
                  disabled={profile?.isComplete}
                />
                {profile?.isComplete && (
                  <p className="text-xs text-gray-500 mt-1">License number cannot be changed after initial registration</p>
                )}
              </div>
              <div className="grid grid-cols-2 gap-4">
                <Input
                  label="Phone"
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                />
                <Input
                  label="Website"
                  type="url"
                  value={website}
                  onChange={(e) => setWebsite(e.target.value)}
                  placeholder="https://"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="input-field min-h-[100px]"
                  placeholder="Tell rescue organizations about your clinic and services..."
                />
              </div>
            </div>
          </div>

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

          {/* Services Info */}
          <div className="card p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Services Provided</h2>
            <p className="text-gray-600 text-sm mb-4">
              As a verified veterinarian on Forever Home, you can provide the following services for pets in the adoption process:
            </p>
            <ul className="space-y-2 text-sm text-gray-600">
              <li className="flex items-center gap-2">
                <span className="text-success-500">✓</span>
                Health verification for pets pending adoption
              </li>
              <li className="flex items-center gap-2">
                <span className="text-success-500">✓</span>
                Vaccination record verification
              </li>
              <li className="flex items-center gap-2">
                <span className="text-success-500">✓</span>
                Spay/neuter status confirmation
              </li>
              <li className="flex items-center gap-2">
                <span className="text-success-500">✓</span>
                Microchip lookup and verification
              </li>
            </ul>
          </div>

          {/* Submit */}
          <div className="flex gap-4">
            <Button type="submit" variant="primary" disabled={saving}>
              {saving ? 'Saving...' : 'Save Changes'}
            </Button>
            <Link to="/vet/dashboard">
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

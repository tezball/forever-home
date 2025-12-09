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

interface SocialLinks {
  facebook?: string;
  instagram?: string;
  twitter?: string;
  tiktok?: string;
}

interface RescueOrgProfile {
  name: string;
  phone?: string;
  website?: string;
  description?: string;
  contactName?: string;
  contactEmail?: string;
  address?: Address;
  socialLinks?: SocialLinks;
  verified: boolean;
  isComplete: boolean;
}

export function RescueOrgSettingsPage() {
  const [profile, setProfile] = useState<RescueOrgProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Form fields
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [website, setWebsite] = useState('');
  const [description, setDescription] = useState('');
  const [contactName, setContactName] = useState('');
  const [contactEmail, setContactEmail] = useState('');
  const [street, setStreet] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [postalCode, setPostalCode] = useState('');
  const [facebook, setFacebook] = useState('');
  const [instagram, setInstagram] = useState('');
  const [twitter, setTwitter] = useState('');
  const [tiktok, setTiktok] = useState('');

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      const response = await apiClient.get<RescueOrgProfile>('/profile/rescue-org');
      const data = response.data;
      setProfile(data);

      // Populate form fields
      setName(data.name || '');
      setPhone(data.phone || '');
      setWebsite(data.website || '');
      setDescription(data.description || '');
      setContactName(data.contactName || '');
      setContactEmail(data.contactEmail || '');
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
        name,
        phone: phone || null,
        website: website || null,
        description: description || null,
        contactName: contactName || null,
        contactEmail: contactEmail || null,
        address: {
          street: street || null,
          city: city || null,
          state: state || null,
          postalCode: postalCode || null,
          country: 'USA',
        },
        socialLinks: {
          facebook: facebook || null,
          instagram: instagram || null,
          twitter: twitter || null,
          tiktok: tiktok || null,
        },
      };

      await apiClient.post('/profile/rescue-org', payload);
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
        <Link to="/rescue/dashboard" className="hover:text-primary-500">Dashboard</Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">Organization Settings</span>
      </nav>

      <div className="max-w-2xl">
        <div className="flex items-center gap-3 mb-6">
          <h1 className="text-2xl font-bold text-gray-900">Organization Settings</h1>
          {profile?.verified && (
            <span className="bg-success-100 text-success-700 text-xs font-medium px-2 py-1 rounded">
              Verified
            </span>
          )}
        </div>

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
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Basic Information</h2>
            <div className="space-y-4">
              <Input
                label="Organization Name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
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
                  placeholder="Tell potential adopters about your organization..."
                />
              </div>
            </div>
          </div>

          {/* Contact Info */}
          <div className="card p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Contact Person</h2>
            <div className="grid grid-cols-2 gap-4">
              <Input
                label="Contact Name"
                value={contactName}
                onChange={(e) => setContactName(e.target.value)}
              />
              <Input
                label="Contact Email"
                type="email"
                value={contactEmail}
                onChange={(e) => setContactEmail(e.target.value)}
              />
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

          {/* Social Links */}
          <div className="card p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Social Media</h2>
            <div className="grid grid-cols-2 gap-4">
              <Input
                label="Facebook"
                value={facebook}
                onChange={(e) => setFacebook(e.target.value)}
                placeholder="https://facebook.com/..."
              />
              <Input
                label="Instagram"
                value={instagram}
                onChange={(e) => setInstagram(e.target.value)}
                placeholder="https://instagram.com/..."
              />
              <Input
                label="Twitter / X"
                value={twitter}
                onChange={(e) => setTwitter(e.target.value)}
                placeholder="https://twitter.com/..."
              />
              <Input
                label="TikTok"
                value={tiktok}
                onChange={(e) => setTiktok(e.target.value)}
                placeholder="https://tiktok.com/..."
              />
            </div>
          </div>

          {/* Submit */}
          <div className="flex gap-4">
            <Button type="submit" variant="primary" disabled={saving}>
              {saving ? 'Saving...' : 'Save Changes'}
            </Button>
            <Link to="/rescue/dashboard">
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

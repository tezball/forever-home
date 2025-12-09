import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { PetCard } from '../components';
import type { Pet, RescueOrganization } from '../types';
import apiClient from '../api/client';

export function RescueOrgProfilePage() {
  const { id } = useParams<{ id: string }>();
  const [rescue, setRescue] = useState<RescueOrganization | null>(null);
  const [pets, setPets] = useState<Pet[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (id) {
      fetchRescueData();
    }
  }, [id]);

  const fetchRescueData = async () => {
    setLoading(true);
    setError('');
    try {
      const [rescueRes, petsRes] = await Promise.all([
        apiClient.get<RescueOrganization>(`/rescues/${id}`),
        apiClient.get<Pet[]>(`/rescues/${id}/pets`),
      ]);
      setRescue(rescueRes.data);
      setPets(petsRes.data);
    } catch {
      setError('Failed to load rescue organization. It may not exist or is not verified.');
    } finally {
      setLoading(false);
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

  if (error || !rescue) {
    return (
      <div className="container-app py-8">
        <div className="text-center py-12">
          <div className="text-6xl mb-4">🔍</div>
          <h1 className="text-2xl font-bold text-gray-900 mb-2">Organization Not Found</h1>
          <p className="text-gray-600 mb-8">{error || 'This rescue organization does not exist.'}</p>
          <Link to="/rescues" className="text-primary-500 hover:underline">
            ← Back to Rescue Organizations
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="container-app py-8">
      {/* Breadcrumb */}
      <nav className="text-sm text-gray-500 mb-6">
        <Link to="/rescues" className="hover:text-primary-500">Rescue Organizations</Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">{rescue.name}</span>
      </nav>

      {/* Organization Header */}
      <div className="card p-6 md:p-8 mb-8">
        <div className="flex flex-col md:flex-row md:items-start gap-6">
          {/* Logo */}
          <div className="w-24 h-24 md:w-32 md:h-32 bg-primary-100 rounded-xl flex items-center justify-center flex-shrink-0">
            {rescue.logoUrl ? (
              <img src={rescue.logoUrl} alt={rescue.name} className="w-20 h-20 md:w-28 md:h-28 object-contain" />
            ) : (
              <span className="text-5xl md:text-6xl">🏥</span>
            )}
          </div>

          {/* Info */}
          <div className="flex-1">
            <div className="flex items-center gap-3 mb-2">
              <h1 className="text-2xl md:text-3xl font-bold text-gray-900">{rescue.name}</h1>
              <span className="bg-success-100 text-success-700 text-xs font-medium px-2 py-1 rounded">
                Verified
              </span>
            </div>
            {rescue.location && (
              <p className="text-gray-500 mb-4 flex items-center gap-1">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                {rescue.location}
              </p>
            )}
            {rescue.description && (
              <p className="text-gray-600 mb-4">{rescue.description}</p>
            )}

            {/* Contact Info */}
            <div className="flex flex-wrap gap-4">
              {rescue.website && (
                <a
                  href={rescue.website}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 text-primary-500 hover:underline"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 01-9 9m9-9a9 9 0 00-9-9m9 9H3m9 9a9 9 0 01-9-9m9 9c1.657 0 3-4.03 3-9s-1.343-9-3-9m0 18c-1.657 0-3-4.03-3-9s1.343-9 3-9m-9 9a9 9 0 019-9" />
                  </svg>
                  Website
                </a>
              )}
              {rescue.email && (
                <a
                  href={`mailto:${rescue.email}`}
                  className="inline-flex items-center gap-2 text-primary-500 hover:underline"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                  </svg>
                  {rescue.email}
                </a>
              )}
              {rescue.phone && (
                <a
                  href={`tel:${rescue.phone}`}
                  className="inline-flex items-center gap-2 text-primary-500 hover:underline"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                  </svg>
                  {rescue.phone}
                </a>
              )}
            </div>
          </div>

          {/* Stats */}
          <div className="flex md:flex-col gap-6 md:gap-4 bg-secondary-50 p-4 rounded-lg">
            <div className="text-center">
              <div className="text-3xl font-bold text-primary-600">{rescue.petCount || 0}</div>
              <div className="text-sm text-gray-500">Available Pets</div>
            </div>
          </div>
        </div>
      </div>

      {/* Available Pets */}
      <div className="mb-8">
        <h2 className="text-2xl font-bold text-gray-900 mb-6">
          Available Pets ({pets.length})
        </h2>

        {pets.length === 0 ? (
          <div className="card p-8 text-center">
            <div className="text-5xl mb-4">🐾</div>
            <h3 className="text-xl font-semibold text-gray-900 mb-2">No pets available</h3>
            <p className="text-gray-600">
              This organization doesn't have any pets available for adoption right now.
              Check back later!
            </p>
          </div>
        ) : (
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {pets.map((pet) => (
              <PetCard key={pet.id} pet={pet} />
            ))}
          </div>
        )}
      </div>

      {/* Back Link */}
      <div className="text-center">
        <Link to="/rescues" className="text-primary-500 hover:underline">
          ← View All Rescue Organizations
        </Link>
      </div>
    </div>
  );
}

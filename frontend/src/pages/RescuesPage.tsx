import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Input } from '../components';
import type { RescueOrganization } from '../types';
import apiClient from '../api/client';

export function RescuesPage() {
  const [rescues, setRescues] = useState<RescueOrganization[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');

  useEffect(() => {
    fetchRescues();
  }, []);

  const fetchRescues = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await apiClient.get<RescueOrganization[]>('/rescues');
      setRescues(response.data);
    } catch {
      setError('Failed to load rescue organizations. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const filteredRescues = rescues.filter((rescue) => {
    if (search) {
      const searchLower = search.toLowerCase();
      return (
        rescue.name.toLowerCase().includes(searchLower) ||
        (rescue.location && rescue.location.toLowerCase().includes(searchLower)) ||
        (rescue.description && rescue.description.toLowerCase().includes(searchLower))
      );
    }
    return true;
  });

  return (
    <div className="container-app py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Rescue Organizations</h1>
        <p className="text-gray-600">Find trusted rescue organizations in your area</p>
      </div>

      {/* Search */}
      <div className="bg-secondary-50 rounded-lg p-4 mb-8">
        <div className="max-w-md">
          <Input
            placeholder="Search by name or location..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      {/* Results */}
      {loading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent" />
        </div>
      ) : error && rescues.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-error-500">{error}</p>
        </div>
      ) : filteredRescues.length === 0 ? (
        <div className="text-center py-12">
          <div className="text-6xl mb-4">🐾</div>
          <h3 className="text-xl font-semibold text-gray-900 mb-2">
            {rescues.length === 0 ? 'No rescue organizations yet' : 'No organizations match your search'}
          </h3>
          <p className="text-gray-600 mb-6">
            {rescues.length === 0
              ? 'We\'re expanding our network of rescue partners. Check back soon!'
              : 'Try a different search term or browse all organizations.'}
          </p>
          {search && (
            <button
              onClick={() => setSearch('')}
              className="inline-flex items-center px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
            >
              Clear Search
            </button>
          )}
        </div>
      ) : (
        <>
          <p className="text-sm text-gray-500 mb-4">
            Showing {filteredRescues.length} organization{filteredRescues.length !== 1 ? 's' : ''}
          </p>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredRescues.map((rescue) => (
              <div key={rescue.id} className="card p-6">
                <div className="flex items-start gap-4">
                  <div className="w-16 h-16 bg-primary-100 rounded-lg flex items-center justify-center flex-shrink-0">
                    {rescue.logoUrl ? (
                      <img src={rescue.logoUrl} alt={rescue.name} className="w-12 h-12 object-contain" />
                    ) : (
                      <span className="text-3xl">🐾</span>
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="text-lg font-semibold text-gray-900 mb-1">{rescue.name}</h3>
                    {rescue.location && <p className="text-sm text-gray-500 mb-2">{rescue.location}</p>}
                  </div>
                </div>
                {rescue.description && <p className="text-gray-600 text-sm mt-4 line-clamp-3">{rescue.description}</p>}
                <div className="mt-4 pt-4 border-t border-secondary-200">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-500">
                      {rescue.petCount} pet{rescue.petCount !== 1 ? 's' : ''} available
                    </span>
                    <Link
                      to={`/rescues/${rescue.id}`}
                      className="text-primary-500 text-sm font-medium hover:underline"
                    >
                      View Profile →
                    </Link>
                  </div>
                </div>
                <div className="mt-3 flex flex-wrap gap-2">
                  {rescue.website && (
                    <a
                      href={rescue.website}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-xs text-gray-500 hover:text-primary-500"
                    >
                      Website
                    </a>
                  )}
                  {rescue.email && (
                    <a
                      href={`mailto:${rescue.email}`}
                      className="text-xs text-gray-500 hover:text-primary-500"
                    >
                      Email
                    </a>
                  )}
                  {rescue.phone && (
                    <a
                      href={`tel:${rescue.phone}`}
                      className="text-xs text-gray-500 hover:text-primary-500"
                    >
                      Call
                    </a>
                  )}
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
}

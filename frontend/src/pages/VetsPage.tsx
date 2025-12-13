import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Input } from '../components';
import apiClient from '../api/client';

interface VetPublic {
  id: string;
  clinicName: string;
  description?: string;
  location?: string;
  website?: string;
  phone?: string;
  logoUrl?: string;
  signOffCount: number;
}

export function VetsPage() {
  const [vets, setVets] = useState<VetPublic[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');

  useEffect(() => {
    fetchVets();
  }, []);

  const fetchVets = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await apiClient.get<VetPublic[]>('/vets');
      setVets(response.data);
    } catch {
      setError('Failed to load veterinarians. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const filteredVets = vets.filter((vet) => {
    if (search) {
      const searchLower = search.toLowerCase();
      return (
        vet.clinicName.toLowerCase().includes(searchLower) ||
        (vet.location && vet.location.toLowerCase().includes(searchLower)) ||
        (vet.description && vet.description.toLowerCase().includes(searchLower))
      );
    }
    return true;
  });

  return (
    <div className="container-app py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Verified Veterinarians</h1>
        <p className="text-gray-600">Find trusted veterinarians who verify pets on our platform</p>
      </div>

      {/* Search */}
      <div className="bg-secondary-50 rounded-lg p-4 mb-8">
        <div className="max-w-md">
          <Input
            placeholder="Search by clinic name or location..."
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
      ) : error && vets.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-error-500">{error}</p>
        </div>
      ) : filteredVets.length === 0 ? (
        <div className="text-center py-12">
          <div className="text-6xl mb-4">⚕️</div>
          <h3 className="text-xl font-semibold text-gray-900 mb-2">
            {vets.length === 0 ? 'No verified veterinarians yet' : 'No veterinarians match your search'}
          </h3>
          <p className="text-gray-600 mb-6">
            {vets.length === 0
              ? 'Our network of verified veterinarians is growing. Check back soon!'
              : 'Try a different search term or browse all veterinarians.'}
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
            Showing {filteredVets.length} veterinarian{filteredVets.length !== 1 ? 's' : ''}
          </p>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredVets.map((vet) => (
              <div key={vet.id} className="card p-6">
                <div className="flex items-start gap-4">
                  <div className="w-16 h-16 bg-primary-100 rounded-lg flex items-center justify-center flex-shrink-0">
                    {vet.logoUrl ? (
                      <img src={vet.logoUrl} alt={vet.clinicName} className="w-12 h-12 object-contain" />
                    ) : (
                      <span className="text-3xl">⚕️</span>
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="text-lg font-semibold text-gray-900">{vet.clinicName}</h3>
                      <span className="bg-success-100 text-success-700 text-xs font-medium px-2 py-0.5 rounded">
                        Verified
                      </span>
                    </div>
                    {vet.location && <p className="text-sm text-gray-500 mb-2">{vet.location}</p>}
                  </div>
                </div>
                {vet.description && <p className="text-gray-600 text-sm mt-4 line-clamp-3">{vet.description}</p>}
                <div className="mt-4 pt-4 border-t border-secondary-200">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-500">
                      {vet.signOffCount} pet{vet.signOffCount !== 1 ? 's' : ''} verified
                    </span>
                    <Link
                      to={`/vets/${vet.id}`}
                      className="text-primary-500 text-sm font-medium hover:underline"
                    >
                      View Profile →
                    </Link>
                  </div>
                </div>
                <div className="mt-3 flex flex-wrap gap-2">
                  {vet.website && (
                    <a
                      href={vet.website}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-xs text-gray-500 hover:text-primary-500"
                    >
                      Website
                    </a>
                  )}
                  {vet.phone && (
                    <a
                      href={`tel:${vet.phone}`}
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

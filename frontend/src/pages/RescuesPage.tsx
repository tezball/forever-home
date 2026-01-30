import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Input, Button } from '../components';
import { useGeolocation } from '../hooks';
import type { RescueOrganization, RescueSearchResponse, RescueWithDistance } from '../types';
import apiClient from '../api/client';

type SearchMode = 'all' | 'nearby';

export function RescuesPage() {
  const [rescues, setRescues] = useState<RescueOrganization[]>([]);
  const [nearbyRescues, setNearbyRescues] = useState<RescueWithDistance[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [searchMode, setSearchMode] = useState<SearchMode>('all');
  const [zipCode, setZipCode] = useState('');
  const [radius, setRadius] = useState(50);

  const { location, error: geoError, loading: geoLoading, requestLocation, clearLocation } = useGeolocation();

  useEffect(() => {
    fetchRescues();
  }, []);

  // Fetch nearby rescues when location is obtained
  useEffect(() => {
    if (location && searchMode === 'nearby') {
      fetchNearbyRescues(location.lat, location.lon);
    }
  }, [location, searchMode, radius]);

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

  const fetchNearbyRescues = async (lat: number, lon: number) => {
    setLoading(true);
    setError('');
    try {
      const response = await apiClient.get<RescueSearchResponse>('/rescues/search', {
        params: { lat, lon, radius },
      });
      setNearbyRescues(response.data.results);
    } catch {
      setError('Failed to search nearby rescues. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleSearchByZip = async () => {
    if (!zipCode.trim()) return;

    setLoading(true);
    setError('');
    setSearchMode('nearby');
    try {
      const response = await apiClient.get<RescueSearchResponse>('/rescues/search', {
        params: { zip: zipCode, radius },
      });
      setNearbyRescues(response.data.results);
    } catch {
      setError('Could not find location. Please check your zip code and try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleNearMeClick = () => {
    setSearchMode('nearby');
    setZipCode('');
    requestLocation();
  };

  const handleShowAll = () => {
    setSearchMode('all');
    setNearbyRescues([]);
    clearLocation();
    setZipCode('');
  };

  const formatDistance = (distanceKm: number): string => {
    if (distanceKm < 1) {
      return `${Math.round(distanceKm * 1000)} m`;
    }
    return `${distanceKm.toFixed(1)} km`;
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

  const displayedRescues = searchMode === 'nearby' ? nearbyRescues : filteredRescues;

  return (
    <div className="container-app py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Rescue Organizations</h1>
        <p className="text-gray-600">Find trusted rescue organizations in your area</p>
      </div>

      {/* Search Controls */}
      <div className="bg-secondary-50 rounded-lg p-4 mb-8">
        <div className="flex flex-col md:flex-row gap-4">
          {/* Location Search */}
          <div className="flex-1">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Find rescues near you
            </label>
            <div className="flex gap-2">
              <Button
                onClick={handleNearMeClick}
                disabled={geoLoading}
                variant={searchMode === 'nearby' && location ? 'primary' : 'secondary'}
                className="whitespace-nowrap"
              >
                {geoLoading ? (
                  <>
                    <span className="animate-spin inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full mr-2" />
                    Locating...
                  </>
                ) : (
                  <>
                    <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                    Near Me
                  </>
                )}
              </Button>
              <div className="flex-1 flex gap-2">
                <Input
                  placeholder="Or enter zip code..."
                  value={zipCode}
                  onChange={(e) => setZipCode(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleSearchByZip()}
                  className="flex-1"
                />
                <Button onClick={handleSearchByZip} variant="secondary" disabled={!zipCode.trim()}>
                  Search
                </Button>
              </div>
            </div>
            {geoError && (
              <p className="text-sm text-error-500 mt-2">{geoError}</p>
            )}
          </div>

          {/* Radius Selector (shown when in nearby mode) */}
          {searchMode === 'nearby' && (
            <div className="md:w-40">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Radius
              </label>
              <select
                value={radius}
                onChange={(e) => setRadius(Number(e.target.value))}
                className="w-full px-3 py-2 border border-secondary-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              >
                <option value={10}>10 km</option>
                <option value={25}>25 km</option>
                <option value={50}>50 km</option>
                <option value={100}>100 km</option>
                <option value={200}>200 km</option>
              </select>
            </div>
          )}
        </div>

        {/* Mode indicator and reset */}
        {searchMode === 'nearby' && (
          <div className="mt-4 flex items-center justify-between">
            <span className="text-sm text-gray-600">
              {location ? (
                <>Showing rescues within {radius} km of your location</>
              ) : zipCode ? (
                <>Showing rescues within {radius} km of {zipCode}</>
              ) : (
                <>Enable location or enter a zip code to find nearby rescues</>
              )}
            </span>
            <button
              onClick={handleShowAll}
              className="text-sm text-primary-500 hover:underline"
            >
              Show all rescues
            </button>
          </div>
        )}

        {/* Text search (only in 'all' mode) */}
        {searchMode === 'all' && (
          <div className="mt-4 max-w-md">
            <Input
              placeholder="Search by name or location..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
        )}
      </div>

      {/* Results */}
      {loading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent" />
        </div>
      ) : error && displayedRescues.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-error-500">{error}</p>
          {searchMode === 'nearby' && (
            <button
              onClick={handleShowAll}
              className="mt-4 text-primary-500 hover:underline"
            >
              Show all rescues instead
            </button>
          )}
        </div>
      ) : displayedRescues.length === 0 ? (
        <div className="text-center py-12">
          <div className="text-6xl mb-4">🐾</div>
          <h3 className="text-xl font-semibold text-gray-900 mb-2">
            {searchMode === 'nearby'
              ? 'No rescues found nearby'
              : rescues.length === 0
                ? 'No rescue organizations yet'
                : 'No organizations match your search'}
          </h3>
          <p className="text-gray-600 mb-6">
            {searchMode === 'nearby'
              ? 'Try increasing the search radius or searching in a different area.'
              : rescues.length === 0
                ? "We're expanding our network of rescue partners. Check back soon!"
                : 'Try a different search term or browse all organizations.'}
          </p>
          {(search || searchMode === 'nearby') && (
            <button
              onClick={() => {
                setSearch('');
                handleShowAll();
              }}
              className="inline-flex items-center px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
            >
              {searchMode === 'nearby' ? 'Show All Rescues' : 'Clear Search'}
            </button>
          )}
        </div>
      ) : (
        <>
          <p className="text-sm text-gray-500 mb-4">
            Showing {displayedRescues.length} organization{displayedRescues.length !== 1 ? 's' : ''}
            {searchMode === 'nearby' && ' nearby'}
          </p>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {displayedRescues.map((rescue) => (
              <RescueCard
                key={rescue.id}
                rescue={rescue}
                showDistance={searchMode === 'nearby'}
                formatDistance={formatDistance}
              />
            ))}
          </div>
        </>
      )}
    </div>
  );
}

interface RescueCardProps {
  rescue: RescueOrganization | RescueWithDistance;
  showDistance: boolean;
  formatDistance: (km: number) => string;
}

function RescueCard({ rescue, showDistance, formatDistance }: RescueCardProps) {
  // Type guard to check if it's a RescueWithDistance
  const isWithDistance = (r: RescueOrganization | RescueWithDistance): r is RescueWithDistance => {
    return 'distanceKm' in r && typeof r.distanceKm === 'number';
  };

  // Get location string depending on type
  const getLocation = (): string | null => {
    if ('location' in rescue && rescue.location) {
      return rescue.location;
    }
    if ('city' in rescue || 'state' in rescue) {
      const r = rescue as RescueWithDistance;
      if (r.city && r.state) {
        return `${r.city}, ${r.state}`;
      }
      return r.city || r.state || null;
    }
    return null;
  };

  const location = getLocation();
  const distanceKm = isWithDistance(rescue) ? rescue.distanceKm : undefined;

  return (
    <div className="card p-6">
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
          <div className="flex items-center gap-2 text-sm text-gray-500">
            {location && <span>{location}</span>}
            {showDistance && distanceKm !== undefined && (
              <>
                {location && <span>•</span>}
                <span className="text-primary-600 font-medium">
                  {formatDistance(distanceKm)} away
                </span>
              </>
            )}
          </div>
        </div>
      </div>
      {rescue.description && (
        <p className="text-gray-600 text-sm mt-4 line-clamp-3">{rescue.description}</p>
      )}
      <div className="mt-4 pt-4 border-t border-secondary-200">
        <div className="flex items-center justify-between">
          {'petCount' in rescue && (
            <span className="text-sm text-gray-500">
              {rescue.petCount} pet{rescue.petCount !== 1 ? 's' : ''} available
            </span>
          )}
          <Link
            to={`/rescues/${rescue.id}`}
            className="text-primary-500 text-sm font-medium hover:underline"
          >
            View Profile →
          </Link>
        </div>
      </div>
      {'website' in rescue && (rescue.website || ('email' in rescue && rescue.email) || ('phone' in rescue && rescue.phone)) && (
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
          {'email' in rescue && rescue.email && (
            <a
              href={`mailto:${rescue.email}`}
              className="text-xs text-gray-500 hover:text-primary-500"
            >
              Email
            </a>
          )}
          {'phone' in rescue && rescue.phone && (
            <a
              href={`tel:${rescue.phone}`}
              className="text-xs text-gray-500 hover:text-primary-500"
            >
              Call
            </a>
          )}
        </div>
      )}
    </div>
  );
}

import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Input } from '../components';
import apiClient from '../api/client';

interface RescueOrganization {
  id: string;
  name: string;
  description: string;
  location: string;
  website: string | null;
  email: string;
  phone: string | null;
  logoUrl: string | null;
  petCount: number;
}

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
      // Use mock data for demo
      setRescues(getMockRescues());
    } finally {
      setLoading(false);
    }
  };

  const filteredRescues = rescues.filter((rescue) => {
    if (search) {
      const searchLower = search.toLowerCase();
      return (
        rescue.name.toLowerCase().includes(searchLower) ||
        rescue.location.toLowerCase().includes(searchLower) ||
        rescue.description.toLowerCase().includes(searchLower)
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
          <div className="text-6xl mb-4">🏥</div>
          <h3 className="text-xl font-semibold text-gray-900 mb-2">No rescue organizations found</h3>
          <p className="text-gray-600">Try adjusting your search or check back later</p>
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
                      <span className="text-3xl">🏥</span>
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="text-lg font-semibold text-gray-900 mb-1">{rescue.name}</h3>
                    <p className="text-sm text-gray-500 mb-2">{rescue.location}</p>
                  </div>
                </div>
                <p className="text-gray-600 text-sm mt-4 line-clamp-3">{rescue.description}</p>
                <div className="mt-4 pt-4 border-t border-secondary-200">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-500">
                      {rescue.petCount} pet{rescue.petCount !== 1 ? 's' : ''} available
                    </span>
                    <Link
                      to={`/pets?rescue=${rescue.id}`}
                      className="text-primary-500 text-sm font-medium hover:underline"
                    >
                      View Pets
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
                  <a
                    href={`mailto:${rescue.email}`}
                    className="text-xs text-gray-500 hover:text-primary-500"
                  >
                    Email
                  </a>
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

// Mock data for demo purposes
function getMockRescues(): RescueOrganization[] {
  return [
    {
      id: 'r1',
      name: 'Happy Paws Rescue',
      description: 'Dedicated to rescuing and rehoming dogs and cats in the greater metropolitan area. We believe every pet deserves a loving home.',
      location: 'San Francisco, CA',
      website: 'https://happypawsrescue.org',
      email: 'info@happypawsrescue.org',
      phone: '(555) 123-4567',
      logoUrl: null,
      petCount: 12,
    },
    {
      id: 'r2',
      name: 'Second Chance Animal Shelter',
      description: 'A no-kill shelter providing care and adoption services for abandoned and surrendered pets since 2005.',
      location: 'Oakland, CA',
      website: 'https://secondchanceshelter.org',
      email: 'adopt@secondchanceshelter.org',
      phone: '(555) 234-5678',
      logoUrl: null,
      petCount: 28,
    },
    {
      id: 'r3',
      name: 'Furry Friends Foundation',
      description: 'Specializing in small animals including rabbits, guinea pigs, and hamsters. We provide education on proper care and adoption services.',
      location: 'San Jose, CA',
      website: null,
      email: 'hello@furryfriendsfoundation.org',
      phone: '(555) 345-6789',
      logoUrl: null,
      petCount: 15,
    },
    {
      id: 'r4',
      name: 'Golden Gate Pet Rescue',
      description: 'Rescuing pets from high-kill shelters and providing foster care until they find their forever homes.',
      location: 'Daly City, CA',
      website: 'https://ggpetrescue.org',
      email: 'rescue@ggpetrescue.org',
      phone: null,
      logoUrl: null,
      petCount: 8,
    },
    {
      id: 'r5',
      name: 'Paw Print Animal Rescue',
      description: 'A volunteer-run organization focusing on senior pets and pets with special needs who need extra love and care.',
      location: 'Berkeley, CA',
      website: 'https://pawprintrescue.org',
      email: 'info@pawprintrescue.org',
      phone: '(555) 456-7890',
      logoUrl: null,
      petCount: 6,
    },
    {
      id: 'r6',
      name: 'Bay Area Cat Coalition',
      description: 'Dedicated exclusively to feline rescue, TNR programs, and finding homes for cats of all ages.',
      location: 'Fremont, CA',
      website: 'https://bayareacats.org',
      email: 'cats@bayareacats.org',
      phone: '(555) 567-8901',
      logoUrl: null,
      petCount: 22,
    },
  ];
}

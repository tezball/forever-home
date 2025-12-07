import { useState, useEffect } from 'react';
import { PetCard, Select, Input } from '../components';
import type { Pet, Species, PetSize, PetStatus } from '../types';
import apiClient from '../api/client';

const speciesOptions = [
  { value: '', label: 'All Species' },
  { value: 'DOG', label: 'Dogs' },
  { value: 'CAT', label: 'Cats' },
  { value: 'RABBIT', label: 'Rabbits' },
  { value: 'BIRD', label: 'Birds' },
  { value: 'OTHER', label: 'Other' },
];

const sizeOptions = [
  { value: '', label: 'All Sizes' },
  { value: 'SMALL', label: 'Small' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'LARGE', label: 'Large' },
];

export function PetListPage() {
  const [pets, setPets] = useState<Pet[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filters, setFilters] = useState({
    species: '' as Species | '',
    size: '' as PetSize | '',
    search: '',
  });

  useEffect(() => {
    fetchPets();
  }, [filters.species, filters.size]);

  const fetchPets = async () => {
    setLoading(true);
    setError('');
    try {
      const params = new URLSearchParams();
      if (filters.species) params.append('species', filters.species);
      if (filters.size) params.append('size', filters.size);

      const response = await apiClient.get<Pet[]>(`/pets?${params.toString()}`);
      setPets(response.data);
    } catch {
      setError('Failed to load pets. Please try again.');
      // Use mock data for demo
      setPets(getMockPets());
    } finally {
      setLoading(false);
    }
  };

  const filteredPets = pets.filter((pet) => {
    if (filters.search) {
      const searchLower = filters.search.toLowerCase();
      return (
        pet.name.toLowerCase().includes(searchLower) ||
        pet.breed?.toLowerCase().includes(searchLower) ||
        pet.species.toLowerCase().includes(searchLower)
      );
    }
    return true;
  });

  return (
    <div className="container-app py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Browse Pets</h1>
        <p className="text-gray-600">Find your new best friend from our available pets</p>
      </div>

      {/* Filters */}
      <div className="bg-secondary-50 rounded-lg p-4 mb-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <Input
            placeholder="Search by name or breed..."
            value={filters.search}
            onChange={(e) => setFilters({ ...filters, search: e.target.value })}
          />
          <Select
            options={speciesOptions}
            value={filters.species}
            onChange={(e) => setFilters({ ...filters, species: e.target.value as Species })}
          />
          <Select
            options={sizeOptions}
            value={filters.size}
            onChange={(e) => setFilters({ ...filters, size: e.target.value as PetSize })}
          />
          <button
            onClick={() => setFilters({ species: '', size: '', search: '' })}
            className="text-primary-500 hover:underline text-sm"
          >
            Clear filters
          </button>
        </div>
      </div>

      {/* Results */}
      {loading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent" />
        </div>
      ) : error && pets.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-error-500">{error}</p>
        </div>
      ) : filteredPets.length === 0 ? (
        <div className="text-center py-12">
          <div className="text-6xl mb-4">🐾</div>
          <h3 className="text-xl font-semibold text-gray-900 mb-2">No pets found</h3>
          <p className="text-gray-600">Try adjusting your filters or check back later</p>
        </div>
      ) : (
        <>
          <p className="text-sm text-gray-500 mb-4">
            Showing {filteredPets.length} pet{filteredPets.length !== 1 ? 's' : ''}
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {filteredPets.map((pet) => (
              <PetCard key={pet.id} pet={pet} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}

// Mock data for demo purposes
function getMockPets(): Pet[] {
  return [
    {
      id: '1',
      name: 'Luna',
      species: 'DOG',
      breed: 'Siberian Husky',
      age: 2,
      ageUnit: 'YEARS',
      sex: 'FEMALE',
      size: 'MEDIUM',
      microchipId: 'MC123456',
      description: 'Luna is a friendly and energetic husky who loves long walks and playing in the snow.',
      healthNotes: null,
      status: 'AVAILABLE' as PetStatus,
      fosterId: 'f1',
      rescueOrgId: 'r1',
      createdAt: new Date().toISOString(),
      imageUrls: [],
    },
    {
      id: '2',
      name: 'Max',
      species: 'DOG',
      breed: 'Golden Retriever',
      age: 4,
      ageUnit: 'YEARS',
      sex: 'MALE',
      size: 'LARGE',
      microchipId: 'MC123457',
      description: 'Max is a gentle giant who loves everyone he meets. Great with kids and other pets.',
      healthNotes: null,
      status: 'AVAILABLE' as PetStatus,
      fosterId: 'f2',
      rescueOrgId: 'r1',
      createdAt: new Date().toISOString(),
      imageUrls: [],
    },
    {
      id: '3',
      name: 'Whiskers',
      species: 'CAT',
      breed: 'Maine Coon',
      age: 3,
      ageUnit: 'YEARS',
      sex: 'MALE',
      size: 'LARGE',
      microchipId: 'MC123458',
      description: 'Whiskers is a majestic Maine Coon who loves to cuddle and play with feather toys.',
      healthNotes: null,
      status: 'AVAILABLE' as PetStatus,
      fosterId: 'f3',
      rescueOrgId: 'r2',
      createdAt: new Date().toISOString(),
      imageUrls: [],
    },
    {
      id: '4',
      name: 'Bella',
      species: 'DOG',
      breed: 'French Bulldog',
      age: 1,
      ageUnit: 'YEARS',
      sex: 'FEMALE',
      size: 'SMALL',
      microchipId: 'MC123459',
      description: 'Bella is a playful and affectionate Frenchie who will steal your heart.',
      healthNotes: null,
      status: 'AVAILABLE' as PetStatus,
      fosterId: 'f4',
      rescueOrgId: 'r2',
      createdAt: new Date().toISOString(),
      imageUrls: [],
    },
    {
      id: '5',
      name: 'Milo',
      species: 'CAT',
      breed: 'Tabby',
      age: 6,
      ageUnit: 'MONTHS',
      sex: 'MALE',
      size: 'SMALL',
      microchipId: 'MC123460',
      description: 'Milo is a curious kitten who loves to explore and chase toys.',
      healthNotes: null,
      status: 'AVAILABLE' as PetStatus,
      fosterId: 'f5',
      rescueOrgId: 'r1',
      createdAt: new Date().toISOString(),
      imageUrls: [],
    },
    {
      id: '6',
      name: 'Coco',
      species: 'RABBIT',
      breed: 'Holland Lop',
      age: 2,
      ageUnit: 'YEARS',
      sex: 'FEMALE',
      size: 'SMALL',
      microchipId: 'MC123461',
      description: 'Coco is a sweet and gentle bunny who loves to hop around and munch on veggies.',
      healthNotes: null,
      status: 'AVAILABLE' as PetStatus,
      fosterId: 'f6',
      rescueOrgId: 'r3',
      createdAt: new Date().toISOString(),
      imageUrls: [],
    },
  ];
}

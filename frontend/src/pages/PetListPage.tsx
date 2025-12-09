import { useState, useEffect } from 'react';
import { PetCard, Select, Input } from '../components';
import type { Pet, Species, PetSize, PetSex } from '../types';
import apiClient from '../api/client';

const speciesOptions = [
  { value: '', label: 'All Species' },
  { value: 'DOG', label: 'Dogs' },
  { value: 'CAT', label: 'Cats' },
];

const sizeOptions = [
  { value: '', label: 'All Sizes' },
  { value: 'SMALL', label: 'Small' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'LARGE', label: 'Large' },
];

const sexOptions = [
  { value: '', label: 'All' },
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
];

export function PetListPage() {
  const [pets, setPets] = useState<Pet[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filters, setFilters] = useState({
    species: '' as Species | '',
    size: '' as PetSize | '',
    sex: '' as PetSex | '',
    search: '',
  });

  useEffect(() => {
    fetchPets();
  }, [filters.species, filters.size, filters.sex]);

  const fetchPets = async () => {
    setLoading(true);
    setError('');
    try {
      const params = new URLSearchParams();
      if (filters.species) params.append('species', filters.species);
      if (filters.size) params.append('size', filters.size);
      if (filters.sex) params.append('sex', filters.sex);

      const response = await apiClient.get<Pet[]>(`/pets?${params.toString()}`);
      setPets(response.data);
    } catch {
      setError('Failed to load pets. Please try again.');
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
        <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
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
          <Select
            options={sexOptions}
            value={filters.sex}
            onChange={(e) => setFilters({ ...filters, sex: e.target.value as PetSex })}
          />
          <button
            onClick={() => setFilters({ species: '', size: '', sex: '', search: '' })}
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

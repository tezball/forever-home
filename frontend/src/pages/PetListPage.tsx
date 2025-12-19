import { useState, useEffect, useMemo } from 'react';
import { PetCard, Select, Input, ErrorDisplay, getErrorMessage, SkeletonCard } from '../components';
import type { Pet, Species, PetSize, PetSex } from '../types';
import apiClient from '../api/client';
import { BREEDS, getBreedsBySpecies } from '../constants/breeds';

interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

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

const PAGE_SIZE = 12;

export function PetListPage() {
  const [pets, setPets] = useState<Pet[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [filters, setFilters] = useState({
    species: '' as Species | '',
    size: '' as PetSize | '',
    sex: '' as PetSex | '',
    breed: '',
    search: '',
  });

  // Get breed options based on selected species
  const breedOptions = useMemo(() => {
    const breeds = filters.species ? getBreedsBySpecies(filters.species) : BREEDS;
    return [
      { value: '', label: 'All Breeds' },
      ...breeds.map((b) => ({ value: b.value, label: b.label })),
    ];
  }, [filters.species]);

  useEffect(() => {
    // Reset to first page when filters change
    setCurrentPage(0);
  }, [filters.species, filters.size, filters.sex, filters.breed]);

  // Clear breed when species changes (breed may no longer be valid)
  useEffect(() => {
    if (filters.breed && filters.species) {
      const validBreeds = getBreedsBySpecies(filters.species);
      if (!validBreeds.find((b) => b.value === filters.breed)) {
        setFilters((prev) => ({ ...prev, breed: '' }));
      }
    }
  }, [filters.species, filters.breed]);

  useEffect(() => {
    fetchPets();
  }, [filters.species, filters.size, filters.sex, filters.breed, currentPage]);

  const fetchPets = async () => {
    setLoading(true);
    setError('');
    try {
      const params = new URLSearchParams();
      if (filters.species) params.append('species', filters.species);
      if (filters.size) params.append('size', filters.size);
      if (filters.sex) params.append('sex', filters.sex);
      if (filters.breed) params.append('breed', filters.breed);
      params.append('page', currentPage.toString());
      params.append('pageSize', PAGE_SIZE.toString());

      const response = await apiClient.get<PagedResponse<Pet>>(`/pets?${params.toString()}`);
      setPets(response.data.content);
      setTotalPages(response.data.totalPages);
      setTotalElements(response.data.totalElements);
    } catch (err) {
      setError(getErrorMessage(err));
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

  const handlePageChange = (newPage: number) => {
    setCurrentPage(newPage);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="container-app py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Browse Pets</h1>
        <p className="text-gray-600">Find your new best friend from our available pets</p>
      </div>

      {/* Filters */}
      <div className="bg-secondary-50 rounded-lg p-4 mb-4">
        <div className="flex flex-col gap-3">
          <Input
            placeholder="Search by name..."
            value={filters.search}
            onChange={(e) => setFilters({ ...filters, search: e.target.value })}
          />
          <div className="grid grid-cols-2 gap-2 sm:flex sm:gap-2 sm:flex-wrap">
            <Select
              options={speciesOptions}
              value={filters.species}
              onChange={(e) => setFilters({ ...filters, species: e.target.value as Species })}
            />
            <Select
              options={breedOptions}
              value={filters.breed}
              onChange={(e) => setFilters({ ...filters, breed: e.target.value })}
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
          </div>
        </div>
      </div>

      {/* Active Filter Chips */}
      {(filters.species || filters.breed || filters.size || filters.sex || filters.search) && (
        <div className="flex flex-wrap gap-2 mb-6">
          {filters.species && (
            <span className="inline-flex items-center gap-1 px-3 py-1 bg-primary-100 text-primary-700 rounded-full text-sm">
              {speciesOptions.find((o) => o.value === filters.species)?.label}
              <button
                onClick={() => setFilters({ ...filters, species: '', breed: '' })}
                className="hover:text-primary-900"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </span>
          )}
          {filters.breed && (
            <span className="inline-flex items-center gap-1 px-3 py-1 bg-primary-100 text-primary-700 rounded-full text-sm">
              {breedOptions.find((o) => o.value === filters.breed)?.label}
              <button
                onClick={() => setFilters({ ...filters, breed: '' })}
                className="hover:text-primary-900"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </span>
          )}
          {filters.size && (
            <span className="inline-flex items-center gap-1 px-3 py-1 bg-primary-100 text-primary-700 rounded-full text-sm">
              {sizeOptions.find((o) => o.value === filters.size)?.label}
              <button
                onClick={() => setFilters({ ...filters, size: '' })}
                className="hover:text-primary-900"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </span>
          )}
          {filters.sex && (
            <span className="inline-flex items-center gap-1 px-3 py-1 bg-primary-100 text-primary-700 rounded-full text-sm">
              {sexOptions.find((o) => o.value === filters.sex)?.label}
              <button
                onClick={() => setFilters({ ...filters, sex: '' })}
                className="hover:text-primary-900"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </span>
          )}
          {filters.search && (
            <span className="inline-flex items-center gap-1 px-3 py-1 bg-primary-100 text-primary-700 rounded-full text-sm">
              "{filters.search}"
              <button
                onClick={() => setFilters({ ...filters, search: '' })}
                className="hover:text-primary-900"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </span>
          )}
          <button
            onClick={() => setFilters({ species: '', size: '', sex: '', breed: '', search: '' })}
            className="text-primary-500 hover:underline text-sm"
          >
            Clear all
          </button>
        </div>
      )}

      {/* Results */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {[...Array(8)].map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : error && pets.length === 0 ? (
        <ErrorDisplay
          title="Unable to load pets"
          message={error}
          onRetry={fetchPets}
        />
      ) : filteredPets.length === 0 ? (
        <div className="text-center py-12">
          <div className="text-6xl mb-4">
            {pets.length === 0 ? '🐾' : filters.search ? '🔍' : '🔎'}
          </div>
          <h3 className="text-xl font-semibold text-gray-900 mb-2">
            {pets.length === 0
              ? 'No pets available right now'
              : filters.search
                ? `No pets found matching "${filters.search}"`
                : 'No pets match your filters'}
          </h3>
          <p className="text-gray-600 mb-6">
            {pets.length === 0
              ? 'Check back soon - new pets are added regularly!'
              : filters.search
                ? 'Try a different search term or check your spelling.'
                : 'Try adjusting your filters to find more options.'}
          </p>
          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            {filters.search && (
              <button
                onClick={() => setFilters({ ...filters, search: '' })}
                className="inline-flex items-center justify-center px-4 py-2 border border-gray-300 bg-white text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
              >
                <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
                Clear Search
              </button>
            )}
            {(filters.species || filters.size || filters.sex || filters.breed) && (
              <button
                onClick={() => setFilters({ ...filters, species: '', size: '', sex: '', breed: '' })}
                className="inline-flex items-center justify-center px-4 py-2 border border-gray-300 bg-white text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
              >
                Clear Filters
              </button>
            )}
            {(filters.species || filters.size || filters.sex || filters.breed || filters.search) && (
              <button
                onClick={() => setFilters({ species: '', size: '', sex: '', breed: '', search: '' })}
                className="inline-flex items-center justify-center px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
              >
                Clear All
              </button>
            )}
          </div>
        </div>
      ) : (
        <>
          <p className="text-sm text-gray-500 mb-4">
            Showing {filteredPets.length} of {totalElements} pet{totalElements !== 1 ? 's' : ''}
            {totalPages > 1 && ` (Page ${currentPage + 1} of ${totalPages})`}
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {filteredPets.map((pet) => (
              <PetCard key={pet.id} pet={pet} />
            ))}
          </div>

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="mt-8 flex justify-center items-center gap-2">
              <button
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 0}
                className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Previous
              </button>

              <div className="flex gap-1">
                {/* Show first page */}
                {currentPage > 2 && (
                  <>
                    <button
                      onClick={() => handlePageChange(0)}
                      className="px-3 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                    >
                      1
                    </button>
                    {currentPage > 3 && <span className="px-2 py-2 text-gray-500">...</span>}
                  </>
                )}

                {/* Show pages around current page */}
                {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                  const pageNum = Math.max(0, Math.min(currentPage - 2, totalPages - 5)) + i;
                  if (pageNum < 0 || pageNum >= totalPages) return null;
                  return (
                    <button
                      key={pageNum}
                      onClick={() => handlePageChange(pageNum)}
                      className={`px-3 py-2 border rounded-md text-sm font-medium ${
                        pageNum === currentPage
                          ? 'border-primary-500 bg-primary-500 text-white'
                          : 'border-gray-300 text-gray-700 bg-white hover:bg-gray-50'
                      }`}
                    >
                      {pageNum + 1}
                    </button>
                  );
                })}

                {/* Show last page */}
                {currentPage < totalPages - 3 && (
                  <>
                    {currentPage < totalPages - 4 && <span className="px-2 py-2 text-gray-500">...</span>}
                    <button
                      onClick={() => handlePageChange(totalPages - 1)}
                      className="px-3 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                    >
                      {totalPages}
                    </button>
                  </>
                )}
              </div>

              <button
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage >= totalPages - 1}
                className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

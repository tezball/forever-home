import { useState, useEffect } from 'react';
import { PetCard, Select, Input, ErrorDisplay, getErrorMessage, SkeletonCard } from '../components';
import type { Pet, Species, PetSize, PetSex } from '../types';
import apiClient from '../api/client';

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
    search: '',
  });

  useEffect(() => {
    // Reset to first page when filters change
    setCurrentPage(0);
  }, [filters.species, filters.size, filters.sex]);

  useEffect(() => {
    fetchPets();
  }, [filters.species, filters.size, filters.sex, currentPage]);

  const fetchPets = async () => {
    setLoading(true);
    setError('');
    try {
      const params = new URLSearchParams();
      if (filters.species) params.append('species', filters.species);
      if (filters.size) params.append('size', filters.size);
      if (filters.sex) params.append('sex', filters.sex);
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
      <div className="bg-secondary-50 rounded-lg p-4 mb-8">
        <div className="flex flex-col gap-3">
          <Input
            placeholder="Search by name or breed..."
            value={filters.search}
            onChange={(e) => setFilters({ ...filters, search: e.target.value })}
          />
          <div className="flex gap-2 overflow-x-auto pb-2 md:pb-0 md:flex-wrap scrollbar-hide">
            <div className="flex-shrink-0 w-32">
              <Select
                options={speciesOptions}
                value={filters.species}
                onChange={(e) => setFilters({ ...filters, species: e.target.value as Species })}
              />
            </div>
            <div className="flex-shrink-0 w-28">
              <Select
                options={sizeOptions}
                value={filters.size}
                onChange={(e) => setFilters({ ...filters, size: e.target.value as PetSize })}
              />
            </div>
            <div className="flex-shrink-0 w-24">
              <Select
                options={sexOptions}
                value={filters.sex}
                onChange={(e) => setFilters({ ...filters, sex: e.target.value as PetSex })}
              />
            </div>
            <button
              onClick={() => setFilters({ species: '', size: '', sex: '', search: '' })}
              className="flex-shrink-0 text-primary-500 hover:underline text-sm whitespace-nowrap"
            >
              Clear filters
            </button>
          </div>
        </div>
      </div>

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
          <div className="text-6xl mb-4">🐾</div>
          <h3 className="text-xl font-semibold text-gray-900 mb-2">
            {pets.length === 0 ? 'No pets available right now' : 'No pets match your filters'}
          </h3>
          <p className="text-gray-600 mb-6">
            {pets.length === 0
              ? 'Check back soon - new pets are added regularly!'
              : 'Try adjusting your search or filters to find more options.'}
          </p>
          {(filters.species || filters.size || filters.sex || filters.search) && (
            <button
              onClick={() => setFilters({ species: '', size: '', sex: '', search: '' })}
              className="inline-flex items-center px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors"
            >
              Clear All Filters
            </button>
          )}
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

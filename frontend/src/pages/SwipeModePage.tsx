import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import apiClient from '../api/client';
import {
  SwipeContainer,
  SwipeCard,
  SwipeFilters,
  SwipeActions,
  SwipeEmptyState,
} from '../components/swipe';
import { formatSize } from '../utils';
import type { Pet, Species, PetSize, PetSex } from '../types';

interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

const PAGE_SIZE = 20;

export function SwipeModePage() {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const [pets, setPets] = useState<Pet[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [showDetails, setShowDetails] = useState(false);

  const [filters, setFilters] = useState({
    species: '' as Species | '',
    size: '' as PetSize | '',
    sex: '' as PetSex | '',
  });

  // Lock body scroll on mount
  useEffect(() => {
    document.body.style.overflow = 'hidden';
    document.body.style.position = 'fixed';
    document.body.style.width = '100%';
    document.body.style.height = '100%';

    return () => {
      document.body.style.overflow = '';
      document.body.style.position = '';
      document.body.style.width = '';
      document.body.style.height = '';
    };
  }, []);

  const fetchPets = useCallback(async (page: number, append = false) => {
    if (append) {
      setLoadingMore(true);
    }
    try {
      const params = new URLSearchParams();
      if (filters.species) params.append('species', filters.species);
      if (filters.size) params.append('size', filters.size);
      if (filters.sex) params.append('sex', filters.sex);
      params.append('page', page.toString());
      params.append('pageSize', PAGE_SIZE.toString());

      const response = await apiClient.get<PagedResponse<Pet>>(`/pets?${params.toString()}`);
      const newPets = response.data.content;

      if (append) {
        setPets((prev) => [...prev, ...newPets]);
      } else {
        setPets(newPets);
      }

      setHasMore(!response.data.last);
      setCurrentPage(page);
    } catch {
      // Silently handle errors
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  }, [filters]);

  useEffect(() => {
    setLoading(true);
    setCurrentIndex(0);
    setPets([]);
    fetchPets(0, false);
  }, [filters, fetchPets]);

  useEffect(() => {
    if (pets.length > 0 && currentIndex >= pets.length - 5 && hasMore && !loading && !loadingMore) {
      fetchPets(currentPage + 1, true);
    }
  }, [currentIndex, pets.length, hasMore, loading, loadingMore, currentPage, fetchPets]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (showDetails) return;
      if (e.key === 'ArrowLeft') handlePass();
      if (e.key === 'ArrowRight') handleLike();
      if (e.key === 'Escape') navigate('/pets');
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [currentIndex, pets.length, showDetails, navigate]);

  const handleSwipe = (direction: 'pass' | 'like') => {
    if (direction === 'like') {
      handleLike();
    } else {
      handlePass();
    }
  };

  const handlePass = () => {
    if (currentIndex < pets.length) {
      setCurrentIndex((prev) => prev + 1);
    }
  };

  const handleLike = async () => {
    if (currentIndex >= pets.length) return;

    const pet = pets[currentIndex];

    if (isAuthenticated) {
      try {
        await apiClient.post(`/favorites/${pet.id}`);
      } catch {
        // Already favorited or error - continue anyway
      }
    }

    setCurrentIndex((prev) => prev + 1);
  };

  const handleInfo = () => {
    setShowDetails(true);
  };

  const handleFilterChange = (newFilters: { species: Species | ''; size: PetSize | ''; sex: PetSex | '' }) => {
    setFilters(newFilters);
  };

  const handleClearFilters = () => {
    setFilters({ species: '', size: '', sex: '' });
  };

  const handleReset = () => {
    setCurrentIndex(0);
  };

  const currentPet = pets[currentIndex];
  const nextPet = pets[currentIndex + 1];
  const allSwiped = !loading && currentIndex >= pets.length;
  const noResults = !loading && pets.length === 0;

  const renderDetailOverlay = () => {
    if (!showDetails || !currentPet) return null;

    const ageDisplay = `${currentPet.age} ${currentPet.ageUnit === 'YEARS' ? (currentPet.age === 1 ? 'year' : 'years') : (currentPet.age === 1 ? 'month' : 'months')}`;

    return (
      <div
        className="fixed inset-0 z-[110] bg-black/60 backdrop-blur-sm flex items-end justify-center"
        onClick={() => setShowDetails(false)}
      >
        <div
          className="bg-white w-full max-w-lg rounded-t-3xl max-h-[85vh] overflow-y-auto"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Handle bar */}
          <div className="sticky top-0 bg-white pt-3 pb-2 flex justify-center">
            <div className="w-12 h-1.5 bg-gray-300 rounded-full" />
          </div>

          <div className="px-6 pb-8">
            {currentPet.imageUrls?.[0] && (
              <img
                src={currentPet.imageUrls[0]}
                alt={currentPet.name}
                className="w-full h-56 object-cover rounded-2xl mb-4"
              />
            )}

            <h2 className="text-2xl font-bold text-gray-900 mb-2">{currentPet.name}</h2>

            <div className="flex flex-wrap gap-2 mb-4">
              <span className="px-3 py-1 bg-primary-100 text-primary-700 rounded-full text-sm font-medium">
                {currentPet.species === 'DOG' ? 'Dog' : 'Cat'}
              </span>
              <span className="px-3 py-1 bg-gray-100 text-gray-700 rounded-full text-sm">
                {ageDisplay}
              </span>
              <span className="px-3 py-1 bg-gray-100 text-gray-700 rounded-full text-sm">
                {formatSize(currentPet.size)}
              </span>
              <span className="px-3 py-1 bg-gray-100 text-gray-700 rounded-full text-sm">
                {currentPet.sex === 'MALE' ? 'Male' : 'Female'}
              </span>
            </div>

            {currentPet.description && (
              <div className="mb-4">
                <h3 className="font-semibold text-gray-900 mb-1">About</h3>
                <p className="text-gray-600">{currentPet.description}</p>
              </div>
            )}

            {currentPet.healthNotes && (
              <div className="mb-6">
                <h3 className="font-semibold text-gray-900 mb-1">Health Notes</h3>
                <p className="text-gray-600">{currentPet.healthNotes}</p>
              </div>
            )}

            <Link
              to={`/pets/${currentPet.id}`}
              className="block w-full py-3 bg-primary-500 text-white text-center rounded-xl font-medium hover:bg-primary-600 transition-colors"
            >
              View Full Profile
            </Link>
          </div>
        </div>
      </div>
    );
  };

  return (
    <>
      <SwipeContainer
        onClose={() => navigate('/pets')}
        filters={
          <SwipeFilters
            species={filters.species}
            size={filters.size}
            sex={filters.sex}
            onChange={handleFilterChange}
          />
        }
        actions={
          <SwipeActions
            onPass={handlePass}
            onLike={handleLike}
            disabled={loading || !currentPet}
          />
        }
      >
        {/* Loading more indicator */}
        {loadingMore && (
          <div className="absolute top-4 left-1/2 -translate-x-1/2 z-50">
            <div className="flex items-center gap-2 px-4 py-2 bg-black/50 backdrop-blur-sm rounded-full">
              <div className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent" />
              <span className="text-sm text-white font-medium">Loading more...</span>
            </div>
          </div>
        )}

        {loading ? (
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="animate-spin rounded-full h-12 w-12 border-4 border-white border-t-transparent" />
          </div>
        ) : noResults ? (
          <SwipeEmptyState type="no-results" onClearFilters={handleClearFilters} />
        ) : allSwiped ? (
          <SwipeEmptyState type="all-swiped" onReset={handleReset} />
        ) : (
          <>
            {nextPet && (
              <SwipeCard
                key={`next-${nextPet.id}`}
                pet={nextPet}
                onSwipe={() => {}}
                onTap={() => {}}
                isTop={false}
              />
            )}
            {currentPet && (
              <SwipeCard
                key={`current-${currentPet.id}`}
                pet={currentPet}
                onSwipe={handleSwipe}
                onTap={handleInfo}
                isTop={true}
              />
            )}
          </>
        )}
      </SwipeContainer>

      {renderDetailOverlay()}
    </>
  );
}

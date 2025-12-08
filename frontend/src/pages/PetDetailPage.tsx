import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Button, Modal, ImageCarousel } from '../components';
import type { Pet, PetStatus } from '../types';
import apiClient from '../api/client';

const statusLabels: Record<PetStatus, string> = {
  DRAFT: 'Draft',
  PENDING_RESCUE: 'Pending Review',
  PENDING_VET: 'Pending Vet',
  AVAILABLE: 'Available',
  IN_PROGRESS: 'Application In Progress',
  ADOPTED: 'Adopted',
  WITHDRAWN: 'Withdrawn',
  ON_HOLD: 'On Hold',
};

const statusClasses: Record<PetStatus, string> = {
  DRAFT: 'status-pending',
  PENDING_RESCUE: 'status-pending',
  PENDING_VET: 'status-pending',
  AVAILABLE: 'status-available',
  IN_PROGRESS: 'status-pending',
  ADOPTED: 'status-adopted',
  WITHDRAWN: 'status-withdrawn',
  ON_HOLD: 'status-onhold',
};

export function PetDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { isAuthenticated, user } = useAuth();
  const [pet, setPet] = useState<Pet | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [favorite, setFavorite] = useState(false);
  const [applyModalOpen, setApplyModalOpen] = useState(false);
  const [applying, setApplying] = useState(false);

  useEffect(() => {
    fetchPet();
  }, [id]);

  const fetchPet = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await apiClient.get<Pet>(`/pets/${id}`);
      setPet(response.data);
    } catch {
      setError('Failed to load pet details');
      // Use mock data for demo
      setPet(getMockPet(id!));
    } finally {
      setLoading(false);
    }
  };

  const toggleFavorite = async () => {
    if (!isAuthenticated) return;
    try {
      if (favorite) {
        await apiClient.delete(`/favorites/${id}`);
      } else {
        await apiClient.post(`/favorites/${id}`);
      }
      setFavorite(!favorite);
    } catch {
      // Ignore errors for demo
      setFavorite(!favorite);
    }
  };

  const handleApply = async () => {
    setApplying(true);
    try {
      await apiClient.post('/applications', { petId: id });
      setApplyModalOpen(false);
      // Show success message or redirect
    } catch {
      // Handle error
    } finally {
      setApplying(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent" />
      </div>
    );
  }

  if (error && !pet) {
    return (
      <div className="container-app py-12 text-center">
        <p className="text-error-500">{error}</p>
        <Link to="/pets" className="text-primary-500 hover:underline mt-4 inline-block">
          Back to Browse
        </Link>
      </div>
    );
  }

  if (!pet) return null;

  const canApply = isAuthenticated && user?.role === 'ADOPTER' && pet.status === 'AVAILABLE';

  return (
    <div className="container-app py-8">
      {/* Back link */}
      <Link to="/pets" className="inline-flex items-center text-gray-600 hover:text-primary-500 mb-6">
        <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
        </svg>
        Back to Browse
      </Link>

      <div className="grid md:grid-cols-2 gap-8">
        {/* Image Gallery */}
        <div>
          <ImageCarousel
            images={pet.imageUrls}
            petName={pet.name}
            placeholderUrl={`https://placedog.net/600/450?id=${pet.id.slice(0, 8)}`}
          />
        </div>

        {/* Pet Info */}
        <div>
          <div className="flex items-start justify-between mb-4">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">{pet.name}</h1>
              <p className="text-lg text-gray-600">{pet.breed || pet.species}</p>
            </div>
            {isAuthenticated && (
              <button
                onClick={toggleFavorite}
                className="p-2 rounded-full hover:bg-secondary-100 transition-colors"
                aria-label={favorite ? 'Remove from favorites' : 'Add to favorites'}
              >
                <svg
                  className={`w-7 h-7 ${favorite ? 'text-accent-500 fill-current' : 'text-gray-400'}`}
                  fill={favorite ? 'currentColor' : 'none'}
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
                  />
                </svg>
              </button>
            )}
          </div>

          <span className={`status-badge ${statusClasses[pet.status]} mb-6`}>
            {statusLabels[pet.status]}
          </span>

          {/* Quick Info */}
          <div className="grid grid-cols-3 gap-4 my-6 p-4 bg-secondary-50 rounded-lg">
            <div className="text-center">
              <p className="text-lg font-semibold text-gray-900">
                {pet.age} {pet.ageUnit.toLowerCase()}
              </p>
              <p className="text-sm text-gray-500">Age</p>
            </div>
            <div className="text-center border-x border-secondary-200">
              <p className="text-lg font-semibold text-gray-900 capitalize">{pet.size.toLowerCase()}</p>
              <p className="text-sm text-gray-500">Size</p>
            </div>
            <div className="text-center">
              <p className="text-lg font-semibold text-gray-900 capitalize">{pet.sex.toLowerCase()}</p>
              <p className="text-sm text-gray-500">Sex</p>
            </div>
          </div>

          {/* Description */}
          {pet.description && (
            <div className="mb-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-2">About {pet.name}</h2>
              <p className="text-gray-600 leading-relaxed">{pet.description}</p>
            </div>
          )}

          {/* Health Notes */}
          {pet.healthNotes && (
            <div className="mb-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-2">Health Information</h2>
              <p className="text-gray-600">{pet.healthNotes}</p>
            </div>
          )}

          {/* Action Button */}
          <div className="mt-8">
            {canApply ? (
              <Button variant="primary" size="lg" className="w-full" onClick={() => setApplyModalOpen(true)}>
                Apply to Adopt {pet.name}
              </Button>
            ) : pet.status === 'AVAILABLE' && !isAuthenticated ? (
              <div className="text-center">
                <p className="text-gray-600 mb-4">Sign in to apply for adoption</p>
                <Link to="/login">
                  <Button variant="primary" size="lg" className="w-full">
                    Sign In to Apply
                  </Button>
                </Link>
              </div>
            ) : pet.status !== 'AVAILABLE' ? (
              <div className="text-center p-4 bg-secondary-100 rounded-lg">
                <p className="text-gray-600">
                  This pet is currently not available for adoption.
                </p>
              </div>
            ) : null}
          </div>
        </div>
      </div>

      {/* Apply Modal */}
      <Modal
        isOpen={applyModalOpen}
        onClose={() => setApplyModalOpen(false)}
        title={`Apply to Adopt ${pet?.name}`}
      >
        <div className="space-y-4">
          <p className="text-gray-600">
            You're about to submit an adoption application for {pet?.name}. The rescue organization
            will review your application and contact you with next steps.
          </p>
          <div className="flex gap-4 pt-4">
            <Button variant="outline" onClick={() => setApplyModalOpen(false)} className="flex-1">
              Cancel
            </Button>
            <Button variant="primary" loading={applying} onClick={handleApply} className="flex-1">
              Submit Application
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}

// Mock data for demo
function getMockPet(id: string): Pet {
  const pets: Record<string, Pet> = {
    '1': {
      id: '1',
      name: 'Luna',
      species: 'DOG',
      breed: 'Siberian Husky',
      age: 2,
      ageUnit: 'YEARS',
      sex: 'FEMALE',
      size: 'MEDIUM',
      microchipId: 'MC123456',
      description: 'Luna is a friendly and energetic husky who loves long walks and playing in the snow. She gets along well with other dogs and is great with children. Luna is fully trained and knows basic commands. She would thrive in an active household with a yard.',
      healthNotes: 'Up to date on all vaccinations. Spayed. No known health issues.',
      status: 'AVAILABLE',
      fosterId: 'f1',
      rescueOrgId: 'r1',
      createdAt: new Date().toISOString(),
      imageUrls: [],
    },
  };

  return pets[id] || {
    id,
    name: 'Unknown Pet',
    species: 'DOG',
    breed: null,
    age: 1,
    ageUnit: 'YEARS',
    sex: 'MALE',
    size: 'MEDIUM',
    microchipId: 'UNKNOWN',
    description: 'Pet information not available.',
    healthNotes: null,
    status: 'AVAILABLE',
    fosterId: '',
    rescueOrgId: null,
    createdAt: new Date().toISOString(),
    imageUrls: [],
  };
}

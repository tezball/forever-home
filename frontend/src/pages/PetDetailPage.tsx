import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Button, Modal, ImageCarousel, PetTimeline, Textarea } from '../components';
import { formatSize, formatSex, formatBreed } from '../utils';
import type { Pet, PetStatus, RescueOrganization } from '../types';
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

interface ApplicationCheckResponse {
  hasApplied: boolean;
  activeApplicationCount: number;
}

const MAX_ACTIVE_APPLICATIONS = 3;

export function PetDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { isAuthenticated, user } = useAuth();
  const [pet, setPet] = useState<Pet | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [favorite, setFavorite] = useState(false);
  const [applyModalOpen, setApplyModalOpen] = useState(false);
  const [applying, setApplying] = useState(false);
  const [applicationMessage, setApplicationMessage] = useState('');
  const [hasApplied, setHasApplied] = useState(false);
  const [activeApplicationCount, setActiveApplicationCount] = useState(0);
  const [applicationSuccess, setApplicationSuccess] = useState(false);
  const [rescueOrg, setRescueOrg] = useState<RescueOrganization | null>(null);

  useEffect(() => {
    fetchPet();
    if (isAuthenticated && user?.role === 'ADOPTER') {
      fetchFavoriteStatus();
      checkApplicationStatus();
    }
  }, [id, isAuthenticated, user?.role]);

  const fetchPet = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await apiClient.get<Pet>(`/pets/${id}`);
      setPet(response.data);
      // Fetch rescue org info if available
      if (response.data.rescueOrgId) {
        fetchRescueOrg(response.data.rescueOrgId);
      }
    } catch {
      setError('Failed to load pet details');
    } finally {
      setLoading(false);
    }
  };

  const fetchRescueOrg = async (orgId: string) => {
    try {
      const response = await apiClient.get<RescueOrganization>(`/rescues/${orgId}`);
      setRescueOrg(response.data);
    } catch {
      // Silently fail - rescue org info is optional
    }
  };

  const fetchFavoriteStatus = async () => {
    try {
      const response = await apiClient.get<boolean>(`/favorites/${id}/status`);
      setFavorite(response.data);
    } catch {
      // Ignore errors - user might not have favorited this pet
    }
  };

  const checkApplicationStatus = async () => {
    try {
      const response = await apiClient.get<ApplicationCheckResponse>(`/applications/check/${id}`);
      setHasApplied(response.data.hasApplied);
      setActiveApplicationCount(response.data.activeApplicationCount);
    } catch {
      // Ignore errors - endpoint might not be available
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

  const [applyError, setApplyError] = useState('');

  const handleApply = async () => {
    setApplying(true);
    setApplyError('');
    try {
      await apiClient.post('/applications', { petId: id, message: applicationMessage });
      setApplyModalOpen(false);
      setHasApplied(true);
      setActiveApplicationCount(prev => prev + 1);
      setApplicationSuccess(true);
      setApplicationMessage('');
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const response = (err as { response?: { data?: { message?: string } } }).response;
        setApplyError(response?.data?.message || 'Failed to submit application. Please try again.');
      } else {
        setApplyError('Failed to submit application. Please try again.');
      }
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

  const isAdopter = isAuthenticated && user?.role === 'ADOPTER';
  const canApply = isAdopter && pet.status === 'AVAILABLE' && !hasApplied && activeApplicationCount < MAX_ACTIVE_APPLICATIONS;
  const atApplicationLimit = isAdopter && activeApplicationCount >= MAX_ACTIVE_APPLICATIONS;

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
              <p className="text-lg text-gray-600">{formatBreed(pet.breed) || pet.species}</p>
            </div>
            {isAuthenticated && (
              <button
                onClick={toggleFavorite}
                className="p-3 -m-1 rounded-full hover:bg-secondary-100 transition-colors"
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
              <p className="text-lg font-semibold text-gray-900">{formatSize(pet.size)}</p>
              <p className="text-sm text-gray-500">Size</p>
            </div>
            <div className="text-center">
              <p className="text-lg font-semibold text-gray-900">{formatSex(pet.sex)}</p>
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

          {/* Rescue Organization */}
          {rescueOrg && (
            <div className="mb-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-2">Rescue Organization</h2>
              <Link
                to={`/rescues/${rescueOrg.id}`}
                className="flex items-center gap-3 p-4 bg-secondary-50 rounded-lg hover:bg-secondary-100 transition-colors"
              >
                {rescueOrg.logoUrl ? (
                  <img
                    src={rescueOrg.logoUrl}
                    alt={rescueOrg.name}
                    className="w-12 h-12 rounded-full object-cover"
                  />
                ) : (
                  <div className="w-12 h-12 rounded-full bg-primary-100 flex items-center justify-center">
                    <svg className="w-6 h-6 text-primary-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                    </svg>
                  </div>
                )}
                <div className="flex-1">
                  <p className="font-medium text-gray-900">{rescueOrg.name}</p>
                  {rescueOrg.location && (
                    <p className="text-sm text-gray-500">{rescueOrg.location}</p>
                  )}
                </div>
                <svg className="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                </svg>
              </Link>
            </div>
          )}

          {/* Status Timeline */}
          <div className="mb-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Status History</h2>
            <div className="bg-white border border-gray-200 rounded-lg p-4">
              <PetTimeline petId={pet.id} />
            </div>
          </div>

          {/* Action Button */}
          <div className="mt-8">
            {/* Success message after applying */}
            {applicationSuccess && (
              <div className="p-4 bg-success-50 border border-success-200 rounded-lg mb-4">
                <div className="flex items-center gap-2 text-success-700">
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  <span className="font-medium">Application submitted successfully!</span>
                </div>
                <p className="text-success-600 text-sm mt-1">
                  The rescue organization will review your application and contact you.
                </p>
              </div>
            )}

            {/* Already applied state */}
            {isAdopter && hasApplied && !applicationSuccess && (
              <div className="text-center p-4 bg-primary-50 border border-primary-200 rounded-lg">
                <div className="flex items-center justify-center gap-2 text-primary-700 mb-2">
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <span className="font-medium">You've already applied for {pet.name}</span>
                </div>
                <p className="text-primary-600 text-sm">
                  Check your <Link to="/adopter" className="underline hover:no-underline">dashboard</Link> to track your application status.
                </p>
              </div>
            )}

            {/* At application limit */}
            {isAdopter && !hasApplied && atApplicationLimit && pet.status === 'AVAILABLE' && (
              <div className="text-center p-4 bg-warning-50 border border-warning-200 rounded-lg">
                <div className="flex items-center justify-center gap-2 text-warning-700 mb-2">
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                  <span className="font-medium">Application Limit Reached</span>
                </div>
                <p className="text-warning-600 text-sm">
                  You already have {MAX_ACTIVE_APPLICATIONS} active applications. Please wait for a response or withdraw an existing application.
                </p>
              </div>
            )}

            {/* Can apply */}
            {canApply && (
              <Button variant="primary" size="lg" className="w-full" onClick={() => setApplyModalOpen(true)}>
                Apply to Adopt {pet.name}
              </Button>
            )}

            {/* Not authenticated */}
            {pet.status === 'AVAILABLE' && !isAuthenticated && (
              <div className="text-center">
                <p className="text-gray-600 mb-4">Sign in to apply for adoption</p>
                <Link to="/login">
                  <Button variant="primary" size="lg" className="w-full">
                    Sign In to Apply
                  </Button>
                </Link>
              </div>
            )}

            {/* Authenticated but not an adopter */}
            {pet.status === 'AVAILABLE' && isAuthenticated && user?.role !== 'ADOPTER' && (
              <div className="text-center p-4 bg-secondary-100 rounded-lg">
                <p className="text-gray-600">
                  To apply for adoption, you need an adopter account.{' '}
                  <Link to="/register" className="text-primary-500 hover:underline">
                    Create an adopter account
                  </Link>
                </p>
              </div>
            )}

            {/* Pet not available */}
            {pet.status !== 'AVAILABLE' && (
              <div className="text-center p-4 bg-secondary-100 rounded-lg">
                <p className="text-gray-600">
                  This pet is currently not available for adoption.
                </p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Apply Modal */}
      <Modal
        isOpen={applyModalOpen}
        onClose={() => {
          setApplyModalOpen(false);
          setApplyError('');
        }}
        title={`Apply to Adopt ${pet?.name}`}
      >
        <div className="space-y-4">
          <p className="text-gray-600">
            You're about to submit an adoption application for {pet?.name}. The rescue organization
            will review your application and contact you with next steps.
          </p>

          <div>
            <label htmlFor="applicationMessage" className="block text-sm font-medium text-gray-700 mb-1">
              Tell us why you'd like to adopt {pet?.name} (optional)
            </label>
            <Textarea
              id="applicationMessage"
              value={applicationMessage}
              onChange={(e) => setApplicationMessage(e.target.value)}
              placeholder="Share a bit about yourself, your living situation, and why you think you'd be a great match..."
              rows={4}
            />
          </div>

          {activeApplicationCount > 0 && (
            <p className="text-sm text-gray-500">
              You currently have {activeApplicationCount} active application{activeApplicationCount !== 1 ? 's' : ''}.
              {activeApplicationCount >= MAX_ACTIVE_APPLICATIONS - 1 && activeApplicationCount < MAX_ACTIVE_APPLICATIONS && (
                <span className="text-warning-600"> This will be your last allowed application.</span>
              )}
            </p>
          )}

          {applyError && (
            <div className="bg-error-50 border border-error-200 text-error-700 px-4 py-3 rounded">
              {applyError}
            </div>
          )}

          <div className="flex gap-4 pt-4">
            <Button variant="outline" onClick={() => {
              setApplyModalOpen(false);
              setApplyError('');
            }} className="flex-1">
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

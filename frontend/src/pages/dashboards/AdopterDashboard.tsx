import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { Button, Modal, HelpIcon } from '../../components';
import type { Pet, AdoptionApplication, PetStatus } from '../../types';
import apiClient from '../../api/client';
import { formatRelativeTime, formatBreed } from '../../utils';

const statusColors: Record<PetStatus, string> = {
  DRAFT: 'bg-gray-200 text-gray-900',
  PENDING_RESCUE: 'bg-yellow-100 text-yellow-900',
  PENDING_VET: 'bg-blue-100 text-blue-900',
  AVAILABLE: 'bg-green-100 text-green-900',
  IN_PROGRESS: 'bg-purple-100 text-purple-900',
  ADOPTED: 'bg-primary-100 text-primary-900',
  WITHDRAWN: 'bg-red-100 text-red-900',
  ON_HOLD: 'bg-orange-100 text-orange-900',
};

const statusLabels: Record<PetStatus, string> = {
  DRAFT: 'Draft',
  PENDING_RESCUE: 'Pending Review',
  PENDING_VET: 'Pending Vet',
  AVAILABLE: 'Available',
  IN_PROGRESS: 'In Progress',
  ADOPTED: 'Adopted',
  WITHDRAWN: 'Withdrawn',
  ON_HOLD: 'On Hold',
};

interface FavoritePetCardProps {
  pet: Pet;
  onRemove: () => void;
  isRemoving: boolean;
}

function FavoritePetCard({ pet, onRemove, isRemoving }: FavoritePetCardProps) {
  const placeholderImage = `https://placedog.net/400/300?id=${pet.id.slice(0, 8)}`;

  return (
    <div className="card hover:shadow-lg transition-shadow relative group">
      {/* Remove button - always visible on mobile, hover on desktop */}
      <button
        onClick={(e) => {
          e.preventDefault();
          e.stopPropagation();
          onRemove();
        }}
        disabled={isRemoving}
        className="absolute top-3 right-3 z-10 p-2 bg-white/90 hover:bg-white rounded-full shadow-md opacity-100 sm:opacity-0 sm:group-hover:opacity-100 transition-opacity disabled:opacity-50"
        aria-label={`Remove ${pet.name} from liked pets`}
        title="Remove from liked pets"
      >
        {isRemoving ? (
          <div className="w-5 h-5 border-2 border-gray-400 border-t-transparent rounded-full animate-spin" />
        ) : (
          <svg className="w-5 h-5 text-accent-500 fill-current" viewBox="0 0 24 24">
            <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
          </svg>
        )}
      </button>

      <Link to={`/pets/${pet.id}`} className="block">
        <div className="aspect-w-4 aspect-h-3 relative">
          <img
            src={pet.imageUrls[0] || placeholderImage}
            alt={pet.name}
            className="w-full h-48 object-cover"
          />
          {pet.imageUrls.length === 0 && (
            <div className="absolute bottom-2 right-2 bg-warning-100 text-warning-700 text-xs px-2 py-1 rounded">
              No photos
            </div>
          )}
        </div>
        <div className="p-4">
          <div className="flex justify-between items-start mb-2">
            <h3 className="text-lg font-semibold text-gray-900">{pet.name}</h3>
            <span className={`status-badge ${statusColors[pet.status]}`}>
              {statusLabels[pet.status]}
            </span>
          </div>
          <p className="text-sm text-gray-500 mb-2">
            {formatBreed(pet.breed) || pet.species} • {pet.age} {pet.ageUnit.toLowerCase()} • {pet.sex.toLowerCase()}
          </p>
          {pet.description && (
            <p className="text-sm text-gray-600 line-clamp-2">{pet.description}</p>
          )}
        </div>
      </Link>
    </div>
  );
}

export function AdopterDashboard() {
  const { user } = useAuth();
  const [favorites, setFavorites] = useState<Pet[]>([]);
  const [applications, setApplications] = useState<AdoptionApplication[]>([]);
  const [loading, setLoading] = useState(true);
  const [withdrawModalOpen, setWithdrawModalOpen] = useState(false);
  const [selectedApplication, setSelectedApplication] = useState<AdoptionApplication | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [error, setError] = useState('');
  const [removingFavoriteId, setRemovingFavoriteId] = useState<string | null>(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [favoritesRes, applicationsRes] = await Promise.all([
        apiClient.get<Pet[]>('/favorites/pets'),
        apiClient.get<AdoptionApplication[]>('/applications'),
      ]);
      setFavorites(favoritesRes.data);
      setApplications(applicationsRes.data);
    } catch {
      // Demo data
      setFavorites([]);
      setApplications([]);
    } finally {
      setLoading(false);
    }
  };

  const openWithdrawModal = (app: AdoptionApplication, e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setSelectedApplication(app);
    setWithdrawModalOpen(true);
    setError('');
  };

  const handleWithdrawApplication = async () => {
    if (!selectedApplication) return;

    setActionLoading(true);
    setError('');

    try {
      await apiClient.delete(`/applications/${selectedApplication.id}`);
      setSuccessMessage(`Your application for ${selectedApplication.petName} has been withdrawn.`);
      setWithdrawModalOpen(false);
      setSelectedApplication(null);
      fetchData(); // Refresh data
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const response = (err as { response?: { data?: { message?: string } } }).response;
        setError(response?.data?.message || 'Failed to withdraw application');
      } else {
        setError('Failed to withdraw application');
      }
    } finally {
      setActionLoading(false);
    }
  };

  const canWithdraw = (status: string) => {
    return ['SUBMITTED', 'UNDER_REVIEW'].includes(status);
  };

  const handleRemoveFavorite = async (petId: string, petName: string) => {
    setRemovingFavoriteId(petId);
    try {
      await apiClient.delete(`/favorites/${petId}`);
      setFavorites(prev => prev.filter(p => p.id !== petId));
      setSuccessMessage(`${petName} has been removed from your saved pets.`);
    } catch {
      setError('Failed to remove pet from favorites');
    } finally {
      setRemovingFavoriteId(null);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'SUBMITTED':
        return <span className="px-3 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-800">Submitted</span>;
      case 'UNDER_REVIEW':
        return <span className="px-3 py-1 text-xs font-semibold rounded-full bg-warning-100 text-warning-800">Under Review</span>;
      case 'APPROVED':
        return <span className="px-3 py-1 text-xs font-semibold rounded-full bg-success-100 text-success-800">Approved</span>;
      case 'FINALIZED':
        return <span className="px-3 py-1 text-xs font-semibold rounded-full bg-primary-100 text-primary-800">Adopted!</span>;
      case 'REJECTED':
        return <span className="px-3 py-1 text-xs font-semibold rounded-full bg-error-100 text-error-800">Rejected</span>;
      case 'WITHDRAWN':
        return <span className="px-3 py-1 text-xs font-semibold rounded-full bg-gray-100 text-gray-800">Withdrawn</span>;
      default:
        return null;
    }
  };

  return (
    <div className="container-app py-8">
      <div className="flex justify-between items-center mb-8">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-3xl font-bold text-gray-900">Adopter Dashboard</h1>
            <HelpIcon title="Adoption Application Tips" size="md">
              <div className="space-y-4">
                <p>Here's how to make your adoption applications stand out:</p>
                <div className="space-y-3">
                  <div>
                    <h4 className="font-semibold text-gray-900">Write a compelling application</h4>
                    <ul className="list-disc pl-4 text-sm mt-1 space-y-1">
                      <li>Share your experience with pets</li>
                      <li>Describe your living situation honestly</li>
                      <li>Explain why this specific pet interests you</li>
                      <li>Mention any other pets in your home</li>
                    </ul>
                  </div>
                  <div>
                    <h4 className="font-semibold text-gray-900">Application limits</h4>
                    <p className="text-sm mt-1">You can have up to 3 active applications at a time. This ensures you can give each application proper attention.</p>
                  </div>
                  <div>
                    <h4 className="font-semibold text-gray-900">Application statuses</h4>
                    <ul className="text-sm mt-1 space-y-1">
                      <li><strong>Submitted:</strong> Waiting for rescue review</li>
                      <li><strong>Under Review:</strong> Rescue is evaluating your application</li>
                      <li><strong>Approved:</strong> Contact rescue to schedule pickup!</li>
                      <li><strong>Rejected:</strong> You can apply for other pets</li>
                    </ul>
                  </div>
                </div>
              </div>
            </HelpIcon>
          </div>
          <p className="text-gray-600">Welcome back, {user?.name}</p>
        </div>
        <Link to="/pets">
          <Button variant="primary">Browse Pets</Button>
        </Link>
      </div>

      {/* Success Message */}
      {successMessage && (
        <div className="mb-6 bg-success-50 border border-success-200 text-success-700 px-4 py-3 rounded flex justify-between items-center">
          <span>{successMessage}</span>
          <button onClick={() => setSuccessMessage('')} className="text-success-700 hover:text-success-900">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      )}

      {loading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent" />
        </div>
      ) : (
        <div className="space-y-8">
          {/* Quick Actions - show when user has no applications and no favorites */}
          {applications.length === 0 && favorites.length === 0 && (
            <section className="card p-8 text-center">
              <div className="text-6xl mb-4">🐾</div>
              <h2 className="text-2xl font-semibold text-gray-900 mb-2">Welcome to Forever Home!</h2>
              <p className="text-gray-600 mb-6 max-w-lg mx-auto">
                Start your journey to find your perfect companion. Browse available pets, save your favorites, and submit adoption applications.
              </p>
              <div className="flex flex-wrap justify-center gap-4">
                <Link to="/pets">
                  <Button variant="primary">Browse Available Pets</Button>
                </Link>
                <Link to="/rescues">
                  <Button variant="outline">View Rescue Organizations</Button>
                </Link>
              </div>
            </section>
          )}

          {/* Action Needed - Approved applications requiring user action */}
          {applications.filter(a => a.status === 'APPROVED').length > 0 && (
            <section>
              <div className="flex items-center gap-2 mb-4">
                <h2 className="text-xl font-semibold text-gray-900">Action Needed</h2>
                <span className="px-2 py-0.5 text-xs font-medium bg-warning-100 text-warning-700 rounded-full">
                  {applications.filter(a => a.status === 'APPROVED').length}
                </span>
              </div>
              <div className="card border-2 border-warning-200 bg-warning-50 divide-y divide-warning-200">
                {applications.filter(a => a.status === 'APPROVED').map((app) => (
                  <div
                    key={app.id}
                    className="flex items-center justify-between p-4"
                  >
                    <Link to={`/pets/${app.petId}`} className="flex items-center gap-4 flex-1">
                      <img
                        src={app.petImageUrl || `https://placedog.net/60/60?id=${app.petId.slice(0, 8)}`}
                        alt={app.petName}
                        className="w-14 h-14 rounded-lg object-cover flex-shrink-0"
                      />
                      <div className="flex-1">
                        <p className="font-medium text-gray-900">{app.petName}</p>
                        <p className="text-sm text-success-600 font-medium">
                          Approved! Contact the rescue to schedule pickup.
                        </p>
                      </div>
                    </Link>
                    <div className="flex items-center gap-3">
                      {getStatusBadge(app.status)}
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* Pending Applications */}
          {applications.filter(a => ['SUBMITTED', 'UNDER_REVIEW'].includes(a.status)).length > 0 && (
            <section>
              <h2 className="text-xl font-semibold text-gray-900 mb-4">Pending Applications</h2>
              <div className="card divide-y divide-secondary-200">
                {applications.filter(a => ['SUBMITTED', 'UNDER_REVIEW'].includes(a.status)).map((app) => (
                  <div
                    key={app.id}
                    className="flex items-center justify-between p-4 hover:bg-secondary-50"
                  >
                    <Link to={`/pets/${app.petId}`} className="flex items-center gap-4 flex-1">
                      <img
                        src={app.petImageUrl || `https://placedog.net/60/60?id=${app.petId.slice(0, 8)}`}
                        alt={app.petName}
                        className="w-14 h-14 rounded-lg object-cover flex-shrink-0"
                      />
                      <div className="flex-1">
                        <p className="font-medium text-gray-900">{app.petName}</p>
                        <p className="text-sm text-gray-500">
                          Applied {formatRelativeTime(app.submittedAt)}
                        </p>
                      </div>
                    </Link>
                    <div className="flex items-center gap-3">
                      {getStatusBadge(app.status)}
                      {canWithdraw(app.status) && (
                        <button
                          onClick={(e) => openWithdrawModal(app, e)}
                          className="text-sm text-error-600 hover:text-error-700 font-medium"
                        >
                          Withdraw
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* Completed - Adopted pets (collapsible) */}
          {applications.filter(a => a.status === 'FINALIZED').length > 0 && (
            <section>
              <details className="group">
                <summary className="flex items-center gap-2 cursor-pointer list-none mb-4">
                  <svg className="w-4 h-4 text-gray-500 transition-transform group-open:rotate-90" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                  </svg>
                  <h2 className="text-xl font-semibold text-gray-900">Adopted</h2>
                  <span className="text-sm text-gray-500">({applications.filter(a => a.status === 'FINALIZED').length})</span>
                </summary>
                <div className="card divide-y divide-secondary-200">
                  {applications.filter(a => a.status === 'FINALIZED').map((app) => (
                    <div
                      key={app.id}
                      className="flex items-center justify-between p-4 hover:bg-secondary-50"
                    >
                      <Link to={`/pets/${app.petId}`} className="flex items-center gap-4 flex-1">
                        <img
                          src={app.petImageUrl || `https://placedog.net/60/60?id=${app.petId.slice(0, 8)}`}
                          alt={app.petName}
                          className="w-14 h-14 rounded-lg object-cover flex-shrink-0"
                        />
                        <div className="flex-1">
                          <p className="font-medium text-gray-900">{app.petName}</p>
                          <p className="text-sm text-gray-500">
                            Adopted {formatRelativeTime(app.submittedAt)}
                          </p>
                        </div>
                      </Link>
                      <div className="flex items-center gap-3">
                        {getStatusBadge(app.status)}
                      </div>
                    </div>
                  ))}
                </div>
              </details>
            </section>
          )}

          {/* Rejected Applications (collapsible) */}
          {applications.filter(a => a.status === 'REJECTED').length > 0 && (
            <section>
              <details className="group">
                <summary className="flex items-center gap-2 cursor-pointer list-none mb-4">
                  <svg className="w-4 h-4 text-gray-500 transition-transform group-open:rotate-90" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                  </svg>
                  <h2 className="text-xl font-semibold text-gray-900">Rejected</h2>
                  <span className="text-sm text-gray-500">({applications.filter(a => a.status === 'REJECTED').length})</span>
                </summary>
                <div className="card divide-y divide-secondary-200">
                  {applications.filter(a => a.status === 'REJECTED').map((app) => (
                    <div
                      key={app.id}
                      className="p-4 hover:bg-secondary-50"
                    >
                      <div className="flex items-center justify-between">
                        <Link to={`/pets/${app.petId}`} className="flex items-center gap-4 flex-1">
                          <img
                            src={app.petImageUrl || `https://placedog.net/60/60?id=${app.petId.slice(0, 8)}`}
                            alt={app.petName}
                            className="w-14 h-14 rounded-lg object-cover flex-shrink-0"
                          />
                          <div className="flex-1">
                            <p className="font-medium text-gray-900">{app.petName}</p>
                            <p className="text-sm text-gray-500">
                              Applied {formatRelativeTime(app.submittedAt)}
                            </p>
                          </div>
                        </Link>
                        {getStatusBadge(app.status)}
                      </div>
                      {app.rejectionReason && (
                        <div className="mt-3 ml-18 p-2 bg-error-50 border border-error-200 rounded text-sm">
                          <span className="font-medium text-error-700">Reason: </span>
                          <span className="text-error-600">{app.rejectionReason}</span>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </details>
            </section>
          )}

          {/* Empty state for applications */}
          {applications.length === 0 && favorites.length > 0 && (
            <section>
              <h2 className="text-xl font-semibold text-gray-900 mb-4">My Applications</h2>
              <div className="card p-4 bg-secondary-50 flex items-center justify-between">
                <p className="text-gray-600">No applications yet.</p>
                <Link to="/pets" className="text-primary-500 hover:underline text-sm font-medium">
                  Find a Pet to Adopt
                </Link>
              </div>
            </section>
          )}

          {/* Liked Pets Section */}
          <section>
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold text-gray-900">Liked Pets</h2>
              {favorites.length > 0 && (
                <Link to="/pets" className="text-primary-500 hover:underline text-sm font-medium">
                  Browse More Pets
                </Link>
              )}
            </div>
            {favorites.length > 0 ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {favorites.map((pet) => (
                  <FavoritePetCard
                    key={pet.id}
                    pet={pet}
                    onRemove={() => handleRemoveFavorite(pet.id, pet.name)}
                    isRemoving={removingFavoriteId === pet.id}
                  />
                ))}
              </div>
            ) : (
              <div className="card p-6 bg-secondary-50 text-center">
                <div className="text-4xl mb-3">
                  <svg className="w-12 h-12 mx-auto text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                  </svg>
                </div>
                <p className="text-gray-600 mb-3">No liked pets yet.</p>
                <p className="text-sm text-gray-500 mb-4">
                  Browse available pets and click the heart icon to save your favorites.
                </p>
                <Link to="/pets">
                  <Button variant="primary" size="sm">Browse Pets</Button>
                </Link>
              </div>
            )}
          </section>
        </div>
      )}

      {/* Withdraw Application Modal */}
      <Modal
        isOpen={withdrawModalOpen}
        onClose={() => {
          setWithdrawModalOpen(false);
          setSelectedApplication(null);
          setError('');
        }}
        title="Withdraw Application"
      >
        <div className="space-y-4">
          {selectedApplication && (
            <>
              <div className="flex items-center gap-4 p-4 bg-gray-50 rounded-lg">
                <img
                  src={selectedApplication.petImageUrl || `https://placedog.net/60/60?id=${selectedApplication.petId.slice(0, 8)}`}
                  alt={selectedApplication.petName}
                  className="w-14 h-14 rounded-lg object-cover"
                />
                <div>
                  <h4 className="font-semibold text-gray-900">{selectedApplication.petName}</h4>
                  <p className="text-sm text-gray-600">
                    Applied {new Date(selectedApplication.submittedAt).toLocaleDateString()}
                  </p>
                </div>
              </div>

              <div className="bg-warning-50 border border-warning-200 rounded-lg p-4">
                <div className="flex gap-3">
                  <svg className="w-5 h-5 text-warning-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                  <div>
                    <h4 className="font-medium text-warning-800">Are you sure?</h4>
                    <p className="text-sm text-warning-700 mt-1">
                      Withdrawing your application will remove you from consideration for adopting {selectedApplication.petName}.
                      You can submit a new application later if the pet is still available.
                    </p>
                  </div>
                </div>
              </div>

              {error && (
                <div className="bg-error-50 border border-error-200 text-error-700 px-4 py-3 rounded">
                  {error}
                </div>
              )}

              <div className="flex gap-3 pt-2">
                <Button
                  variant="outline"
                  onClick={() => {
                    setWithdrawModalOpen(false);
                    setSelectedApplication(null);
                    setError('');
                  }}
                  className="flex-1"
                >
                  Keep Application
                </Button>
                <Button
                  variant="danger"
                  onClick={handleWithdrawApplication}
                  loading={actionLoading}
                  className="flex-1"
                >
                  Withdraw Application
                </Button>
              </div>
            </>
          )}
        </div>
      </Modal>
    </div>
  );
}

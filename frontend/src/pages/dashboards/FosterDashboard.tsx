import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { Button, PetCard, Modal } from '../../components';
import type { Pet } from '../../types';
import apiClient from '../../api/client';
import { formatBreed } from '../../utils';

export function FosterDashboard() {
  const { user } = useAuth();
  const [pets, setPets] = useState<Pet[]>([]);
  const [loading, setLoading] = useState(true);
  const [withdrawModalOpen, setWithdrawModalOpen] = useState(false);
  const [selectedPet, setSelectedPet] = useState<Pet | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  useEffect(() => {
    fetchMyPets();
  }, []);

  const openWithdrawModal = (pet: Pet) => {
    setSelectedPet(pet);
    setWithdrawModalOpen(true);
    setError('');
  };

  const handleWithdrawPet = async () => {
    if (!selectedPet) return;

    setActionLoading(true);
    setError('');

    try {
      await apiClient.post(`/pets/${selectedPet.id}/withdraw`);
      setSuccessMessage(`${selectedPet.name} has been withdrawn from adoption.`);
      setWithdrawModalOpen(false);
      setSelectedPet(null);
      fetchMyPets(); // Refresh the list
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const response = (err as { response?: { data?: { message?: string } } }).response;
        setError(response?.data?.message || 'Failed to withdraw pet');
      } else {
        setError('Failed to withdraw pet');
      }
    } finally {
      setActionLoading(false);
    }
  };

  const fetchMyPets = async () => {
    try {
      const response = await apiClient.get<Pet[]>('/pets/my');
      setPets(response.data);
    } catch {
      // Demo data
      setPets([]);
    } finally {
      setLoading(false);
    }
  };

  const draftPets = pets.filter((p) => p.status === 'DRAFT');
  const pendingRescuePets = pets.filter((p) => p.status === 'PENDING_RESCUE');
  const pendingVetPets = pets.filter((p) => p.status === 'PENDING_VET');
  const availablePets = pets.filter((p) => p.status === 'AVAILABLE');
  const inProgressPets = pets.filter((p) => p.status === 'IN_PROGRESS');
  const adoptedPets = pets.filter((p) => p.status === 'ADOPTED');
  const withdrawnPets = pets.filter((p) => p.status === 'WITHDRAWN');

  return (
    <div className="container-app py-8">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Foster Dashboard</h1>
          <p className="text-gray-600">Welcome back, {user?.name}</p>
        </div>
        <Link to="/foster/pets/new">
          <Button variant="primary">Register a Pet</Button>
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
      ) : pets.length === 0 ? (
        <div className="card p-12 text-center">
          <div className="text-6xl mb-4">🐾</div>
          <h3 className="text-xl font-semibold text-gray-900 mb-2">No pets registered yet</h3>
          <p className="text-gray-600 mb-6">
            Start by registering a pet you'd like to rehome.
          </p>
          <Link to="/foster/pets/new">
            <Button variant="primary">Register Your First Pet</Button>
          </Link>
        </div>
      ) : (
        <div className="space-y-8">
          {/* Status Overview Stats - Only show non-zero counts */}
          <div className="flex flex-wrap gap-3 mb-2">
            {draftPets.length > 0 && (
              <div className="card px-4 py-2 flex items-center gap-2">
                <span className="text-xl font-bold text-gray-500">{draftPets.length}</span>
                <span className="text-sm text-gray-500">Drafts</span>
              </div>
            )}
            {(pendingRescuePets.length > 0 || pendingVetPets.length > 0) && (
              <div className="card px-4 py-2 flex items-center gap-2">
                <span className="text-xl font-bold text-yellow-600">{pendingRescuePets.length + pendingVetPets.length}</span>
                <span className="text-sm text-gray-500">Pending</span>
              </div>
            )}
            {availablePets.length > 0 && (
              <div className="card px-4 py-2 flex items-center gap-2">
                <span className="text-xl font-bold text-success-600">{availablePets.length}</span>
                <span className="text-sm text-gray-500">Available</span>
              </div>
            )}
            {inProgressPets.length > 0 && (
              <div className="card px-4 py-2 flex items-center gap-2">
                <span className="text-xl font-bold text-purple-600">{inProgressPets.length}</span>
                <span className="text-sm text-gray-500">In Progress</span>
              </div>
            )}
            {adoptedPets.length > 0 && (
              <div className="card px-4 py-2 flex items-center gap-2">
                <span className="text-xl font-bold text-primary-600">{adoptedPets.length}</span>
                <span className="text-sm text-gray-500">Rehomed</span>
              </div>
            )}
          </div>

          {/* Draft Pets - with prominent CTA */}
          {draftPets.length > 0 && (
            <section>
              <div className="flex items-center gap-2 mb-4">
                <h2 className="text-xl font-semibold text-gray-900">Drafts</h2>
                <span className="text-sm text-gray-500">- Ready to submit for review</span>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {draftPets.map((pet) => (
                  <div key={pet.id} className="card hover:shadow-lg transition-shadow border-2 border-dashed border-gray-300">
                    <Link to={`/pets/${pet.id}`} className="block">
                      <div className="relative">
                        <img
                          src={pet.imageUrls[0] || `https://placedog.net/400/300?id=${pet.id.slice(0, 8)}`}
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
                          <span className="status-badge bg-gray-100 text-gray-800">Draft</span>
                        </div>
                        <p className="text-sm text-gray-500 mb-2">
                          {formatBreed(pet.breed) || pet.species} • {pet.age} {pet.ageUnit.toLowerCase()} • {pet.sex.toLowerCase()}
                        </p>
                      </div>
                    </Link>
                    <div className="px-4 pb-4 space-y-2">
                      <Link to={`/foster/pets/${pet.id}/submit`} className="block">
                        <Button variant="primary" size="sm" className="w-full">
                          Continue → Submit for Review
                        </Button>
                      </Link>
                      <div className="flex gap-2">
                        <Link to={`/foster/pets/${pet.id}/edit`} className="flex-1">
                          <Button variant="outline" size="sm" className="w-full">
                            Edit
                          </Button>
                        </Link>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => openWithdrawModal(pet)}
                          className="text-error-600"
                        >
                          Withdraw
                        </Button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* Pending Rescue Review */}
          {pendingRescuePets.length > 0 && (
            <section>
              <div className="flex items-center gap-2 mb-4">
                <h2 className="text-xl font-semibold text-gray-900">Pending Rescue Review</h2>
                <span className="text-sm text-gray-500">- Awaiting rescue organization approval</span>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {pendingRescuePets.map((pet) => (
                  <PetCard key={pet.id} pet={pet} showEditButton onWithdraw={openWithdrawModal} />
                ))}
              </div>
            </section>
          )}

          {/* Pending Vet */}
          {pendingVetPets.length > 0 && (
            <section>
              <div className="flex items-center gap-2 mb-4">
                <h2 className="text-xl font-semibold text-gray-900">Pending Vet Verification</h2>
                <span className="text-sm text-gray-500">- Awaiting veterinary sign-off</span>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {pendingVetPets.map((pet) => (
                  <PetCard key={pet.id} pet={pet} showEditButton onWithdraw={openWithdrawModal} />
                ))}
              </div>
            </section>
          )}

          {/* Available for Adoption */}
          {availablePets.length > 0 && (
            <section>
              <div className="flex items-center gap-2 mb-4">
                <h2 className="text-xl font-semibold text-gray-900">Available for Adoption</h2>
                <span className="text-sm text-success-600">- Listed and visible to adopters</span>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {availablePets.map((pet) => (
                  <PetCard key={pet.id} pet={pet} showEditButton onWithdraw={openWithdrawModal} />
                ))}
              </div>
            </section>
          )}

          {/* In Progress */}
          {inProgressPets.length > 0 && (
            <section>
              <div className="flex items-center gap-2 mb-4">
                <h2 className="text-xl font-semibold text-gray-900">Adoption In Progress</h2>
                <span className="text-sm text-purple-600">- Has approved adoption application</span>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {inProgressPets.map((pet) => (
                  <PetCard key={pet.id} pet={pet} showEditButton onWithdraw={openWithdrawModal} />
                ))}
              </div>
            </section>
          )}

          {/* Rehomed */}
          {adoptedPets.length > 0 && (
            <section>
              <div className="flex items-center gap-2 mb-4">
                <h2 className="text-xl font-semibold text-gray-900">Rehomed</h2>
                <span className="text-sm text-primary-600">Found their forever homes!</span>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {adoptedPets.map((pet) => (
                  <PetCard key={pet.id} pet={pet} />
                ))}
              </div>
            </section>
          )}

          {/* Withdrawn */}
          {withdrawnPets.length > 0 && (
            <section>
              <div className="flex items-center gap-2 mb-4">
                <h2 className="text-xl font-semibold text-gray-900">Withdrawn</h2>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {withdrawnPets.map((pet) => (
                  <PetCard key={pet.id} pet={pet} />
                ))}
              </div>
            </section>
          )}
        </div>
      )}

      {/* Withdraw Confirmation Modal */}
      <Modal
        isOpen={withdrawModalOpen}
        onClose={() => {
          setWithdrawModalOpen(false);
          setSelectedPet(null);
          setError('');
        }}
        title="Withdraw Pet"
      >
        <div className="space-y-4">
          {selectedPet && (
            <>
              <div className="flex items-center gap-4 p-4 bg-gray-50 rounded-lg">
                <img
                  src={selectedPet.imageUrls[0] || `https://placedog.net/100/100?id=${selectedPet.id.slice(0, 8)}`}
                  alt={selectedPet.name}
                  className="w-16 h-16 rounded-lg object-cover"
                />
                <div>
                  <h4 className="font-semibold text-gray-900">{selectedPet.name}</h4>
                  <p className="text-sm text-gray-600">{selectedPet.breed || selectedPet.species}</p>
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
                      Withdrawing {selectedPet.name} will remove them from the adoption process.
                      {selectedPet.status === 'IN_PROGRESS' && ' This pet has active adoption applications that will be cancelled.'}
                      {' '}This action cannot be undone.
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
                    setSelectedPet(null);
                    setError('');
                  }}
                  className="flex-1"
                >
                  Cancel
                </Button>
                <Button
                  variant="danger"
                  onClick={handleWithdrawPet}
                  loading={actionLoading}
                  className="flex-1"
                >
                  Withdraw Pet
                </Button>
              </div>
            </>
          )}
        </div>
      </Modal>
    </div>
  );
}

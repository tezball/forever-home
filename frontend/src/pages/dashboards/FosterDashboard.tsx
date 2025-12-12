import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { Button, PetCard, Modal } from '../../components';
import type { Pet } from '../../types';
import apiClient from '../../api/client';

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
  const pendingPets = pets.filter((p) => ['PENDING_RESCUE', 'PENDING_VET'].includes(p.status));
  const activePets = pets.filter((p) => ['AVAILABLE', 'IN_PROGRESS'].includes(p.status));
  const completedPets = pets.filter((p) => ['ADOPTED', 'WITHDRAWN'].includes(p.status));

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
          {/* Draft Pets */}
          {draftPets.length > 0 && (
            <section>
              <h2 className="text-xl font-semibold text-gray-900 mb-4">Drafts</h2>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {draftPets.map((pet) => (
                  <PetCard key={pet.id} pet={pet} showEditButton onWithdraw={openWithdrawModal} />
                ))}
              </div>
            </section>
          )}

          {/* Pending Pets */}
          {pendingPets.length > 0 && (
            <section>
              <h2 className="text-xl font-semibold text-gray-900 mb-4">Pending Review</h2>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {pendingPets.map((pet) => (
                  <PetCard key={pet.id} pet={pet} showEditButton onWithdraw={openWithdrawModal} />
                ))}
              </div>
            </section>
          )}

          {/* Active Pets */}
          {activePets.length > 0 && (
            <section>
              <h2 className="text-xl font-semibold text-gray-900 mb-4">Active Listings</h2>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {activePets.map((pet) => (
                  <PetCard key={pet.id} pet={pet} showEditButton onWithdraw={openWithdrawModal} />
                ))}
              </div>
            </section>
          )}

          {/* Completed */}
          {completedPets.length > 0 && (
            <section>
              <h2 className="text-xl font-semibold text-gray-900 mb-4">Completed</h2>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {completedPets.map((pet) => (
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

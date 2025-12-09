import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { Button, PetCard, Modal } from '../../components';
import type { Pet, AdoptionApplication } from '../../types';
import apiClient from '../../api/client';

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

  const pendingApplications = applications.filter((a) => a.status === 'SUBMITTED' || a.status === 'UNDER_REVIEW');
  const approvedApplications = applications.filter((a) => a.status === 'APPROVED');
  const rejectedApplications = applications.filter((a) => a.status === 'REJECTED');

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'SUBMITTED':
        return <span className="px-3 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-800">Submitted</span>;
      case 'UNDER_REVIEW':
        return <span className="px-3 py-1 text-xs font-semibold rounded-full bg-warning-100 text-warning-800">Under Review</span>;
      case 'APPROVED':
        return <span className="px-3 py-1 text-xs font-semibold rounded-full bg-success-100 text-success-800">Approved</span>;
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
          <h1 className="text-3xl font-bold text-gray-900">Adopter Dashboard</h1>
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

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        <div className="card p-4 text-center">
          <p className="text-3xl font-bold text-accent-500">{favorites.length}</p>
          <p className="text-sm text-gray-600">Favorites</p>
        </div>
        <div className="card p-4 text-center">
          <p className="text-3xl font-bold text-warning-600">{pendingApplications.length}</p>
          <p className="text-sm text-gray-600">Pending</p>
        </div>
        <div className="card p-4 text-center">
          <p className="text-3xl font-bold text-success-500">{approvedApplications.length}</p>
          <p className="text-sm text-gray-600">Approved</p>
        </div>
        <div className="card p-4 text-center">
          <p className="text-3xl font-bold text-gray-500">{rejectedApplications.length}</p>
          <p className="text-sm text-gray-600">Rejected</p>
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent" />
        </div>
      ) : (
        <div className="space-y-8">
          {/* Applications */}
          {applications.length > 0 && (
            <section>
              <h2 className="text-xl font-semibold text-gray-900 mb-4">My Applications</h2>
              <div className="card divide-y divide-secondary-200">
                {applications.map((app) => (
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
                      <div>
                        <p className="font-medium text-gray-900">{app.petName}</p>
                        <p className="text-sm text-gray-500">
                          Applied {new Date(app.submittedAt).toLocaleDateString()}
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

          {/* Favorites */}
          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Saved Pets</h2>
            {favorites.length > 0 ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {favorites.map((pet) => (
                  <PetCard key={pet.id} pet={pet} />
                ))}
              </div>
            ) : (
              <div className="card p-12 text-center">
                <div className="text-6xl mb-4">❤️</div>
                <h3 className="text-xl font-semibold text-gray-900 mb-2">No saved pets yet</h3>
                <p className="text-gray-600 mb-6">
                  Browse available pets and save your favorites.
                </p>
                <Link to="/pets">
                  <Button variant="primary">Browse Pets</Button>
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

import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { Button, Modal, PetCard, Textarea } from '../../components';
import type { Pet, AdoptionApplication } from '../../types';
import apiClient from '../../api/client';
import { formatRelativeTime, formatBreed } from '../../utils';

type StatusFilter = 'ALL' | 'PENDING_VET' | 'AVAILABLE' | 'IN_PROGRESS' | 'ON_HOLD' | 'ADOPTED';

const statusFilterOptions: { value: StatusFilter; label: string }[] = [
  { value: 'ALL', label: 'All' },
  { value: 'PENDING_VET', label: 'Pending Vet' },
  { value: 'AVAILABLE', label: 'Available' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'ON_HOLD', label: 'On Hold' },
  { value: 'ADOPTED', label: 'Adopted' },
];

interface ApprovalRequestCount {
  count: number;
}

export function RescueDashboard() {
  const { user } = useAuth();
  const [pendingPets, setPendingPets] = useState<Pet[]>([]);
  const [activePets, setActivePets] = useState<Pet[]>([]);
  const [applications, setApplications] = useState<AdoptionApplication[]>([]);
  const [vetRequestCount, setVetRequestCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [declineModalOpen, setDeclineModalOpen] = useState(false);
  const [selectedPet, setSelectedPet] = useState<Pet | null>(null);
  const [declineReason, setDeclineReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [activeStatusFilter, setActiveStatusFilter] = useState<StatusFilter>('ALL');

  // Expanded pet details
  const [expandedPetId, setExpandedPetId] = useState<string | null>(null);

  // Application modals
  const [selectedApplication, setSelectedApplication] = useState<AdoptionApplication | null>(null);
  const [viewApplicationModalOpen, setViewApplicationModalOpen] = useState(false);
  const [rejectApplicationModalOpen, setRejectApplicationModalOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [finalizeModalOpen, setFinalizeModalOpen] = useState(false);

  // Hold modal state
  const [holdModalOpen, setHoldModalOpen] = useState(false);
  const [selectedPetForHold, setSelectedPetForHold] = useState<Pet | null>(null);
  const [holdReason, setHoldReason] = useState('');

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [pendingRes, activeRes, appsRes, vetCountRes] = await Promise.all([
        apiClient.get<Pet[]>('/rescues/my/pending'),
        apiClient.get<Pet[]>('/rescues/my/pets'),
        apiClient.get<AdoptionApplication[]>('/rescues/my/applications'),
        apiClient.get<ApprovalRequestCount>('/rescue-org/approval-requests/count'),
      ]);
      setPendingPets(pendingRes.data);
      setActivePets(activeRes.data);
      setApplications(appsRes.data);
      setVetRequestCount(vetCountRes.data.count);
    } catch {
      // Demo data
      setPendingPets([]);
      setActivePets([]);
      setApplications([]);
      setVetRequestCount(0);
    } finally {
      setLoading(false);
    }
  };

  const togglePetDetails = (petId: string) => {
    setExpandedPetId(expandedPetId === petId ? null : petId);
  };

  const handleAcceptPet = async (pet: Pet) => {
    setActionLoading(true);
    setError('');
    try {
      await apiClient.post(`/pets/${pet.id}/accept`);
      setSuccessMessage(`${pet.name} has been accepted and sent for vet verification`);
      // Remove from pending and add to active
      setPendingPets((prev) => prev.filter((p) => p.id !== pet.id));
      setActivePets((prev) => [...prev, { ...pet, status: 'PENDING_VET' }]);
      setTimeout(() => setSuccessMessage(''), 5000);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to accept pet';
      setError(errorMessage);
    } finally {
      setActionLoading(false);
    }
  };

  const openDeclineModal = (pet: Pet) => {
    setSelectedPet(pet);
    setDeclineReason('');
    setDeclineModalOpen(true);
  };

  const handleDeclinePet = async () => {
    if (!selectedPet || !declineReason.trim()) return;

    setActionLoading(true);
    setError('');
    try {
      await apiClient.post(`/pets/${selectedPet.id}/decline`, {
        reason: declineReason,
      });
      setSuccessMessage(`${selectedPet.name} has been declined`);
      // Remove from pending
      setPendingPets((prev) => prev.filter((p) => p.id !== selectedPet.id));
      setDeclineModalOpen(false);
      setSelectedPet(null);
      setDeclineReason('');
      setTimeout(() => setSuccessMessage(''), 5000);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to decline pet';
      setError(errorMessage);
    } finally {
      setActionLoading(false);
    }
  };

  const pendingApps = applications.filter((a) => a.status === 'SUBMITTED' || a.status === 'UNDER_REVIEW');
  const approvedApps = applications.filter((a) => a.status === 'APPROVED');
  const rejectedApps = applications.filter((a) => a.status === 'REJECTED');

  const openViewApplication = (app: AdoptionApplication) => {
    setSelectedApplication(app);
    setViewApplicationModalOpen(true);
  };

  const handleApproveApplication = async (app: AdoptionApplication) => {
    setActionLoading(true);
    setError('');
    try {
      await apiClient.put(`/applications/${app.id}/approve`);
      setSuccessMessage(`Application for ${app.petName} has been approved`);
      setApplications((prev) =>
        prev.map((a) => (a.id === app.id ? { ...a, status: 'APPROVED' } : a))
      );
      // Update pet status from AVAILABLE to IN_PROGRESS
      setActivePets((prev) =>
        prev.map((p) => (p.id === app.petId ? { ...p, status: 'IN_PROGRESS' } : p))
      );
      setViewApplicationModalOpen(false);
      setTimeout(() => setSuccessMessage(''), 5000);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to approve application';
      setError(errorMessage);
    } finally {
      setActionLoading(false);
    }
  };

  const openRejectApplication = (app: AdoptionApplication) => {
    setSelectedApplication(app);
    setRejectReason('');
    setRejectApplicationModalOpen(true);
    setViewApplicationModalOpen(false);
  };

  const handleRejectApplication = async () => {
    if (!selectedApplication || !rejectReason.trim()) return;

    setActionLoading(true);
    setError('');
    try {
      await apiClient.put(`/applications/${selectedApplication.id}/reject`, {
        reason: rejectReason,
      });
      setSuccessMessage(`Application for ${selectedApplication.petName} has been rejected`);
      setApplications((prev) =>
        prev.map((a) => (a.id === selectedApplication.id ? { ...a, status: 'REJECTED', rejectionReason: rejectReason, reviewedAt: new Date().toISOString() } : a))
      );
      setRejectApplicationModalOpen(false);
      setSelectedApplication(null);
      setRejectReason('');
      setTimeout(() => setSuccessMessage(''), 5000);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to reject application';
      setError(errorMessage);
    } finally {
      setActionLoading(false);
    }
  };

  const openFinalizeAdoption = (app: AdoptionApplication) => {
    setSelectedApplication(app);
    setFinalizeModalOpen(true);
  };

  const handleFinalizeAdoption = async () => {
    if (!selectedApplication) return;

    setActionLoading(true);
    setError('');
    try {
      await apiClient.post('/adoptions', {
        applicationId: selectedApplication.id,
      });
      setSuccessMessage(`Congratulations! The adoption of ${selectedApplication.petName} has been finalized!`);
      // Update the pet status in activePets
      setActivePets((prev) =>
        prev.map((p) => (p.id === selectedApplication.petId ? { ...p, status: 'ADOPTED' } : p))
      );
      // Remove from applications or mark as finalized
      setApplications((prev) => prev.filter((a) => a.id !== selectedApplication.id));
      setFinalizeModalOpen(false);
      setSelectedApplication(null);
      setTimeout(() => setSuccessMessage(''), 5000);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to finalize adoption';
      setError(errorMessage);
    } finally {
      setActionLoading(false);
    }
  };

  const openHoldModal = (pet: Pet) => {
    setSelectedPetForHold(pet);
    setHoldReason('');
    setHoldModalOpen(true);
  };

  const handlePutOnHold = async () => {
    if (!selectedPetForHold || !holdReason.trim()) return;

    setActionLoading(true);
    setError('');
    try {
      await apiClient.post(`/pets/${selectedPetForHold.id}/hold`, {
        reason: holdReason,
      });
      setSuccessMessage(`${selectedPetForHold.name} has been put on hold`);
      // Update pet status in activePets
      setActivePets((prev) =>
        prev.map((p) => (p.id === selectedPetForHold.id ? { ...p, status: 'ON_HOLD' } : p))
      );
      setHoldModalOpen(false);
      setSelectedPetForHold(null);
      setHoldReason('');
      setTimeout(() => setSuccessMessage(''), 5000);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to put pet on hold';
      setError(errorMessage);
    } finally {
      setActionLoading(false);
    }
  };

  const handleRemoveHold = async (pet: Pet) => {
    setActionLoading(true);
    setError('');
    try {
      await apiClient.delete(`/pets/${pet.id}/hold`);
      setSuccessMessage(`${pet.name} is now available again`);
      // Update pet status in activePets
      setActivePets((prev) =>
        prev.map((p) => (p.id === pet.id ? { ...p, status: 'AVAILABLE' } : p))
      );
      setTimeout(() => setSuccessMessage(''), 5000);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to remove hold';
      setError(errorMessage);
    } finally {
      setActionLoading(false);
    }
  };

  // Filter active pets by status
  const filteredActivePets = activeStatusFilter === 'ALL'
    ? activePets
    : activePets.filter((pet) => pet.status === activeStatusFilter);

  // Compute counts for each status
  const getStatusCount = (status: StatusFilter): number => {
    if (status === 'ALL') return activePets.length;
    return activePets.filter((pet) => pet.status === status).length;
  };

  return (
    <div className="container-app py-8">
      <div className="flex justify-between items-start mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Rescue Dashboard</h1>
          <p className="text-gray-600">Welcome back, {user?.name}</p>
        </div>
        <div className="flex gap-3">
          <Link to="/rescue/pets/new">
            <Button variant="primary">
              + Add Pet
            </Button>
          </Link>
          <Link to="/rescue/vets">
            <Button variant="outline" className="relative">
              Manage Vets
              {vetRequestCount > 0 && (
                <span className="absolute -top-2 -right-2 bg-warning-500 text-white text-xs font-bold rounded-full w-5 h-5 flex items-center justify-center">
                  {vetRequestCount}
                </span>
              )}
            </Button>
          </Link>
        </div>
      </div>

      {/* Success Message */}
      {successMessage && (
        <div className="bg-success-50 border border-success-200 text-success-700 px-4 py-3 rounded mb-8">
          {successMessage}
        </div>
      )}

      {/* Error Message */}
      {error && (
        <div className="bg-error-50 border border-error-200 text-error-700 px-4 py-3 rounded mb-8">
          {error}
        </div>
      )}

      {/* Action Required Banner - Only show if there are actionable items */}
      {(pendingPets.length > 0 || pendingApps.length > 0 || approvedApps.length > 0 || vetRequestCount > 0) && (
        <div className="card p-4 mb-8 border-2 border-warning-200 bg-warning-50">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-8 h-8 bg-warning-500 rounded-full flex items-center justify-center">
              <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            <h2 className="text-lg font-semibold text-warning-800">Action Required</h2>
            <span className="px-2 py-0.5 text-xs font-bold bg-warning-500 text-white rounded-full">
              {pendingPets.length + pendingApps.length + approvedApps.length + (vetRequestCount > 0 ? 1 : 0)}
            </span>
          </div>
          <div className="flex flex-wrap gap-4 text-sm">
            {pendingPets.length > 0 && (
              <span className="text-warning-700">{pendingPets.length} pet{pendingPets.length !== 1 ? 's' : ''} awaiting review</span>
            )}
            {pendingApps.length > 0 && (
              <span className="text-warning-700">{pendingApps.length} application{pendingApps.length !== 1 ? 's' : ''} to review</span>
            )}
            {approvedApps.length > 0 && (
              <span className="text-warning-700">{approvedApps.length} adoption{approvedApps.length !== 1 ? 's' : ''} to finalize</span>
            )}
            {vetRequestCount > 0 && (
              <Link to="/rescue/vets" className="text-warning-700 underline">{vetRequestCount} vet request{vetRequestCount !== 1 ? 's' : ''}</Link>
            )}
          </div>
        </div>
      )}

      {/* Stats - Consolidated */}
      <div className="flex flex-wrap gap-4 mb-8">
        <div className="card px-5 py-3 flex items-center gap-3">
          <span className="text-2xl font-bold text-success-500">{activePets.filter((p) => p.status === 'AVAILABLE').length}</span>
          <span className="text-sm text-gray-600">Available</span>
        </div>
        <div className="card px-5 py-3 flex items-center gap-3">
          <span className="text-2xl font-bold text-blue-500">{activePets.filter((p) => p.status === 'PENDING_VET').length}</span>
          <span className="text-sm text-gray-600">Pending Vet</span>
        </div>
        <div className="card px-5 py-3 flex items-center gap-3">
          <span className="text-2xl font-bold text-purple-500">{activePets.filter((p) => p.status === 'IN_PROGRESS').length}</span>
          <span className="text-sm text-gray-600">In Progress</span>
        </div>
        <div className="card px-5 py-3 flex items-center gap-3">
          <span className="text-2xl font-bold text-orange-500">{activePets.filter((p) => p.status === 'ON_HOLD').length}</span>
          <span className="text-sm text-gray-600">On Hold</span>
        </div>
        <div className="card px-5 py-3 flex items-center gap-3">
          <span className="text-2xl font-bold text-primary-500">{activePets.filter((p) => p.status === 'ADOPTED').length}</span>
          <span className="text-sm text-gray-600">Adopted</span>
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent" />
        </div>
      ) : (
        <div className="space-y-8">
          {/* Pending Pets for Review */}
          <section>
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-semibold text-gray-900">Pending Review</h2>
              {pendingPets.length > 0 && (
                <span className="text-sm text-gray-500">{pendingPets.length} pets awaiting review</span>
              )}
            </div>
            {pendingPets.length > 0 ? (
              <div className="card divide-y divide-secondary-200">
                {pendingPets.map((pet) => (
                  <div key={pet.id}>
                    <div className="p-4 flex items-center justify-between">
                      <div className="flex items-center gap-4 flex-1">
                        <img
                          src={pet.imageUrls[0] || `https://placedog.net/80/80?id=${pet.id.slice(0, 8)}`}
                          alt={pet.name}
                          className="w-16 h-16 rounded-lg object-cover"
                        />
                        <div className="flex-1">
                          <Link to={`/pets/${pet.id}`} className="font-medium text-gray-900 hover:text-primary-500">
                            {pet.name}
                          </Link>
                          <p className="text-sm text-gray-500">{formatBreed(pet.breed) || pet.species}</p>
                          <p className="text-xs text-gray-400">Microchip: {pet.microchipId}</p>
                        </div>
                        <button
                          onClick={() => togglePetDetails(pet.id)}
                          className="text-sm text-primary-500 hover:text-primary-600 flex items-center gap-1"
                        >
                          {expandedPetId === pet.id ? 'Hide' : 'View'} Details
                          <svg
                            className={`w-4 h-4 transition-transform ${expandedPetId === pet.id ? 'rotate-180' : ''}`}
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                          >
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                          </svg>
                        </button>
                      </div>
                      <div className="flex gap-2 ml-4">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => openDeclineModal(pet)}
                          disabled={actionLoading}
                        >
                          Decline
                        </Button>
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => handleAcceptPet(pet)}
                          loading={actionLoading}
                        >
                          Accept
                        </Button>
                      </div>
                    </div>
                    {/* Expandable Details */}
                    {expandedPetId === pet.id && (
                      <div className="px-4 pb-4 pt-0 bg-gray-50 border-t border-gray-100">
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 py-3">
                          <div>
                            <p className="text-xs text-gray-500">Age</p>
                            <p className="text-sm font-medium">{pet.age} {pet.ageUnit.toLowerCase()}</p>
                          </div>
                          <div>
                            <p className="text-xs text-gray-500">Sex</p>
                            <p className="text-sm font-medium capitalize">{pet.sex.toLowerCase()}</p>
                          </div>
                          <div>
                            <p className="text-xs text-gray-500">Size</p>
                            <p className="text-sm font-medium capitalize">{pet.size.toLowerCase()}</p>
                          </div>
                          <div>
                            <p className="text-xs text-gray-500">Species</p>
                            <p className="text-sm font-medium capitalize">{pet.species.toLowerCase()}</p>
                          </div>
                        </div>
                        {pet.description && (
                          <div className="py-2 border-t border-gray-200">
                            <p className="text-xs text-gray-500 mb-1">Description</p>
                            <p className="text-sm text-gray-700">{pet.description}</p>
                          </div>
                        )}
                        {pet.healthNotes && (
                          <div className="py-2 border-t border-gray-200">
                            <p className="text-xs text-gray-500 mb-1">Health Notes</p>
                            <p className="text-sm text-gray-700">{pet.healthNotes}</p>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div className="card p-8 text-center text-gray-500">
                No pets awaiting review
              </div>
            )}
          </section>

          {/* Pending Applications */}
          <section>
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-semibold text-gray-900">Adoption Applications</h2>
              {pendingApps.length > 0 && (
                <span className="text-sm text-gray-500">{pendingApps.length} pending applications</span>
              )}
            </div>
            {pendingApps.length > 0 ? (
              <div className="card divide-y divide-secondary-200">
                {pendingApps.map((app) => (
                  <div key={app.id} className="p-4 flex items-center justify-between">
                    <div className="flex items-center gap-4">
                      <div>
                        <p className="font-medium text-gray-900">
                          {app.adopterName} for{' '}
                          <Link to={`/pets/${app.petId}`} className="text-primary-500 hover:underline">
                            {app.petName}
                          </Link>
                        </p>
                        <p className="text-sm text-gray-500">
                          Submitted {formatRelativeTime(app.submittedAt)}
                        </p>
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <Button variant="outline" size="sm" onClick={() => openViewApplication(app)}>
                        View
                      </Button>
                      <Button variant="outline" size="sm" onClick={() => openRejectApplication(app)}>
                        Reject
                      </Button>
                      <Button variant="primary" size="sm" onClick={() => handleApproveApplication(app)} loading={actionLoading}>
                        Approve
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="card p-8 text-center text-gray-500">
                No pending applications
              </div>
            )}
          </section>

          {/* Approved Applications - Ready to Finalize */}
          {approvedApps.length > 0 && (
            <section>
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-semibold text-gray-900">Ready to Finalize</h2>
                <span className="text-sm text-gray-500">{approvedApps.length} approved applications</span>
              </div>
              <div className="card divide-y divide-secondary-200">
                {approvedApps.map((app) => (
                  <div key={app.id} className="p-4 flex items-center justify-between">
                    <div className="flex items-center gap-4">
                      <div>
                        <p className="font-medium text-gray-900">
                          {app.adopterName} adopting{' '}
                          <Link to={`/pets/${app.petId}`} className="text-primary-500 hover:underline">
                            {app.petName}
                          </Link>
                        </p>
                        <p className="text-sm text-success-600">
                          Approved - Ready to finalize adoption
                        </p>
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <Button variant="primary" size="sm" onClick={() => openFinalizeAdoption(app)}>
                        Finalize Adoption
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* Rejected Applications */}
          {rejectedApps.length > 0 && (
            <section>
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-semibold text-gray-900">Rejected Applications</h2>
                <span className="text-sm text-gray-500">{rejectedApps.length} rejected</span>
              </div>
              <div className="card divide-y divide-secondary-200">
                {rejectedApps.map((app) => (
                  <div key={app.id} className="p-4">
                    <div className="flex items-center justify-between mb-2">
                      <div>
                        <p className="font-medium text-gray-900">
                          {app.adopterName} for{' '}
                          <Link to={`/pets/${app.petId}`} className="text-primary-500 hover:underline">
                            {app.petName}
                          </Link>
                        </p>
                        <p className="text-sm text-gray-500">
                          Rejected {app.reviewedAt ? formatRelativeTime(app.reviewedAt) : ''}
                        </p>
                      </div>
                      <span className="px-2 py-1 text-xs font-medium rounded-full bg-error-100 text-error-700">
                        Rejected
                      </span>
                    </div>
                    {app.rejectionReason && (
                      <div className="mt-2 p-2 bg-error-50 border border-error-200 rounded text-sm">
                        <span className="font-medium text-error-700">Reason: </span>
                        <span className="text-error-600">{app.rejectionReason}</span>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* Active Pets */}
          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Active Listings</h2>

            {/* Status Filter Tabs */}
            <div className="flex flex-wrap gap-2 mb-6">
              {statusFilterOptions.map((option) => {
                const count = getStatusCount(option.value);
                const isActive = activeStatusFilter === option.value;
                return (
                  <button
                    key={option.value}
                    onClick={() => setActiveStatusFilter(option.value)}
                    className={`px-4 py-2 rounded-full text-sm font-medium transition-colors ${
                      isActive
                        ? 'bg-primary-500 text-white'
                        : 'bg-secondary-100 text-gray-700 hover:bg-secondary-200'
                    }`}
                  >
                    {option.label}
                    <span
                      className={`ml-2 px-2 py-0.5 rounded-full text-xs ${
                        isActive ? 'bg-primary-600 text-white' : 'bg-secondary-200 text-gray-600'
                      }`}
                    >
                      {count}
                    </span>
                  </button>
                );
              })}
            </div>

            {filteredActivePets.length > 0 ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {filteredActivePets.map((pet) => (
                  <div key={pet.id} className="relative">
                    <PetCard pet={pet} />
                    {/* Hold/Remove Hold Actions */}
                    {pet.status === 'AVAILABLE' && (
                      <div className="mt-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => openHoldModal(pet)}
                          disabled={actionLoading}
                          className="w-full"
                        >
                          Put on Hold
                        </Button>
                      </div>
                    )}
                    {pet.status === 'ON_HOLD' && (
                      <div className="mt-2">
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => handleRemoveHold(pet)}
                          loading={actionLoading}
                          className="w-full"
                        >
                          Remove Hold
                        </Button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div className="card p-8 text-center text-gray-500">
                {activeStatusFilter === 'ALL'
                  ? 'No active pet listings'
                  : `No pets with status "${statusFilterOptions.find((o) => o.value === activeStatusFilter)?.label}"`}
              </div>
            )}
          </section>
        </div>
      )}

      {/* Decline Pet Modal */}
      <Modal
        isOpen={declineModalOpen}
        onClose={() => setDeclineModalOpen(false)}
        title="Decline Pet"
      >
        <div className="space-y-4">
          <p className="text-gray-600">
            Please provide a reason for declining {selectedPet?.name}. This will be sent to the foster.
          </p>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Reason for Decline *
            </label>
            <textarea
              value={declineReason}
              onChange={(e) => setDeclineReason(e.target.value)}
              className="input min-h-24"
              placeholder="Please explain why this pet cannot be accepted..."
              required
            />
          </div>
          <div className="flex gap-4 pt-4">
            <Button
              variant="outline"
              onClick={() => setDeclineModalOpen(false)}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleDeclinePet}
              loading={actionLoading}
              disabled={!declineReason.trim()}
              className="flex-1"
            >
              Decline Pet
            </Button>
          </div>
        </div>
      </Modal>

      {/* View Application Modal */}
      <Modal
        isOpen={viewApplicationModalOpen}
        onClose={() => setViewApplicationModalOpen(false)}
        title={`Application for ${selectedApplication?.petName}`}
      >
        {selectedApplication && (
          <div className="space-y-4">
            <div className="bg-secondary-50 rounded-lg p-4">
              <h4 className="font-medium text-gray-900 mb-2">Applicant Information</h4>
              <p className="text-gray-700">{selectedApplication.adopterName}</p>
              {selectedApplication.adopterPhone && (
                <p className="text-gray-600 text-sm">{selectedApplication.adopterPhone}</p>
              )}
            </div>

            {selectedApplication.whyAdopt && (
              <div>
                <h4 className="font-medium text-gray-900 mb-1">Why they want to adopt</h4>
                <p className="text-gray-600">{selectedApplication.whyAdopt}</p>
              </div>
            )}

            {selectedApplication.livingSituation && (
              <div>
                <h4 className="font-medium text-gray-900 mb-1">Living Situation</h4>
                <p className="text-gray-600">{selectedApplication.livingSituation}</p>
              </div>
            )}

            {selectedApplication.petExperience && (
              <div>
                <h4 className="font-medium text-gray-900 mb-1">Pet Experience</h4>
                <p className="text-gray-600">{selectedApplication.petExperience}</p>
              </div>
            )}

            <div className="text-sm text-gray-500">
              Submitted {formatRelativeTime(selectedApplication.submittedAt)}
            </div>

            <div className="flex gap-4 pt-4">
              <Button
                variant="outline"
                onClick={() => openRejectApplication(selectedApplication)}
                className="flex-1"
              >
                Reject
              </Button>
              <Button
                variant="primary"
                onClick={() => handleApproveApplication(selectedApplication)}
                loading={actionLoading}
                className="flex-1"
              >
                Approve
              </Button>
            </div>
          </div>
        )}
      </Modal>

      {/* Reject Application Modal */}
      <Modal
        isOpen={rejectApplicationModalOpen}
        onClose={() => setRejectApplicationModalOpen(false)}
        title="Reject Application"
      >
        <div className="space-y-4">
          <p className="text-gray-600">
            Please provide a reason for rejecting {selectedApplication?.adopterName}'s application for {selectedApplication?.petName}.
          </p>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Reason for Rejection *
            </label>
            <Textarea
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              placeholder="Please explain why this application is being rejected..."
              rows={4}
            />
          </div>
          <div className="flex gap-4 pt-4">
            <Button
              variant="outline"
              onClick={() => setRejectApplicationModalOpen(false)}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleRejectApplication}
              loading={actionLoading}
              disabled={!rejectReason.trim()}
              className="flex-1"
            >
              Reject Application
            </Button>
          </div>
        </div>
      </Modal>

      {/* Finalize Adoption Modal */}
      <Modal
        isOpen={finalizeModalOpen}
        onClose={() => setFinalizeModalOpen(false)}
        title="Finalize Adoption"
      >
        <div className="space-y-4">
          <div className="bg-success-50 border border-success-200 rounded-lg p-4">
            <p className="text-success-700">
              You're about to finalize the adoption of <strong>{selectedApplication?.petName}</strong> by <strong>{selectedApplication?.adopterName}</strong>.
            </p>
          </div>
          <p className="text-gray-600">
            This action will:
          </p>
          <ul className="list-disc list-inside text-gray-600 space-y-1">
            <li>Mark the pet as Adopted</li>
            <li>Notify the adopter of the finalized adoption</li>
            <li>Complete the adoption process</li>
          </ul>
          <div className="flex gap-4 pt-4">
            <Button
              variant="outline"
              onClick={() => setFinalizeModalOpen(false)}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleFinalizeAdoption}
              loading={actionLoading}
              className="flex-1"
            >
              Finalize Adoption
            </Button>
          </div>
        </div>
      </Modal>

      {/* Put on Hold Modal */}
      <Modal
        isOpen={holdModalOpen}
        onClose={() => setHoldModalOpen(false)}
        title="Put Pet on Hold"
      >
        <div className="space-y-4">
          <p className="text-gray-600">
            Please provide a reason for placing {selectedPetForHold?.name} on hold.
            The pet will remain visible but won't accept new applications.
          </p>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Reason for Hold *
            </label>
            <Textarea
              value={holdReason}
              onChange={(e) => setHoldReason(e.target.value)}
              placeholder="e.g., Medical treatment, behavioral assessment, administrative review..."
              rows={4}
            />
          </div>
          <div className="flex gap-4 pt-4">
            <Button
              variant="outline"
              onClick={() => setHoldModalOpen(false)}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handlePutOnHold}
              loading={actionLoading}
              disabled={!holdReason.trim()}
              className="flex-1"
            >
              Put on Hold
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}

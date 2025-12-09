import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { Button, Input, Modal } from '../../components';
import type { Pet } from '../../types';
import apiClient from '../../api/client';

export function VetDashboard() {
  const { user } = useAuth();
  const [microchipId, setMicrochipId] = useState('');
  const [searching, setSearching] = useState(false);
  const [pet, setPet] = useState<Pet | null>(null);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [signOffModalOpen, setSignOffModalOpen] = useState(false);
  const [declineModalOpen, setDeclineModalOpen] = useState(false);
  const [signOffNotes, setSignOffNotes] = useState('');
  const [declineReason, setDeclineReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [isNeutered, setIsNeutered] = useState(false);
  const [isVaccinated, setIsVaccinated] = useState(false);
  const [isHealthy, setIsHealthy] = useState(false);

  // Decline reason checkboxes
  const [declineNotNeutered, setDeclineNotNeutered] = useState(false);
  const [declineVaccinations, setDeclineVaccinations] = useState(false);
  const [declineHealthConcerns, setDeclineHealthConcerns] = useState(false);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!microchipId.trim()) return;

    setSearching(true);
    setError('');
    setPet(null);

    try {
      const response = await apiClient.get<Pet>(`/vet/pets/lookup?microchip=${microchipId}`);
      setPet(response.data);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'No pet found with that microchip ID';
      setError(errorMessage);
    } finally {
      setSearching(false);
    }
  };

  const resetSignOffForm = () => {
    setIsNeutered(false);
    setIsVaccinated(false);
    setIsHealthy(false);
    setSignOffNotes('');
  };

  const openSignOffModal = () => {
    resetSignOffForm();
    setSignOffModalOpen(true);
  };

  const openDeclineModal = () => {
    setDeclineReason('');
    setDeclineNotNeutered(false);
    setDeclineVaccinations(false);
    setDeclineHealthConcerns(false);
    setDeclineModalOpen(true);
  };

  const buildDeclineReason = () => {
    const reasons: string[] = [];
    if (declineNotNeutered) reasons.push('Not neutered/spayed');
    if (declineVaccinations) reasons.push('Vaccinations incomplete');
    if (declineHealthConcerns) reasons.push('Health concerns requiring treatment');
    if (declineReason.trim()) reasons.push(declineReason.trim());
    return reasons.join('; ');
  };

  const hasDeclineReason = declineNotNeutered || declineVaccinations || declineHealthConcerns || declineReason.trim();

  const handleSignOff = async () => {
    if (!pet || !isNeutered || !isVaccinated || !isHealthy) return;

    setSubmitting(true);
    setError('');
    try {
      await apiClient.post(`/vet/pets/${pet.id}/sign-off`, {
        isNeutered,
        isVaccinated,
        isHealthy,
        healthNotes: signOffNotes || null,
      });
      setSignOffModalOpen(false);
      setPet({ ...pet, status: 'AVAILABLE' });
      setSuccessMessage(`${pet.name} has been verified and is now available for adoption`);
      resetSignOffForm();
      setTimeout(() => setSuccessMessage(''), 5000);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to sign off on pet';
      setError(errorMessage);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDecline = async () => {
    if (!pet || !hasDeclineReason) return;

    setSubmitting(true);
    setError('');
    try {
      await apiClient.post(`/vet/pets/${pet.id}/decline`, {
        reason: buildDeclineReason(),
      });
      setDeclineModalOpen(false);
      setSuccessMessage(`${pet.name} has been declined and returned to rescue org for review`);
      setPet(null);
      setMicrochipId('');
      setDeclineReason('');
      setDeclineNotNeutered(false);
      setDeclineVaccinations(false);
      setDeclineHealthConcerns(false);
      setTimeout(() => setSuccessMessage(''), 5000);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to decline pet';
      setError(errorMessage);
    } finally {
      setSubmitting(false);
    }
  };

  const canApprove = isNeutered && isVaccinated && isHealthy;

  const canSignOff = pet?.status === 'PENDING_VET';

  return (
    <div className="container-app py-8">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Veterinarian Dashboard</h1>
          <p className="text-gray-600">Welcome back, {user?.name}</p>
        </div>
        <Link to="/vet/history">
          <Button variant="outline">View Sign-Off History</Button>
        </Link>
      </div>

      {/* Microchip Lookup */}
      <div className="card p-6 mb-8">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">Pet Lookup</h2>
        <p className="text-gray-600 mb-6">
          Enter a pet's microchip ID to look up their information and provide a health sign-off.
        </p>
        <form onSubmit={handleSearch} className="flex gap-4">
          <div className="flex-1">
            <Input
              placeholder="Enter microchip ID (e.g., MC123456)"
              value={microchipId}
              onChange={(e) => setMicrochipId(e.target.value)}
            />
          </div>
          <Button type="submit" variant="primary" loading={searching}>
            Search
          </Button>
        </form>
      </div>

      {/* Success Message */}
      {successMessage && (
        <div className="bg-success-50 border border-success-200 text-success-700 px-4 py-3 rounded mb-8">
          {successMessage}
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="bg-error-50 border border-error-200 text-error-700 px-4 py-3 rounded mb-8">
          {error}
        </div>
      )}

      {/* Pet Result */}
      {pet && (
        <div className="card overflow-hidden">
          <div className="flex flex-col md:flex-row">
            <div className="md:w-64 h-48 md:h-auto">
              <img
                src={pet.imageUrls[0] || `https://placedog.net/256/256?id=${pet.id.slice(0, 8)}`}
                alt={pet.name}
                className="w-full h-full object-cover"
              />
            </div>
            <div className="flex-1 p-6">
              <div className="flex justify-between items-start mb-4">
                <div>
                  <h3 className="text-2xl font-bold text-gray-900">{pet.name}</h3>
                  <p className="text-gray-600">{pet.breed || pet.species}</p>
                </div>
                <span className={`status-badge ${pet.status === 'PENDING_VET' ? 'status-pending' : 'status-available'}`}>
                  {pet.status === 'PENDING_VET' ? 'Awaiting Sign-off' : 'Verified'}
                </span>
              </div>

              <div className="grid grid-cols-3 gap-4 mb-6">
                <div>
                  <p className="text-sm text-gray-500">Microchip</p>
                  <p className="font-medium">{pet.microchipId}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Age</p>
                  <p className="font-medium">{pet.age} {pet.ageUnit.toLowerCase()}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Sex</p>
                  <p className="font-medium capitalize">{pet.sex.toLowerCase()}</p>
                </div>
              </div>

              {pet.description && (
                <div className="mb-6">
                  <p className="text-sm text-gray-500 mb-1">Description</p>
                  <p className="text-gray-700">{pet.description}</p>
                </div>
              )}

              {canSignOff && (
                <div className="flex gap-4">
                  <Button variant="outline" onClick={openDeclineModal}>
                    Decline
                  </Button>
                  <Button variant="primary" onClick={openSignOffModal}>
                    Sign Off - Ready for Adoption
                  </Button>
                </div>
              )}

              {pet.status === 'AVAILABLE' && (
                <div className="bg-success-50 text-success-700 p-4 rounded">
                  <p className="font-medium">This pet has been verified and is ready for adoption.</p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Sign Off Modal */}
      <Modal
        isOpen={signOffModalOpen}
        onClose={() => setSignOffModalOpen(false)}
        title="Vet Sign-Off"
      >
        <div className="space-y-4">
          <p className="text-gray-600">
            Please verify that {pet?.name} meets all health requirements for adoption:
          </p>

          <div className="space-y-3">
            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={isNeutered}
                onChange={(e) => setIsNeutered(e.target.checked)}
                className="w-5 h-5 rounded border-gray-300 text-primary-500 focus:ring-primary-500"
              />
              <span className="text-gray-700">Neutered/Spayed</span>
            </label>

            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={isVaccinated}
                onChange={(e) => setIsVaccinated(e.target.checked)}
                className="w-5 h-5 rounded border-gray-300 text-primary-500 focus:ring-primary-500"
              />
              <span className="text-gray-700">Up-to-date on vaccinations</span>
            </label>

            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={isHealthy}
                onChange={(e) => setIsHealthy(e.target.checked)}
                className="w-5 h-5 rounded border-gray-300 text-primary-500 focus:ring-primary-500"
              />
              <span className="text-gray-700">No major health concerns</span>
            </label>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Health Notes (optional)
            </label>
            <textarea
              value={signOffNotes}
              onChange={(e) => setSignOffNotes(e.target.value)}
              className="input min-h-24"
              placeholder="Any additional health notes or observations..."
            />
          </div>

          <div className="flex gap-4 pt-4">
            <Button
              variant="outline"
              onClick={() => setSignOffModalOpen(false)}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleSignOff}
              loading={submitting}
              disabled={!canApprove}
              className="flex-1"
            >
              Approve for Adoption
            </Button>
          </div>
        </div>
      </Modal>

      {/* Decline Modal */}
      <Modal
        isOpen={declineModalOpen}
        onClose={() => setDeclineModalOpen(false)}
        title="Decline Pet"
      >
        <div className="space-y-4">
          <p className="text-gray-600">
            Please select the reasons for declining {pet?.name}. The pet will be returned to the rescue organization for further review.
          </p>

          <div className="space-y-3">
            <p className="text-sm font-medium text-gray-700">Common Decline Reasons</p>
            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={declineNotNeutered}
                onChange={(e) => setDeclineNotNeutered(e.target.checked)}
                className="w-5 h-5 rounded border-gray-300 text-error-600 focus:ring-error-500"
              />
              <span className="text-gray-700">Not neutered/spayed</span>
            </label>

            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={declineVaccinations}
                onChange={(e) => setDeclineVaccinations(e.target.checked)}
                className="w-5 h-5 rounded border-gray-300 text-error-600 focus:ring-error-500"
              />
              <span className="text-gray-700">Vaccinations incomplete</span>
            </label>

            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={declineHealthConcerns}
                onChange={(e) => setDeclineHealthConcerns(e.target.checked)}
                className="w-5 h-5 rounded border-gray-300 text-error-600 focus:ring-error-500"
              />
              <span className="text-gray-700">Health concerns requiring treatment</span>
            </label>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Additional Notes (optional)
            </label>
            <textarea
              value={declineReason}
              onChange={(e) => setDeclineReason(e.target.value)}
              className="input min-h-24"
              placeholder="Provide any additional details about the decline reason..."
            />
          </div>

          {!hasDeclineReason && (
            <p className="text-sm text-error-600">
              Please select at least one reason or provide additional notes.
            </p>
          )}

          <div className="flex gap-4 pt-4">
            <Button
              variant="outline"
              onClick={() => setDeclineModalOpen(false)}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              onClick={handleDecline}
              loading={submitting}
              disabled={!hasDeclineReason}
              className="flex-1"
            >
              Decline Pet
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}

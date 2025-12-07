import { useState } from 'react';
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
  const [signOffModalOpen, setSignOffModalOpen] = useState(false);
  const [signOffNotes, setSignOffNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!microchipId.trim()) return;

    setSearching(true);
    setError('');
    setPet(null);

    try {
      const response = await apiClient.get<Pet>(`/pets/lookup?microchip=${microchipId}`);
      setPet(response.data);
    } catch {
      setError('No pet found with that microchip ID');
      // Demo: Show mock pet
      if (microchipId === 'MC123456') {
        setPet({
          id: '1',
          name: 'Luna',
          species: 'DOG',
          breed: 'Siberian Husky',
          age: 2,
          ageUnit: 'YEARS',
          sex: 'FEMALE',
          size: 'MEDIUM',
          microchipId: 'MC123456',
          description: 'Luna is a friendly and energetic husky.',
          healthNotes: null,
          status: 'PENDING_VET',
          fosterId: 'f1',
          rescueOrgId: 'r1',
          createdAt: new Date().toISOString(),
          imageUrls: [],
        });
        setError('');
      }
    } finally {
      setSearching(false);
    }
  };

  const handleSignOff = async () => {
    if (!pet) return;

    setSubmitting(true);
    try {
      await apiClient.post('/vets/signoff', {
        petId: pet.id,
        notes: signOffNotes,
      });
      setSignOffModalOpen(false);
      setPet({ ...pet, status: 'AVAILABLE' });
      setSignOffNotes('');
    } catch {
      // Handle error
    } finally {
      setSubmitting(false);
    }
  };

  const handleDecline = async () => {
    if (!pet) return;

    setSubmitting(true);
    try {
      await apiClient.post('/vets/decline', {
        petId: pet.id,
        reason: signOffNotes,
      });
      setPet(null);
      setMicrochipId('');
      setSignOffNotes('');
    } catch {
      // Handle error
    } finally {
      setSubmitting(false);
    }
  };

  const canSignOff = pet?.status === 'PENDING_VET';

  return (
    <div className="container-app py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Veterinarian Dashboard</h1>
        <p className="text-gray-600">Welcome back, {user?.name}</p>
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
                  <Button variant="outline" onClick={() => setSignOffModalOpen(true)}>
                    Decline
                  </Button>
                  <Button variant="primary" onClick={() => setSignOffModalOpen(true)}>
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
            Please confirm that {pet?.name} meets all health requirements for adoption:
          </p>
          <ul className="list-disc list-inside text-gray-600 text-sm">
            <li>Neutered/Spayed</li>
            <li>Up-to-date on vaccinations</li>
            <li>No major health concerns</li>
          </ul>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Notes (optional)
            </label>
            <textarea
              value={signOffNotes}
              onChange={(e) => setSignOffNotes(e.target.value)}
              className="input min-h-24"
              placeholder="Any additional health notes..."
            />
          </div>
          <div className="flex gap-4 pt-4">
            <Button variant="outline" onClick={handleDecline} loading={submitting} className="flex-1">
              Decline
            </Button>
            <Button variant="primary" onClick={handleSignOff} loading={submitting} className="flex-1">
              Approve
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}

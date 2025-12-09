import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { Button } from '../components';
import type { Pet, RescueOrganization } from '../types';
import apiClient from '../api/client';

export function PetSubmitPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [pet, setPet] = useState<Pet | null>(null);
  const [rescueOrgs, setRescueOrgs] = useState<RescueOrganization[]>([]);
  const [selectedRescueId, setSelectedRescueId] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  useEffect(() => {
    fetchData();
  }, [id]);

  const fetchData = async () => {
    setLoading(true);
    setError('');
    try {
      const [petResponse, rescuesResponse] = await Promise.all([
        apiClient.get<Pet>(`/pets/${id}`),
        apiClient.get<RescueOrganization[]>('/rescues'),
      ]);
      setPet(petResponse.data);
      setRescueOrgs(rescuesResponse.data);

      // Check if pet is in DRAFT status
      if (petResponse.data.status !== 'DRAFT') {
        setError('This pet cannot be submitted for review. Only draft pets can be submitted.');
      }
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to load data';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedRescueId || !pet) return;

    setSubmitting(true);
    setError('');
    try {
      await apiClient.post(`/pets/${pet.id}/submit`, {
        rescueOrgId: selectedRescueId,
      });
      setSuccessMessage('Pet submitted for review successfully!');
      setTimeout(() => {
        navigate('/foster/dashboard');
      }, 2000);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to submit pet for review';
      setError(errorMessage);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent" />
      </div>
    );
  }

  if (!pet) {
    return (
      <div className="container-app py-12 text-center">
        <p className="text-error-500">Pet not found</p>
        <Link to="/foster/dashboard" className="text-primary-500 hover:underline mt-4 inline-block">
          Back to Dashboard
        </Link>
      </div>
    );
  }

  return (
    <div className="container-app py-8">
      {/* Back link */}
      <Link
        to="/foster/dashboard"
        className="inline-flex items-center text-gray-600 hover:text-primary-500 mb-6"
      >
        <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
        </svg>
        Back to Dashboard
      </Link>

      <div className="max-w-2xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Submit for Review</h1>
        <p className="text-gray-600 mb-8">
          Select a rescue organization to review {pet.name} for adoption.
        </p>

        {/* Success Message */}
        {successMessage && (
          <div className="bg-success-50 border border-success-200 text-success-700 px-4 py-3 rounded-lg mb-6">
            {successMessage}
          </div>
        )}

        {/* Error Message */}
        {error && (
          <div className="bg-error-50 border border-error-200 text-error-700 px-4 py-3 rounded-lg mb-6">
            {error}
          </div>
        )}

        {/* Pet Summary */}
        <div className="card p-6 mb-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Pet Summary</h2>
          <div className="flex items-center gap-4">
            <img
              src={pet.imageUrls[0] || `https://placedog.net/80/80?id=${pet.id.slice(0, 8)}`}
              alt={pet.name}
              className="w-20 h-20 rounded-lg object-cover"
            />
            <div>
              <h3 className="font-semibold text-gray-900">{pet.name}</h3>
              <p className="text-sm text-gray-500">{pet.breed || pet.species}</p>
              <p className="text-sm text-gray-500">
                {pet.age} {pet.ageUnit.toLowerCase()} old - {pet.size.toLowerCase()} - {pet.sex.toLowerCase()}
              </p>
              <p className="text-sm text-gray-500">Microchip: {pet.microchipId}</p>
            </div>
          </div>
        </div>

        {/* Rescue Organization Selection */}
        {pet.status === 'DRAFT' && (
          <form onSubmit={handleSubmit}>
            <div className="card p-6 mb-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">Select Rescue Organization</h2>
              <p className="text-sm text-gray-600 mb-4">
                Choose a rescue organization to review your pet. They will verify the pet's information and, once approved,
                the pet will be sent for vet verification.
              </p>

              {rescueOrgs.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                  <p>No rescue organizations available</p>
                </div>
              ) : (
                <div className="space-y-3">
                  {rescueOrgs.map((org) => (
                    <label
                      key={org.id}
                      className={`flex items-start gap-4 p-4 border rounded-lg cursor-pointer transition-colors ${
                        selectedRescueId === org.id
                          ? 'border-primary-500 bg-primary-50'
                          : 'border-gray-200 hover:border-gray-300'
                      }`}
                    >
                      <input
                        type="radio"
                        name="rescueOrg"
                        value={org.id}
                        checked={selectedRescueId === org.id}
                        onChange={(e) => setSelectedRescueId(e.target.value)}
                        className="mt-1"
                      />
                      <div className="flex-1">
                        <div className="flex items-center gap-2">
                          <h3 className="font-medium text-gray-900">{org.name}</h3>
                          <span className="text-xs bg-success-100 text-success-700 px-2 py-0.5 rounded">
                            Verified
                          </span>
                        </div>
                        {org.location && (
                          <p className="text-sm text-gray-500">
                            {org.location}
                          </p>
                        )}
                        {org.description && (
                          <p className="text-sm text-gray-600 mt-1 line-clamp-2">{org.description}</p>
                        )}
                      </div>
                    </label>
                  ))}
                </div>
              )}
            </div>

            {/* Actions */}
            <div className="flex gap-4">
              <Button
                type="button"
                variant="outline"
                onClick={() => navigate('/foster/dashboard')}
                className="flex-1"
              >
                Cancel
              </Button>
              <Button
                type="submit"
                variant="primary"
                loading={submitting}
                disabled={!selectedRescueId}
                className="flex-1"
              >
                Submit for Review
              </Button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

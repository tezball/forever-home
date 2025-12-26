import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { Button, AdoptionJourney, HelpIcon } from '../components';
import type { Pet, RescueOrganization } from '../types';
import apiClient from '../api/client';

export function PetSubmitPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [pet, setPet] = useState<Pet | null>(null);
  const [rescueOrgs, setRescueOrgs] = useState<RescueOrganization[]>([]);
  const [selectedRescueId, setSelectedRescueId] = useState<string>('');
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  useEffect(() => {
    fetchData();
  }, [id]);

  // Filter rescue organizations based on search term
  const filteredRescueOrgs = rescueOrgs.filter((org) => {
    if (!searchTerm.trim()) return true;
    const search = searchTerm.toLowerCase();
    return (
      org.name.toLowerCase().includes(search) ||
      (org.location && org.location.toLowerCase().includes(search)) ||
      (org.description && org.description.toLowerCase().includes(search))
    );
  });

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

        {/* What Happens Next - Journey Overview */}
        <div className="card p-6 mb-6">
          <div className="flex items-center gap-2 mb-4">
            <h2 className="text-lg font-semibold text-gray-900">What happens next?</h2>
            <HelpIcon title="The Adoption Journey">
              <div className="space-y-3">
                <p>Here's what to expect after you submit your pet for review:</p>
                <ol className="list-decimal pl-4 space-y-2">
                  <li>
                    <strong>Rescue Review (1-3 days):</strong> The rescue organization reviews your pet's information and photos.
                  </li>
                  <li>
                    <strong>Vet Verification:</strong> Once accepted, take your pet to an approved vet for a health check. The vet will verify vaccinations, neutering status, and overall health.
                  </li>
                  <li>
                    <strong>Available for Adoption:</strong> After vet sign-off, your pet becomes visible to potential adopters.
                  </li>
                  <li>
                    <strong>Adoption Applications:</strong> Adopters submit applications which the rescue reviews and approves.
                  </li>
                  <li>
                    <strong>Forever Home:</strong> Coordinate with the rescue and approved adopter for the handoff.
                  </li>
                </ol>
              </div>
            </HelpIcon>
          </div>
          <AdoptionJourney currentStatus="DRAFT" variant="horizontal" />
          <p className="text-sm text-gray-600 mt-4 text-center">
            You're at step 1. Submitting to a rescue moves you to step 2.
          </p>
        </div>

        {/* Rescue Organization Selection */}
        {pet.status === 'DRAFT' && (
          <form onSubmit={handleSubmit}>
            <div className="card p-6 mb-6">
              <div className="flex items-center gap-2 mb-4">
                <h2 className="text-lg font-semibold text-gray-900">Select Rescue Organization</h2>
                <HelpIcon title="How to Choose a Rescue Organization">
                  <div className="space-y-3">
                    <p>Rescue organizations are your partners in finding a forever home for your pet. Here's how to choose:</p>
                    <h4 className="font-semibold text-gray-900">Consider:</h4>
                    <ul className="list-disc pl-4 space-y-1">
                      <li><strong>Location:</strong> Choose a rescue near you for easier coordination during the process.</li>
                      <li><strong>Specialization:</strong> Some rescues focus on specific breeds or pet types.</li>
                      <li><strong>Reputation:</strong> All rescues on Forever Home are verified, but you can research their adoption history.</li>
                    </ul>
                    <h4 className="font-semibold text-gray-900">What rescues do:</h4>
                    <ul className="list-disc pl-4 space-y-1">
                      <li>Review and approve pet listings</li>
                      <li>Coordinate vet verification</li>
                      <li>Screen potential adopters</li>
                      <li>Facilitate the adoption handoff</li>
                    </ul>
                    <div className="bg-blue-50 p-3 rounded-lg mt-2">
                      <p className="text-sm text-blue-800">
                        <strong>Tip:</strong> You can change your rescue organization later if the one you choose doesn't respond within a few days.
                      </p>
                    </div>
                  </div>
                </HelpIcon>
              </div>
              <p className="text-sm text-gray-600 mb-4">
                Choose a rescue organization to review your pet. They will verify the pet's information and, once approved,
                the pet will be sent for vet verification.
              </p>

              <div className="bg-amber-50 border border-amber-200 rounded-lg p-4 mb-4">
                <div className="flex gap-3">
                  <svg className="w-5 h-5 text-amber-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <div>
                    <p className="text-sm text-amber-800 font-medium">Not all rescue centers are registered</p>
                    <p className="text-sm text-amber-700 mt-1">
                      Only rescue organizations that have registered on Forever Home are shown below.
                      If your preferred rescue isn't listed, please ask them to register on our platform.
                    </p>
                  </div>
                </div>
              </div>

              {/* Search input */}
              {rescueOrgs.length > 0 && (
                <div className="relative mb-4">
                  <svg
                    className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                    />
                  </svg>
                  <input
                    type="text"
                    placeholder="Search by name, location, or description..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                  />
                </div>
              )}

              {rescueOrgs.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                  <p>No rescue organizations available</p>
                </div>
              ) : filteredRescueOrgs.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                  <p>No rescue organizations match your search</p>
                  <button
                    type="button"
                    onClick={() => setSearchTerm('')}
                    className="text-primary-500 hover:underline mt-2"
                  >
                    Clear search
                  </button>
                </div>
              ) : (
                <div className="space-y-3">
                  {filteredRescueOrgs.map((org) => (
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

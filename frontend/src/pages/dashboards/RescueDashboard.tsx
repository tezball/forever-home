import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { Button, PetCard } from '../../components';
import type { Pet, AdoptionApplication } from '../../types';
import apiClient from '../../api/client';

export function RescueDashboard() {
  const { user } = useAuth();
  const [pendingPets, setPendingPets] = useState<Pet[]>([]);
  const [activePets, setActivePets] = useState<Pet[]>([]);
  const [applications, setApplications] = useState<AdoptionApplication[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [pendingRes, activeRes, appsRes] = await Promise.all([
        apiClient.get<Pet[]>('/rescues/my/pending'),
        apiClient.get<Pet[]>('/rescues/my/pets'),
        apiClient.get<AdoptionApplication[]>('/rescues/my/applications'),
      ]);
      setPendingPets(pendingRes.data);
      setActivePets(activeRes.data);
      setApplications(appsRes.data);
    } catch {
      // Demo data
      setPendingPets([]);
      setActivePets([]);
      setApplications([]);
    } finally {
      setLoading(false);
    }
  };

  const pendingApps = applications.filter((a) => a.status === 'SUBMITTED' || a.status === 'UNDER_REVIEW');

  return (
    <div className="container-app py-8">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Rescue Dashboard</h1>
          <p className="text-gray-600">Welcome back, {user?.name}</p>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        <div className="card p-4 text-center">
          <p className="text-3xl font-bold text-warning-600">{pendingPets.length}</p>
          <p className="text-sm text-gray-600">Pending Review</p>
        </div>
        <div className="card p-4 text-center">
          <p className="text-3xl font-bold text-success-500">{activePets.filter((p) => p.status === 'AVAILABLE').length}</p>
          <p className="text-sm text-gray-600">Available</p>
        </div>
        <div className="card p-4 text-center">
          <p className="text-3xl font-bold text-primary-500">{pendingApps.length}</p>
          <p className="text-sm text-gray-600">Applications</p>
        </div>
        <div className="card p-4 text-center">
          <p className="text-3xl font-bold text-info-500">{activePets.filter((p) => p.status === 'IN_PROGRESS').length}</p>
          <p className="text-sm text-gray-600">In Progress</p>
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
                  <div key={pet.id} className="p-4 flex items-center justify-between">
                    <div className="flex items-center gap-4">
                      <img
                        src={pet.imageUrls[0] || `https://placedog.net/80/80?id=${pet.id.slice(0, 8)}`}
                        alt={pet.name}
                        className="w-16 h-16 rounded-lg object-cover"
                      />
                      <div>
                        <Link to={`/pets/${pet.id}`} className="font-medium text-gray-900 hover:text-primary-500">
                          {pet.name}
                        </Link>
                        <p className="text-sm text-gray-500">{pet.breed || pet.species}</p>
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <Button variant="outline" size="sm">Decline</Button>
                      <Button variant="primary" size="sm">Accept</Button>
                    </div>
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
                    <div>
                      <p className="font-medium text-gray-900">Application #{app.id.slice(0, 8)}</p>
                      <p className="text-sm text-gray-500">
                        Submitted {new Date(app.submittedAt).toLocaleDateString()}
                      </p>
                    </div>
                    <div className="flex gap-2">
                      <Button variant="outline" size="sm">Reject</Button>
                      <Button variant="primary" size="sm">Approve</Button>
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

          {/* Active Pets */}
          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Active Listings</h2>
            {activePets.length > 0 ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {activePets.map((pet) => (
                  <PetCard key={pet.id} pet={pet} />
                ))}
              </div>
            ) : (
              <div className="card p-8 text-center text-gray-500">
                No active pet listings
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
}

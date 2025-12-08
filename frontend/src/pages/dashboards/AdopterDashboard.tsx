import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { Button, PetCard } from '../../components';
import type { Pet, AdoptionApplication } from '../../types';
import apiClient from '../../api/client';

export function AdopterDashboard() {
  const { user } = useAuth();
  const [favorites, setFavorites] = useState<Pet[]>([]);
  const [applications, setApplications] = useState<AdoptionApplication[]>([]);
  const [loading, setLoading] = useState(true);

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

  const pendingApplications = applications.filter((a) => a.status === 'SUBMITTED' || a.status === 'UNDER_REVIEW');
  const approvedApplications = applications.filter((a) => a.status === 'APPROVED');
  const rejectedApplications = applications.filter((a) => a.status === 'REJECTED');

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'SUBMITTED':
        return <span className="status-badge status-pending">Submitted</span>;
      case 'UNDER_REVIEW':
        return <span className="status-badge status-pending">Under Review</span>;
      case 'APPROVED':
        return <span className="status-badge status-available">Approved</span>;
      case 'REJECTED':
        return <span className="status-badge status-withdrawn">Rejected</span>;
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
                  <Link
                    key={app.id}
                    to={`/pets/${app.petId}`}
                    className="flex items-center justify-between p-4 hover:bg-secondary-50"
                  >
                    <div>
                      <p className="font-medium text-gray-900">Pet Application</p>
                      <p className="text-sm text-gray-500">
                        Submitted {new Date(app.submittedAt).toLocaleDateString()}
                      </p>
                    </div>
                    {getStatusBadge(app.status)}
                  </Link>
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
    </div>
  );
}

import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { Button, PetCard } from '../../components';
import type { Pet } from '../../types';
import apiClient from '../../api/client';

export function FosterDashboard() {
  const { user } = useAuth();
  const [pets, setPets] = useState<Pet[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchMyPets();
  }, []);

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

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        <div className="card p-4 text-center">
          <p className="text-3xl font-bold text-gray-900">{draftPets.length}</p>
          <p className="text-sm text-gray-600">Drafts</p>
        </div>
        <div className="card p-4 text-center">
          <p className="text-3xl font-bold text-warning-600">{pendingPets.length}</p>
          <p className="text-sm text-gray-600">Pending</p>
        </div>
        <div className="card p-4 text-center">
          <p className="text-3xl font-bold text-success-500">{activePets.length}</p>
          <p className="text-sm text-gray-600">Active</p>
        </div>
        <div className="card p-4 text-center">
          <p className="text-3xl font-bold text-primary-500">{completedPets.length}</p>
          <p className="text-sm text-gray-600">Completed</p>
        </div>
      </div>

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
                  <PetCard key={pet.id} pet={pet} showEditButton />
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
                  <PetCard key={pet.id} pet={pet} showEditButton />
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
                  <PetCard key={pet.id} pet={pet} showEditButton />
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
    </div>
  );
}

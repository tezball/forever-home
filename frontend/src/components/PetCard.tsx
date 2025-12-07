import type { Pet, PetStatus } from '../types';
import { Link } from 'react-router-dom';

interface PetCardProps {
  pet: Pet;
}

const statusColors: Record<PetStatus, string> = {
  DRAFT: 'bg-gray-100 text-gray-800',
  PENDING_RESCUE: 'bg-yellow-100 text-yellow-800',
  PENDING_VET: 'bg-blue-100 text-blue-800',
  AVAILABLE: 'bg-green-100 text-green-800',
  IN_PROGRESS: 'bg-purple-100 text-purple-800',
  ADOPTED: 'bg-primary-100 text-primary-800',
  WITHDRAWN: 'bg-red-100 text-red-800',
  ON_HOLD: 'bg-orange-100 text-orange-800',
};

const statusLabels: Record<PetStatus, string> = {
  DRAFT: 'Draft',
  PENDING_RESCUE: 'Pending Review',
  PENDING_VET: 'Pending Vet',
  AVAILABLE: 'Available',
  IN_PROGRESS: 'In Progress',
  ADOPTED: 'Adopted',
  WITHDRAWN: 'Withdrawn',
  ON_HOLD: 'On Hold',
};

export function PetCard({ pet }: PetCardProps) {
  const placeholderImage = `https://placedog.net/400/300?id=${pet.id.slice(0, 8)}`;

  return (
    <Link to={`/pets/${pet.id}`} className="block">
      <div className="card hover:shadow-lg transition-shadow">
        <div className="aspect-w-4 aspect-h-3">
          <img
            src={pet.imageUrls[0] || placeholderImage}
            alt={pet.name}
            className="w-full h-48 object-cover"
          />
        </div>
        <div className="p-4">
          <div className="flex justify-between items-start mb-2">
            <h3 className="text-lg font-semibold text-gray-900">{pet.name}</h3>
            <span className={`status-badge ${statusColors[pet.status]}`}>
              {statusLabels[pet.status]}
            </span>
          </div>
          <p className="text-sm text-gray-500 mb-2">
            {pet.breed || pet.species} • {pet.age} {pet.ageUnit.toLowerCase()} • {pet.sex.toLowerCase()}
          </p>
          {pet.description && (
            <p className="text-sm text-gray-600 line-clamp-2">{pet.description}</p>
          )}
        </div>
      </div>
    </Link>
  );
}

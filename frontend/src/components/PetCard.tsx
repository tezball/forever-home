import type { Pet, PetStatus } from '../types';
import { Link } from 'react-router-dom';

interface PetCardProps {
  pet: Pet;
  showEditButton?: boolean;
  onWithdraw?: (pet: Pet) => void;
}

const statusColors: Record<PetStatus, string> = {
  DRAFT: 'bg-gray-200 text-gray-900',
  PENDING_RESCUE: 'bg-yellow-100 text-yellow-900',
  PENDING_VET: 'bg-blue-100 text-blue-900',
  AVAILABLE: 'bg-green-100 text-green-900',
  IN_PROGRESS: 'bg-purple-100 text-purple-900',
  ADOPTED: 'bg-primary-100 text-primary-900',
  WITHDRAWN: 'bg-red-100 text-red-900',
  ON_HOLD: 'bg-orange-100 text-orange-900',
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

export function PetCard({ pet, showEditButton = false, onWithdraw }: PetCardProps) {
  const placeholderImage = `https://placedog.net/400/300?id=${pet.id.slice(0, 8)}`;
  const canEdit = showEditButton && ['DRAFT', 'PENDING_RESCUE', 'PENDING_VET', 'AVAILABLE'].includes(pet.status);
  const canSubmit = showEditButton && pet.status === 'DRAFT';
  const canWithdraw = onWithdraw && !['ADOPTED', 'WITHDRAWN'].includes(pet.status);

  return (
    <div className="card hover:shadow-lg transition-shadow">
      <Link to={`/pets/${pet.id}`} className="block">
        <div className="aspect-w-4 aspect-h-3 relative">
          <img
            src={pet.imageUrls[0] || placeholderImage}
            alt={pet.name}
            className="w-full h-48 object-cover"
          />
          {pet.imageUrls.length === 0 && (
            <div className="absolute bottom-2 right-2 bg-warning-100 text-warning-700 text-xs px-2 py-1 rounded">
              No photos
            </div>
          )}
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
      </Link>
      {(canEdit || canSubmit || canWithdraw) && (
        <div className="px-4 pb-4 pt-0 flex flex-wrap gap-3">
          {canEdit && (
            <Link
              to={`/foster/pets/${pet.id}/edit`}
              className="inline-flex items-center text-sm text-primary-500 hover:text-primary-600"
              onClick={(e) => e.stopPropagation()}
            >
              <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              Edit
            </Link>
          )}
          {canSubmit && (
            <Link
              to={`/foster/pets/${pet.id}/submit`}
              className="inline-flex items-center text-sm text-success-600 hover:text-success-700"
              onClick={(e) => e.stopPropagation()}
            >
              <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              Submit for Review
            </Link>
          )}
          {canWithdraw && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                e.preventDefault();
                onWithdraw(pet);
              }}
              className="inline-flex items-center text-sm text-error-600 hover:text-error-700"
            >
              <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
              </svg>
              Withdraw
            </button>
          )}
        </div>
      )}
    </div>
  );
}

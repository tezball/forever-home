import { Tooltip } from './Tooltip';
import type { PetStatus } from '../types';

// Status display configuration
const statusConfig: Record<PetStatus, {
  label: string;
  bgColor: string;
  textColor: string;
  tooltip: string;
  nextStep?: string;
}> = {
  DRAFT: {
    label: 'Draft',
    bgColor: 'bg-gray-100',
    textColor: 'text-gray-800',
    tooltip: 'This pet listing is saved as a draft and not yet submitted for review.',
    nextStep: 'Submit to a rescue organization to continue the adoption process.',
  },
  PENDING_RESCUE: {
    label: 'Pending Rescue',
    bgColor: 'bg-yellow-100',
    textColor: 'text-yellow-800',
    tooltip: 'Waiting for the rescue organization to review and approve this pet listing.',
    nextStep: 'The rescue organization will review the pet\'s information and either accept or request changes.',
  },
  PENDING_VET: {
    label: 'Pending Vet',
    bgColor: 'bg-blue-100',
    textColor: 'text-blue-800',
    tooltip: 'This pet has been accepted by the rescue and is awaiting veterinary verification.',
    nextStep: 'Take the pet to a verified vet for health check, vaccinations, and neutering confirmation.',
  },
  AVAILABLE: {
    label: 'Available',
    bgColor: 'bg-green-100',
    textColor: 'text-green-800',
    tooltip: 'This pet is verified and available for adoption. Adopters can now submit applications.',
    nextStep: 'Wait for adopter applications. The rescue will review and approve suitable adopters.',
  },
  IN_PROGRESS: {
    label: 'In Progress',
    bgColor: 'bg-purple-100',
    textColor: 'text-purple-800',
    tooltip: 'An adoption application has been approved. The adoption is being finalized.',
    nextStep: 'Coordinate with the rescue organization to complete the handoff to the adopter.',
  },
  ADOPTED: {
    label: 'Adopted',
    bgColor: 'bg-primary-100',
    textColor: 'text-primary-800',
    tooltip: 'This pet has been successfully adopted and found their forever home!',
  },
  WITHDRAWN: {
    label: 'Withdrawn',
    bgColor: 'bg-red-100',
    textColor: 'text-red-800',
    tooltip: 'This pet has been withdrawn from the adoption process by the foster.',
  },
  ON_HOLD: {
    label: 'On Hold',
    bgColor: 'bg-orange-100',
    textColor: 'text-orange-800',
    tooltip: 'This pet is temporarily on hold and not available for new applications.',
    nextStep: 'The rescue organization will update the status when the pet is available again.',
  },
};

// Application status configuration
const applicationStatusConfig: Record<string, {
  label: string;
  bgColor: string;
  textColor: string;
  tooltip: string;
  nextStep?: string;
}> = {
  SUBMITTED: {
    label: 'Submitted',
    bgColor: 'bg-blue-100',
    textColor: 'text-blue-800',
    tooltip: 'Your application has been submitted and is waiting to be reviewed.',
    nextStep: 'The rescue organization will review your application soon.',
  },
  UNDER_REVIEW: {
    label: 'Under Review',
    bgColor: 'bg-warning-100',
    textColor: 'text-warning-800',
    tooltip: 'The rescue organization is actively reviewing your application.',
    nextStep: 'You may be contacted for additional information or an interview.',
  },
  APPROVED: {
    label: 'Approved',
    bgColor: 'bg-success-100',
    textColor: 'text-success-800',
    tooltip: 'Congratulations! Your application has been approved.',
    nextStep: 'Contact the rescue organization to schedule the pet pickup.',
  },
  FINALIZED: {
    label: 'Adopted!',
    bgColor: 'bg-primary-100',
    textColor: 'text-primary-800',
    tooltip: 'The adoption is complete! You\'ve successfully adopted this pet.',
  },
  REJECTED: {
    label: 'Rejected',
    bgColor: 'bg-error-100',
    textColor: 'text-error-800',
    tooltip: 'Unfortunately, your application was not approved for this pet.',
    nextStep: 'You can apply for other available pets that might be a better match.',
  },
  WITHDRAWN: {
    label: 'Withdrawn',
    bgColor: 'bg-gray-100',
    textColor: 'text-gray-800',
    tooltip: 'You withdrew this application.',
    nextStep: 'You can submit a new application for this pet if still available.',
  },
};

interface StatusBadgeProps {
  status: PetStatus | string;
  type?: 'pet' | 'application';
  showTooltip?: boolean;
  size?: 'sm' | 'md';
}

export function StatusBadge({ status, type = 'pet', showTooltip = true, size = 'sm' }: StatusBadgeProps) {
  const config = type === 'pet'
    ? statusConfig[status as PetStatus]
    : applicationStatusConfig[status];

  if (!config) {
    return (
      <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-gray-100 text-gray-800">
        {status}
      </span>
    );
  }

  const sizeClasses = size === 'sm'
    ? 'px-2 py-0.5 text-xs'
    : 'px-3 py-1 text-sm';

  const badge = (
    <span className={`${sizeClasses} font-semibold rounded-full ${config.bgColor} ${config.textColor} inline-flex items-center gap-1`}>
      {config.label}
      {showTooltip && (
        <svg className="w-3 h-3 opacity-60" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      )}
    </span>
  );

  if (!showTooltip) {
    return badge;
  }

  return (
    <Tooltip
      content={
        <div className="space-y-1">
          <p>{config.tooltip}</p>
          {config.nextStep && (
            <p className="text-gray-300 text-xs">
              <span className="font-medium">Next:</span> {config.nextStep}
            </p>
          )}
        </div>
      }
      position="top"
    >
      {badge}
    </Tooltip>
  );
}

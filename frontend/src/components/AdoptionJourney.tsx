import type { PetStatus } from '../types';

interface AdoptionJourneyProps {
  currentStatus: PetStatus;
  variant?: 'horizontal' | 'vertical';
  showDescriptions?: boolean;
}

const journeySteps = [
  {
    status: 'DRAFT',
    label: 'Draft',
    shortLabel: 'Draft',
    description: 'Create pet listing with photos and details',
    icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
      </svg>
    ),
  },
  {
    status: 'PENDING_RESCUE',
    label: 'Rescue Review',
    shortLabel: 'Review',
    description: 'Rescue organization reviews and accepts',
    icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    ),
  },
  {
    status: 'PENDING_VET',
    label: 'Vet Verification',
    shortLabel: 'Vet',
    description: 'Veterinarian confirms health and vaccinations',
    icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
      </svg>
    ),
  },
  {
    status: 'AVAILABLE',
    label: 'Available',
    shortLabel: 'Available',
    description: 'Listed for adopters to view and apply',
    icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
      </svg>
    ),
  },
  {
    status: 'IN_PROGRESS',
    label: 'Adoption In Progress',
    shortLabel: 'In Progress',
    description: 'Matched with an approved adopter',
    icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
      </svg>
    ),
  },
  {
    status: 'ADOPTED',
    label: 'Forever Home',
    shortLabel: 'Adopted',
    description: 'Successfully found their new family',
    icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
      </svg>
    ),
  },
];

const statusOrder: PetStatus[] = ['DRAFT', 'PENDING_RESCUE', 'PENDING_VET', 'AVAILABLE', 'IN_PROGRESS', 'ADOPTED'];

function getStepState(stepStatus: string, currentStatus: PetStatus): 'completed' | 'current' | 'upcoming' {
  const stepIndex = statusOrder.indexOf(stepStatus as PetStatus);
  const currentIndex = statusOrder.indexOf(currentStatus);

  if (currentStatus === 'WITHDRAWN' || currentStatus === 'ON_HOLD') {
    return 'upcoming';
  }

  if (stepIndex < currentIndex) return 'completed';
  if (stepIndex === currentIndex) return 'current';
  return 'upcoming';
}

export function AdoptionJourney({ currentStatus, variant = 'horizontal', showDescriptions = false }: AdoptionJourneyProps) {
  if (variant === 'vertical') {
    return (
      <div className="space-y-4">
        {journeySteps.map((step, index) => {
          const state = getStepState(step.status, currentStatus);
          const isLast = index === journeySteps.length - 1;

          return (
            <div key={step.status} className="flex gap-4">
              {/* Icon and Line */}
              <div className="flex flex-col items-center">
                <div
                  className={`w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 ${
                    state === 'completed'
                      ? 'bg-success-500 text-white'
                      : state === 'current'
                        ? 'bg-primary-500 text-white ring-4 ring-primary-100'
                        : 'bg-gray-200 text-gray-400'
                  }`}
                >
                  {state === 'completed' ? (
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                    </svg>
                  ) : (
                    step.icon
                  )}
                </div>
                {!isLast && (
                  <div
                    className={`w-0.5 flex-1 my-2 ${
                      state === 'completed' ? 'bg-success-500' : 'bg-gray-200'
                    }`}
                  />
                )}
              </div>

              {/* Content */}
              <div className="flex-1 pb-4">
                <p
                  className={`font-medium ${
                    state === 'current' ? 'text-primary-700' : state === 'completed' ? 'text-gray-900' : 'text-gray-400'
                  }`}
                >
                  {step.label}
                </p>
                {showDescriptions && (
                  <p className={`text-sm mt-1 ${state === 'upcoming' ? 'text-gray-400' : 'text-gray-600'}`}>
                    {step.description}
                  </p>
                )}
              </div>
            </div>
          );
        })}
      </div>
    );
  }

  // Horizontal variant
  return (
    <div className="w-full">
      <div className="flex items-center justify-between">
        {journeySteps.map((step, index) => {
          const state = getStepState(step.status, currentStatus);
          const isLast = index === journeySteps.length - 1;

          return (
            <div key={step.status} className="flex items-center flex-1 last:flex-initial">
              {/* Step */}
              <div className="flex flex-col items-center">
                <div
                  className={`w-8 h-8 sm:w-10 sm:h-10 rounded-full flex items-center justify-center flex-shrink-0 ${
                    state === 'completed'
                      ? 'bg-success-500 text-white'
                      : state === 'current'
                        ? 'bg-primary-500 text-white ring-4 ring-primary-100'
                        : 'bg-gray-200 text-gray-400'
                  }`}
                  title={step.label}
                >
                  {state === 'completed' ? (
                    <svg className="w-4 h-4 sm:w-5 sm:h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                    </svg>
                  ) : (
                    <span className="scale-75 sm:scale-100">{step.icon}</span>
                  )}
                </div>
                <span
                  className={`text-xs mt-1 text-center hidden sm:block ${
                    state === 'current' ? 'text-primary-700 font-medium' : state === 'completed' ? 'text-gray-700' : 'text-gray-400'
                  }`}
                >
                  {step.shortLabel}
                </span>
              </div>

              {/* Connector Line */}
              {!isLast && (
                <div
                  className={`flex-1 h-0.5 mx-2 ${
                    state === 'completed' ? 'bg-success-500' : 'bg-gray-200'
                  }`}
                />
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

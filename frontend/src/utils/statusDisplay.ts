/**
 * Utility functions for displaying pet statuses in a human-readable format.
 */

export const PET_STATUS_DISPLAY: Record<string, string> = {
  DRAFT: 'Draft',
  PENDING_RESCUE: 'Pending Review',
  PENDING_VET: 'Vet Review',
  AVAILABLE: 'Available',
  IN_PROGRESS: 'Adoption In Progress',
  ADOPTED: 'Adopted',
  WITHDRAWN: 'Withdrawn',
  ON_HOLD: 'On Hold',
};

export const STATUS_COLORS: Record<string, string> = {
  DRAFT: 'bg-gray-100 text-gray-800',
  PENDING_RESCUE: 'bg-yellow-100 text-yellow-800',
  PENDING_VET: 'bg-blue-100 text-blue-800',
  AVAILABLE: 'bg-green-100 text-green-800',
  IN_PROGRESS: 'bg-purple-100 text-purple-800',
  ADOPTED: 'bg-teal-100 text-teal-800',
  WITHDRAWN: 'bg-red-100 text-red-800',
  ON_HOLD: 'bg-orange-100 text-orange-800',
};

export const STATUS_DOT_COLORS: Record<string, string> = {
  DRAFT: 'bg-gray-400',
  PENDING_RESCUE: 'bg-yellow-400',
  PENDING_VET: 'bg-blue-400',
  AVAILABLE: 'bg-green-400',
  IN_PROGRESS: 'bg-purple-400',
  ADOPTED: 'bg-teal-400',
  WITHDRAWN: 'bg-red-400',
  ON_HOLD: 'bg-orange-400',
};

/**
 * Convert a pet status enum value to a human-readable string.
 */
export function formatPetStatus(status: string | undefined | null): string {
  if (!status) return 'Unknown';
  return PET_STATUS_DISPLAY[status] || status.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
}

/**
 * Get the CSS class for a status badge.
 */
export function getStatusBadgeClass(status: string | undefined | null): string {
  if (!status) return 'bg-gray-100 text-gray-800';
  return STATUS_COLORS[status] || 'bg-gray-100 text-gray-800';
}

/**
 * Get the CSS class for a status dot indicator.
 */
export function getStatusDotClass(status: string | undefined | null): string {
  if (!status) return 'bg-gray-400';
  return STATUS_DOT_COLORS[status] || 'bg-gray-400';
}

/**
 * Application status display mapping.
 */
export const APPLICATION_STATUS_DISPLAY: Record<string, string> = {
  PENDING: 'Pending Review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  WITHDRAWN: 'Withdrawn',
};

export const APPLICATION_STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-yellow-100 text-yellow-800',
  APPROVED: 'bg-green-100 text-green-800',
  REJECTED: 'bg-red-100 text-red-800',
  WITHDRAWN: 'bg-gray-100 text-gray-800',
};

export function formatApplicationStatus(status: string | undefined | null): string {
  if (!status) return 'Unknown';
  return APPLICATION_STATUS_DISPLAY[status] || status;
}

export function getApplicationStatusClass(status: string | undefined | null): string {
  if (!status) return 'bg-gray-100 text-gray-800';
  return APPLICATION_STATUS_COLORS[status] || 'bg-gray-100 text-gray-800';
}

/**
 * User account status display mapping.
 */
export const ACCOUNT_STATUS_DISPLAY: Record<string, string> = {
  ACTIVE: 'Active',
  PENDING: 'Pending Verification',
  SUSPENDED: 'Suspended',
};

export const ACCOUNT_STATUS_COLORS: Record<string, string> = {
  ACTIVE: 'bg-green-100 text-green-800',
  PENDING: 'bg-yellow-100 text-yellow-800',
  SUSPENDED: 'bg-red-100 text-red-800',
};

export function formatAccountStatus(status: string | undefined | null): string {
  if (!status) return 'Unknown';
  return ACCOUNT_STATUS_DISPLAY[status] || status;
}

export function getAccountStatusClass(status: string | undefined | null): string {
  if (!status) return 'bg-gray-100 text-gray-800';
  return ACCOUNT_STATUS_COLORS[status] || 'bg-gray-100 text-gray-800';
}

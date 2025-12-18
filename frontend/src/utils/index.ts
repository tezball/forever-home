/**
 * Utility functions for the Forever Home application
 */

import { BREEDS } from '../constants/breeds';

/**
 * Format a breed enum value to a human-readable string
 * e.g., 'CAVALIER_KING_CHARLES' -> 'Cavalier King Charles'
 */
export function formatBreed(breed: string | null | undefined): string {
  if (!breed) return '';

  // First check if we have a label in our breeds constant
  const breedOption = BREEDS.find((b) => b.value === breed);
  if (breedOption) {
    return breedOption.label;
  }

  // Fallback: convert UPPER_SNAKE_CASE to Title Case
  return breed
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

/**
 * Format a date to a relative time string
 * e.g., '2 days ago', 'Today', 'Yesterday', '3 weeks ago'
 */
export function formatRelativeTime(date: string | Date): string {
  const now = new Date();
  const then = new Date(date);
  const diffInMs = now.getTime() - then.getTime();
  const diffInSeconds = Math.floor(diffInMs / 1000);
  const diffInMinutes = Math.floor(diffInSeconds / 60);
  const diffInHours = Math.floor(diffInMinutes / 60);
  const diffInDays = Math.floor(diffInHours / 24);
  const diffInWeeks = Math.floor(diffInDays / 7);
  const diffInMonths = Math.floor(diffInDays / 30);

  // Check if same day
  if (diffInDays === 0) {
    if (diffInHours === 0) {
      if (diffInMinutes === 0) {
        return 'Just now';
      }
      return diffInMinutes === 1 ? '1 minute ago' : `${diffInMinutes} minutes ago`;
    }
    return diffInHours === 1 ? '1 hour ago' : `${diffInHours} hours ago`;
  }

  // Check if yesterday
  if (diffInDays === 1) {
    return 'Yesterday';
  }

  // Less than a week
  if (diffInDays < 7) {
    return `${diffInDays} days ago`;
  }

  // Less than a month
  if (diffInWeeks < 4) {
    return diffInWeeks === 1 ? '1 week ago' : `${diffInWeeks} weeks ago`;
  }

  // Less than a year
  if (diffInMonths < 12) {
    return diffInMonths === 1 ? '1 month ago' : `${diffInMonths} months ago`;
  }

  // Fallback to date
  return then.toLocaleDateString();
}

/**
 * Format a date string to a short date format
 */
export function formatShortDate(date: string | Date): string {
  return new Date(date).toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
  });
}

/**
 * Get status badge color classes based on status
 */
export function getStatusColor(status: string): string {
  switch (status) {
    case 'AVAILABLE':
      return 'bg-green-100 text-green-800';
    case 'PENDING_VET':
    case 'PENDING_RESCUE':
    case 'UNDER_REVIEW':
    case 'SUBMITTED':
      return 'bg-blue-100 text-blue-800';
    case 'IN_PROGRESS':
      return 'bg-yellow-100 text-yellow-800';
    case 'ADOPTED':
    case 'APPROVED':
    case 'FINALIZED':
      return 'bg-green-100 text-green-800';
    case 'WITHDRAWN':
    case 'REJECTED':
      return 'bg-gray-100 text-gray-800';
    case 'DRAFT':
      return 'bg-gray-200 text-gray-700';
    case 'ON_HOLD':
      return 'bg-orange-100 text-orange-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
}

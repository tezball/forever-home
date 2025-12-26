import { useState, useEffect } from 'react';
import apiClient from '../api/client';

interface StatusHistoryEntry {
  id: string;
  petId: string;
  fromStatus: string | null;
  toStatus: string;
  changedBy: string | null;
  changedAt: string;
  notes: string | null;
}

interface PetTimelineProps {
  petId: string;
}

const statusLabels: Record<string, string> = {
  DRAFT: 'Draft',
  PENDING_RESCUE: 'Pending Rescue Review',
  PENDING_VET: 'Pending Vet Verification',
  AVAILABLE: 'Available for Adoption',
  IN_PROGRESS: 'Adoption In Progress',
  ADOPTED: 'Adopted',
  WITHDRAWN: 'Withdrawn',
  ON_HOLD: 'On Hold',
};

const statusColors: Record<string, string> = {
  DRAFT: 'bg-gray-400',
  PENDING_RESCUE: 'bg-yellow-500',
  PENDING_VET: 'bg-blue-500',
  AVAILABLE: 'bg-green-500',
  IN_PROGRESS: 'bg-purple-500',
  ADOPTED: 'bg-primary-500',
  WITHDRAWN: 'bg-red-500',
  ON_HOLD: 'bg-orange-500',
};

export function PetTimeline({ petId }: PetTimelineProps) {
  const [history, setHistory] = useState<StatusHistoryEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchHistory();
  }, [petId]);

  const fetchHistory = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await apiClient.get<StatusHistoryEntry[]>(`/pets/${petId}/history`);
      setHistory(response.data);
    } catch {
      setError('Failed to load status history');
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  if (loading) {
    return (
      <div className="flex justify-center py-4">
        <div className="animate-spin rounded-full h-6 w-6 border-2 border-primary-500 border-t-transparent" />
      </div>
    );
  }

  if (error) {
    return <p className="text-sm text-error-500">{error}</p>;
  }

  if (history.length === 0) {
    return <p className="text-sm text-gray-500">No status history available</p>;
  }

  return (
    <div className="flow-root">
      <ul className="-mb-8">
        {history.map((entry, idx) => (
          <li key={entry.id}>
            <div className="relative pb-8">
              {idx !== history.length - 1 && (
                <span
                  className="absolute top-5 left-5 -ml-px h-full w-0.5 bg-gray-200 md:top-4 md:left-4"
                  aria-hidden="true"
                />
              )}
              <div className="relative flex space-x-3 md:space-x-3">
                <div className="flex-shrink-0">
                  <span
                    className={`h-10 w-10 md:h-8 md:w-8 rounded-full flex items-center justify-center ring-4 ring-white ${statusColors[entry.toStatus] || 'bg-gray-400'}`}
                  >
                    <svg
                      className="h-5 w-5 md:h-4 md:w-4 text-white"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M5 13l4 4L19 7"
                      />
                    </svg>
                  </span>
                </div>
                <div className="flex min-w-0 flex-1 flex-col md:flex-row md:justify-between md:space-x-4 pt-1">
                  <div className="flex-1">
                    <p className="text-sm md:text-sm text-gray-900">
                      {entry.fromStatus ? (
                        <>
                          <span className="font-medium">{statusLabels[entry.fromStatus] || entry.fromStatus}</span>
                          <span className="mx-1">→</span>
                        </>
                      ) : null}
                      <span className="font-semibold">{statusLabels[entry.toStatus] || entry.toStatus}</span>
                    </p>
                    {entry.notes && (
                      <p className="mt-1 text-sm text-gray-500">{entry.notes}</p>
                    )}
                    <p className="mt-1 text-xs text-gray-400 md:hidden">
                      {formatDate(entry.changedAt)}
                    </p>
                  </div>
                  <div className="hidden md:block whitespace-nowrap text-right text-sm text-gray-500">
                    {formatDate(entry.changedAt)}
                  </div>
                </div>
              </div>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}

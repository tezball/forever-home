import { Link } from 'react-router-dom';

interface SwipeEmptyStateProps {
  type: 'no-results' | 'all-swiped';
  onReset?: () => void;
  onClearFilters?: () => void;
}

export function SwipeEmptyState({ type, onReset, onClearFilters }: SwipeEmptyStateProps) {
  if (type === 'no-results') {
    return (
      <div className="absolute inset-0 flex flex-col items-center justify-center text-center p-6">
        <div className="text-6xl mb-4">🔍</div>
        <h2 className="text-2xl font-bold text-white mb-2">No pets found</h2>
        <p className="text-white/70 mb-6">
          Try adjusting your filters to see more pets.
        </p>
        {onClearFilters && (
          <button
            type="button"
            onClick={onClearFilters}
            className="px-6 py-3 bg-white text-gray-900 rounded-full font-medium hover:bg-white/90 transition-colors"
          >
            Clear Filters
          </button>
        )}
      </div>
    );
  }

  return (
    <div className="absolute inset-0 flex flex-col items-center justify-center text-center p-6">
      <div className="text-6xl mb-4">🎉</div>
      <h2 className="text-2xl font-bold text-white mb-2">You've seen all pets!</h2>
      <p className="text-white/70 mb-6">
        Check back later for new additions.
      </p>
      <div className="flex flex-col gap-3">
        {onReset && (
          <button
            type="button"
            onClick={onReset}
            className="px-6 py-3 bg-white text-gray-900 rounded-full font-medium hover:bg-white/90 transition-colors"
          >
            Start Over
          </button>
        )}
        <Link
          to="/pets"
          className="px-6 py-3 border border-white/30 text-white rounded-full font-medium hover:bg-white/10 transition-colors"
        >
          Browse All Pets
        </Link>
      </div>
    </div>
  );
}

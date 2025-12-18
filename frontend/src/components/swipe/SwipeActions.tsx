interface SwipeActionsProps {
  onPass: () => void;
  onLike: () => void;
  disabled?: boolean;
}

export function SwipeActions({ onPass, onLike, disabled }: SwipeActionsProps) {
  return (
    <div className="flex items-center justify-center gap-8">
      {/* Pass button */}
      <button
        type="button"
        onClick={onPass}
        disabled={disabled}
        className="w-16 h-16 rounded-full bg-white/10 backdrop-blur border-2 border-red-400 text-red-400 flex items-center justify-center shadow-lg hover:bg-red-400/20 active:scale-95 transition-all disabled:opacity-40 disabled:cursor-not-allowed"
        aria-label="Pass"
      >
        <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>

      {/* Like button */}
      <button
        type="button"
        onClick={onLike}
        disabled={disabled}
        className="w-20 h-20 rounded-full bg-gradient-to-br from-emerald-400 to-emerald-500 text-white flex items-center justify-center shadow-lg shadow-emerald-500/30 hover:shadow-emerald-500/50 active:scale-95 transition-all disabled:opacity-40 disabled:cursor-not-allowed"
        aria-label="Like"
      >
        <svg className="w-10 h-10" fill="currentColor" viewBox="0 0 24 24">
          <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" />
        </svg>
      </button>
    </div>
  );
}

import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';

interface SwipeContainerProps {
  children: ReactNode;
  filters: ReactNode;
  actions: ReactNode;
  onClose: () => void;
}

export function SwipeContainer({ children, filters, actions, onClose }: SwipeContainerProps) {
  return (
    <div
      className="fixed inset-0 z-[100] bg-gradient-to-b from-gray-900 via-gray-800 to-gray-900 flex flex-col"
      style={{ touchAction: 'none' }}
    >
      {/* Header */}
      <div
        className="flex items-center justify-between px-4 py-3"
        style={{ paddingTop: 'max(env(safe-area-inset-top), 12px)' }}
      >
        <button
          type="button"
          onClick={onClose}
          className="w-10 h-10 rounded-full bg-white/10 backdrop-blur flex items-center justify-center text-white hover:bg-white/20 transition-colors"
          aria-label="Close"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        <Link to="/" className="flex items-center gap-2">
          <span className="text-xl">🏠</span>
          <span className="font-serif text-lg font-semibold text-white">Forever Home</span>
        </Link>

        <div className="w-10" /> {/* Spacer for centering */}
      </div>

      {/* Filters */}
      <div className="px-4 pb-3">
        {filters}
      </div>

      {/* Card area - takes remaining space */}
      <div className="flex-1 relative overflow-hidden flex items-center justify-center p-4">
        <div className="relative w-full max-w-sm h-full max-h-[600px]">
          {children}
        </div>
      </div>

      {/* Action buttons */}
      <div
        className="flex items-center justify-center gap-6 py-6"
        style={{ paddingBottom: 'max(env(safe-area-inset-bottom), 24px)' }}
      >
        {actions}
      </div>
    </div>
  );
}

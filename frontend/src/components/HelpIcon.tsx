import { useState, type ReactNode } from 'react';
import { Modal } from './Modal';

interface HelpIconProps {
  title: string;
  children: ReactNode;
  size?: 'sm' | 'md';
}

export function HelpIcon({ title, children, size = 'sm' }: HelpIconProps) {
  const [isOpen, setIsOpen] = useState(false);

  const sizeClasses = size === 'sm' ? 'w-4 h-4' : 'w-5 h-5';

  return (
    <>
      <button
        type="button"
        onClick={() => setIsOpen(true)}
        className="inline-flex items-center justify-center text-gray-400 hover:text-primary-500 transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-1 rounded-full"
        aria-label={`Help: ${title}`}
      >
        <svg className={sizeClasses} fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
      </button>

      <Modal isOpen={isOpen} onClose={() => setIsOpen(false)} title={title} size="md">
        <div className="prose prose-sm max-w-none text-gray-600">
          {children}
        </div>
      </Modal>
    </>
  );
}

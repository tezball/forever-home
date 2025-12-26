import { useState } from 'react';

interface PhotoTipsProps {
  variant?: 'inline' | 'expandable';
}

export function PhotoTips({ variant = 'expandable' }: PhotoTipsProps) {
  const [isExpanded, setIsExpanded] = useState(false);

  const tips = [
    {
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
        </svg>
      ),
      title: 'Good lighting',
      description: 'Natural daylight works best. Avoid harsh shadows or flash.',
    },
    {
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
        </svg>
      ),
      title: 'Show their face',
      description: 'Clear photos of their face help adopters connect emotionally.',
    },
    {
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
      ),
      title: 'Multiple angles',
      description: 'Include full body shots, close-ups, and action photos.',
    },
    {
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.828 14.828a4 4 0 01-5.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      ),
      title: 'Capture personality',
      description: 'Show them playing, relaxing, or doing something they love.',
    },
  ];

  const content = (
    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
      {tips.map((tip, index) => (
        <div key={index} className="flex gap-3 p-3 bg-gray-50 rounded-lg">
          <div className="text-primary-500 flex-shrink-0">{tip.icon}</div>
          <div>
            <p className="font-medium text-gray-900 text-sm">{tip.title}</p>
            <p className="text-gray-600 text-xs mt-0.5">{tip.description}</p>
          </div>
        </div>
      ))}
    </div>
  );

  if (variant === 'inline') {
    return (
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
        <div className="flex items-center gap-2 mb-3">
          <svg className="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span className="font-medium text-blue-800">Photo Tips</span>
          <span className="text-blue-600 text-sm">Pets with 3+ photos get 2x more applications</span>
        </div>
        {content}
      </div>
    );
  }

  return (
    <div className="mb-4">
      <button
        type="button"
        onClick={() => setIsExpanded(!isExpanded)}
        className="flex items-center gap-2 text-sm text-primary-600 hover:text-primary-700 transition-colors"
      >
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <span>Tips for great pet photos</span>
        <svg
          className={`w-4 h-4 transition-transform ${isExpanded ? 'rotate-180' : ''}`}
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>
      {isExpanded && (
        <div className="mt-3 bg-blue-50 border border-blue-200 rounded-lg p-4">
          <p className="text-sm text-blue-700 mb-3 font-medium">
            Pets with 3+ quality photos get 2x more adoption applications!
          </p>
          {content}
        </div>
      )}
    </div>
  );
}

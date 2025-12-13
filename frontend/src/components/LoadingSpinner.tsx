interface LoadingSpinnerProps {
  /** Optional message to display below the spinner */
  message?: string;
  /** Size variant */
  size?: 'sm' | 'md' | 'lg';
  /** Whether to center the spinner in its container */
  centered?: boolean;
  /** Whether to take up full screen height */
  fullScreen?: boolean;
}

const sizeClasses = {
  sm: 'h-6 w-6 border-2',
  md: 'h-10 w-10 border-3',
  lg: 'h-14 w-14 border-4',
};

/**
 * A reusable loading spinner component.
 */
export function LoadingSpinner({
  message = 'Loading...',
  size = 'md',
  centered = true,
  fullScreen = false,
}: LoadingSpinnerProps) {
  const containerClasses = fullScreen
    ? 'min-h-screen flex items-center justify-center'
    : centered
    ? 'flex items-center justify-center p-8'
    : '';

  return (
    <div className={containerClasses}>
      <div className="flex flex-col items-center gap-3">
        <div
          className={`animate-spin rounded-full border-primary-500 border-t-transparent ${sizeClasses[size]}`}
          role="status"
          aria-label="Loading"
        />
        {message && (
          <span className="text-sm text-gray-600">{message}</span>
        )}
      </div>
    </div>
  );
}

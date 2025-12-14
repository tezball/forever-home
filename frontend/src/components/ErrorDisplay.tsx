import { Button } from './Button';

interface ErrorDisplayProps {
  title?: string;
  message: string;
  onRetry?: () => void;
  className?: string;
}

/**
 * Reusable error display component for showing user-friendly error messages.
 * Use this for API errors and other recoverable error states.
 */
export function ErrorDisplay({
  title = 'Something went wrong',
  message,
  onRetry,
  className = ''
}: ErrorDisplayProps) {
  return (
    <div className={`text-center py-8 ${className}`}>
      <div className="w-12 h-12 bg-error-100 rounded-full flex items-center justify-center mx-auto mb-4">
        <svg
          className="w-6 h-6 text-error-600"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
      </div>
      <h3 className="text-lg font-semibold text-gray-900 mb-2">{title}</h3>
      <p className="text-gray-600 mb-4 max-w-md mx-auto">{message}</p>
      {onRetry && (
        <Button variant="outline" onClick={onRetry}>
          Try Again
        </Button>
      )}
    </div>
  );
}

/**
 * Maps common API error codes to user-friendly messages.
 */
export function getErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    // Check for network errors
    if (error.message.includes('Network Error') || error.message.includes('fetch')) {
      return 'Unable to connect to the server. Please check your internet connection and try again.';
    }

    // Check for timeout
    if (error.message.includes('timeout')) {
      return 'The request took too long. Please try again.';
    }
  }

  // Check for axios error response
  if (typeof error === 'object' && error !== null) {
    const axiosError = error as { response?: { status?: number; data?: { message?: string } } };

    if (axiosError.response) {
      const status = axiosError.response.status;
      const serverMessage = axiosError.response.data?.message;

      switch (status) {
        case 400:
          return serverMessage || 'Invalid request. Please check your input and try again.';
        case 401:
          return 'Your session has expired. Please log in again.';
        case 403:
          return 'You do not have permission to perform this action.';
        case 404:
          return serverMessage || 'The requested resource was not found.';
        case 409:
          return serverMessage || 'This action conflicts with the current state.';
        case 422:
          return serverMessage || 'The provided data is invalid.';
        case 429:
          return 'Too many requests. Please wait a moment and try again.';
        case 500:
          return 'An unexpected server error occurred. Please try again later.';
        case 502:
        case 503:
        case 504:
          return 'The service is temporarily unavailable. Please try again later.';
        default:
          return serverMessage || 'An unexpected error occurred. Please try again.';
      }
    }
  }

  return 'An unexpected error occurred. Please try again.';
}

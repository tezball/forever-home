import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export interface BreadcrumbItem {
  label: string;
  href?: string;
}

interface BreadcrumbProps {
  items: BreadcrumbItem[];
  showDashboardLink?: boolean;
}

export function Breadcrumb({ items, showDashboardLink = true }: BreadcrumbProps) {
  const { user } = useAuth();

  const getDashboardLink = () => {
    if (!user) return '/';
    switch (user.role) {
      case 'FOSTER': return '/foster/dashboard';
      case 'ADOPTER': return '/adopter/dashboard';
      case 'VET': return '/vet/dashboard';
      case 'RESCUE_ORG': return '/rescue/dashboard';
      case 'ADMIN': return '/admin/dashboard';
      default: return '/';
    }
  };

  const allItems: BreadcrumbItem[] = showDashboardLink
    ? [{ label: 'Dashboard', href: getDashboardLink() }, ...items]
    : items;

  return (
    <nav className="text-sm text-gray-500 mb-6 flex items-center flex-wrap gap-1" aria-label="Breadcrumb">
      {allItems.map((item, index) => (
        <span key={index} className="flex items-center">
          {index > 0 && (
            <svg className="w-4 h-4 mx-1 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
          )}
          {item.href && index < allItems.length - 1 ? (
            <Link to={item.href} className="hover:text-primary-500 transition-colors">
              {item.label}
            </Link>
          ) : (
            <span className={index === allItems.length - 1 ? 'text-gray-900 font-medium' : ''}>
              {item.label}
            </span>
          )}
        </span>
      ))}
    </nav>
  );
}

interface BackLinkProps {
  to?: string;
  label?: string;
}

export function BackToDashboard({ to, label }: BackLinkProps) {
  const { user } = useAuth();

  const getDashboardLink = () => {
    if (to) return to;
    if (!user) return '/';
    switch (user.role) {
      case 'FOSTER': return '/foster/dashboard';
      case 'ADOPTER': return '/adopter/dashboard';
      case 'VET': return '/vet/dashboard';
      case 'RESCUE_ORG': return '/rescue/dashboard';
      case 'ADMIN': return '/admin/dashboard';
      default: return '/';
    }
  };

  return (
    <Link
      to={getDashboardLink()}
      className="inline-flex items-center text-gray-600 hover:text-primary-500 transition-colors mb-6"
    >
      <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
      </svg>
      {label || 'Back to Dashboard'}
    </Link>
  );
}

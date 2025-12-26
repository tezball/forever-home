import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useState, useEffect, useRef } from 'react';
import { NotificationBell } from './NotificationBell';

export function Header() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [userDropdownOpen, setUserDropdownOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Close dropdown when route changes
  useEffect(() => {
    setUserDropdownOpen(false);
    setMobileMenuOpen(false);
  }, [location.pathname]);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setUserDropdownOpen(false);
      }
    };

    if (userDropdownOpen) {
      // Use 'click' instead of 'mousedown' to avoid race condition with toggle button
      document.addEventListener('click', handleClickOutside);
    }

    return () => {
      document.removeEventListener('click', handleClickOutside);
    };
  }, [userDropdownOpen]);

  const handleLogout = async () => {
    if (loggingOut) return; // Prevent double-clicks
    setLoggingOut(true);
    setUserDropdownOpen(false);
    setMobileMenuOpen(false);
    try {
      await logout();
      navigate('/');
    } finally {
      setLoggingOut(false);
    }
  };

  const getDashboardLink = () => {
    if (!user) return '/';
    switch (user.role) {
      case 'FOSTER': return '/foster/dashboard';
      case 'ADOPTER': return '/adopter/dashboard';
      case 'VET': return '/vet/dashboard';
      case 'RESCUE_ORG': return '/rescue/dashboard';
      case 'ADMIN': return 'http://localhost:8081/admin'; // Admin features on moderation-service
      default: return '/';
    }
  };

  const getDashboardLabel = () => {
    if (!user) return 'Dashboard';
    switch (user.role) {
      case 'FOSTER': return 'My Pets';
      case 'ADOPTER': return 'My Applications';
      case 'VET': return 'Vet Dashboard';
      case 'RESCUE_ORG': return 'Rescue Dashboard';
      case 'ADMIN': return 'Admin';
      default: return 'Dashboard';
    }
  };

  return (
    <header className="bg-secondary-50 border-b border-secondary-200 sticky top-0 z-50">
      <div className="container-app">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2">
            <span className="text-2xl">🏠</span>
            <span className="font-serif text-xl font-semibold text-primary-500">Forever Home</span>
          </Link>

          {/* Desktop Navigation */}
          <nav className="hidden md:flex items-center gap-6">
            <Link to="/pets" className="text-gray-600 hover:text-primary-500 transition-colors">
              Browse Pets
            </Link>
            <Link to="/pets/swipe" className="text-gray-600 hover:text-primary-500 transition-colors">
              Swipe
            </Link>
            <Link to="/rescues" className="text-gray-600 hover:text-primary-500 transition-colors">
              Rescues
            </Link>
            <Link to="/faq" className="text-gray-600 hover:text-primary-500 transition-colors">
              FAQ
            </Link>
            {isAuthenticated ? (
              <>
                {user?.role !== 'ADMIN' && (
                  <Link to={getDashboardLink()} className="text-gray-600 hover:text-primary-500 transition-colors">
                    {getDashboardLabel()}
                  </Link>
                )}
                <NotificationBell />
                <div className="relative" ref={dropdownRef}>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setUserDropdownOpen(!userDropdownOpen);
                    }}
                    className="flex items-center gap-2 text-gray-600 hover:text-primary-500"
                  >
                    <div className="w-8 h-8 bg-primary-100 rounded-full flex items-center justify-center">
                      <span className="text-primary-700 text-sm font-medium">
                        {user?.name?.charAt(0).toUpperCase()}
                      </span>
                    </div>
                  </button>
                  {userDropdownOpen && (
                    <div className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg border border-secondary-200">
                      <div className="px-4 py-3 border-b border-secondary-200">
                        <p className="text-sm font-medium text-gray-900">{user?.name}</p>
                        <p className="text-xs text-gray-500">{user?.email}</p>
                      </div>
                      <Link
                        to="/profile"
                        className="block px-4 py-2 text-sm text-gray-700 hover:bg-secondary-100"
                        onClick={() => setUserDropdownOpen(false)}
                      >
                        Profile
                      </Link>
                      {user?.role === 'RESCUE_ORG' && (
                        <Link
                          to="/rescue/settings"
                          className="block px-4 py-2 text-sm text-gray-700 hover:bg-secondary-100"
                          onClick={() => setUserDropdownOpen(false)}
                        >
                          Organization Settings
                        </Link>
                      )}
                      {user?.role === 'VET' && (
                        <Link
                          to="/vet/settings"
                          className="block px-4 py-2 text-sm text-gray-700 hover:bg-secondary-100"
                          onClick={() => setUserDropdownOpen(false)}
                        >
                          Clinic Settings
                        </Link>
                      )}
                      <Link
                        to="/notifications"
                        className="block px-4 py-2 text-sm text-gray-700 hover:bg-secondary-100"
                        onClick={() => setUserDropdownOpen(false)}
                      >
                        Notifications
                      </Link>
                      <Link
                        to="/settings"
                        className="block px-4 py-2 text-sm text-gray-700 hover:bg-secondary-100"
                        onClick={() => setUserDropdownOpen(false)}
                      >
                        Settings
                      </Link>
                      <button
                        type="button"
                        onClick={handleLogout}
                        disabled={loggingOut}
                        className="block w-full text-left px-4 py-2 text-sm text-error-500 hover:bg-secondary-100 cursor-pointer disabled:opacity-50 disabled:cursor-wait"
                      >
                        {loggingOut ? 'Signing out...' : 'Sign Out'}
                      </button>
                    </div>
                  )}
                </div>
              </>
            ) : (
              <>
                <Link to="/login" className="text-gray-600 hover:text-primary-500 transition-colors">
                  Sign In
                </Link>
                <Link
                  to="/register"
                  className="bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 transition-colors"
                >
                  Get Started
                </Link>
              </>
            )}
          </nav>

          {/* Mobile menu button */}
          <button
            className="md:hidden p-2"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            aria-label="Toggle menu"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              {mobileMenuOpen ? (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              ) : (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              )}
            </svg>
          </button>
        </div>

        {/* Mobile Navigation */}
        {mobileMenuOpen && (
          <div className="md:hidden border-t border-secondary-200 py-4">
            <nav className="flex flex-col gap-2">
              <Link
                to="/pets"
                className="px-4 py-2 text-gray-600 hover:bg-secondary-100 rounded"
                onClick={() => setMobileMenuOpen(false)}
              >
                Browse Pets
              </Link>
              <Link
                to="/pets/swipe"
                className="px-4 py-2 text-gray-600 hover:bg-secondary-100 rounded"
                onClick={() => setMobileMenuOpen(false)}
              >
                Swipe
              </Link>
              <Link
                to="/rescues"
                className="px-4 py-2 text-gray-600 hover:bg-secondary-100 rounded"
                onClick={() => setMobileMenuOpen(false)}
              >
                Rescues
              </Link>
              <Link
                to="/faq"
                className="px-4 py-2 text-gray-600 hover:bg-secondary-100 rounded"
                onClick={() => setMobileMenuOpen(false)}
              >
                FAQ
              </Link>
              {isAuthenticated ? (
                <>
                  {user?.role !== 'ADMIN' && (
                    <Link
                      to={getDashboardLink()}
                      className="px-4 py-2 text-gray-600 hover:bg-secondary-100 rounded"
                      onClick={() => setMobileMenuOpen(false)}
                    >
                      {getDashboardLabel()}
                    </Link>
                  )}
                  <Link
                    to="/profile"
                    className="px-4 py-2 text-gray-600 hover:bg-secondary-100 rounded"
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    Profile
                  </Link>
                  {user?.role === 'RESCUE_ORG' && (
                    <Link
                      to="/rescue/settings"
                      className="px-4 py-2 text-gray-600 hover:bg-secondary-100 rounded"
                      onClick={() => setMobileMenuOpen(false)}
                    >
                      Organization Settings
                    </Link>
                  )}
                  {user?.role === 'VET' && (
                    <Link
                      to="/vet/settings"
                      className="px-4 py-2 text-gray-600 hover:bg-secondary-100 rounded"
                      onClick={() => setMobileMenuOpen(false)}
                    >
                      Clinic Settings
                    </Link>
                  )}
                  <Link
                    to="/settings"
                    className="px-4 py-2 text-gray-600 hover:bg-secondary-100 rounded"
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    Settings
                  </Link>
                  <button
                    type="button"
                    onClick={handleLogout}
                    disabled={loggingOut}
                    className="px-4 py-2 text-left text-error-500 hover:bg-secondary-100 rounded cursor-pointer disabled:opacity-50 disabled:cursor-wait"
                  >
                    {loggingOut ? 'Signing out...' : 'Sign Out'}
                  </button>
                </>
              ) : (
                <>
                  <Link
                    to="/login"
                    className="px-4 py-2 text-gray-600 hover:bg-secondary-100 rounded"
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    Sign In
                  </Link>
                  <Link
                    to="/register"
                    className="mx-4 bg-primary-500 text-white px-4 py-2 rounded text-center"
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    Get Started
                  </Link>
                </>
              )}
            </nav>
          </div>
        )}
      </div>
    </header>
  );
}

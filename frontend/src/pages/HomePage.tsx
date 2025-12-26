import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Button, PetCard } from '../components';
import apiClient from '../api/client';
import type { Pet } from '../types';

interface PlatformStats {
  petsAvailable: number;
  totalAdoptions: number;
  totalRescues: number;
}

export function HomePage() {
  const { isAuthenticated, user } = useAuth();
  const [featuredPets, setFeaturedPets] = useState<Pet[]>([]);
  const [stats, setStats] = useState<PlatformStats | null>(null);
  const [loading, setLoading] = useState(true);

  const getRoleSpecificCTA = () => {
    if (!user) return null;
    switch (user.role) {
      case 'FOSTER':
        return { text: 'List a Pet', link: '/foster/pets/new' };
      case 'ADOPTER':
        return { text: 'My Dashboard', link: '/adopter/dashboard' };
      case 'VET':
        return { text: 'Vet Dashboard', link: '/vet/dashboard' };
      case 'RESCUE_ORG':
        return { text: 'Rescue Dashboard', link: '/rescue/dashboard' };
      case 'ADMIN':
        // Admin dashboard is on the moderation service
        return { text: 'Admin Dashboard', link: `${window.location.protocol}//${window.location.hostname}:8081/admin`, external: true };
      default:
        return null;
    }
  };

  useEffect(() => {
    const fetchHomeData = async () => {
      try {
        const [petsRes, statsRes] = await Promise.allSettled([
          apiClient.get<Pet[]>('/pets/featured'),
          apiClient.get<PlatformStats>('/stats'),
        ]);

        if (petsRes.status === 'fulfilled') {
          setFeaturedPets(petsRes.value.data);
        }

        if (statsRes.status === 'fulfilled') {
          setStats(statsRes.value.data);
        }
      } catch (error) {
        console.error('Failed to fetch home data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchHomeData();
  }, []);

  return (
    <div className="min-h-screen">
      {/* Hero Section */}
      <section className="bg-gradient-to-br from-primary-50 to-secondary-50 py-16 md:py-24">
        <div className="container-app">
          <div className="max-w-3xl mx-auto text-center">
            <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold text-gray-900 mb-6">
              Find Your Forever Friend
            </h1>
            <p className="text-xl text-gray-600 mb-8">
              Connect with loving pets waiting for their forever homes. Our trusted network of
              rescue organizations and verified vets ensures every adoption is safe and successful.
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Link to="/pets/swipe">
                <Button variant="primary" size="lg">
                  Swipe Pets
                </Button>
              </Link>
              {isAuthenticated ? (
                getRoleSpecificCTA() && (
                  getRoleSpecificCTA()!.external ? (
                    <a href={getRoleSpecificCTA()!.link}>
                      <Button variant="outline" size="lg">
                        {getRoleSpecificCTA()!.text}
                      </Button>
                    </a>
                  ) : (
                    <Link to={getRoleSpecificCTA()!.link}>
                      <Button variant="outline" size="lg">
                        {getRoleSpecificCTA()!.text}
                      </Button>
                    </Link>
                  )
                )
              ) : (
                <Link to="/register">
                  <Button variant="outline" size="lg">
                    Get Started
                  </Button>
                </Link>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* Platform Stats */}
      {stats && (
        <section className="py-12 bg-white border-b">
          <div className="container-app">
            <div className="grid grid-cols-3 gap-4 md:gap-8 max-w-3xl mx-auto">
              <div className="text-center">
                <div className="w-10 h-10 md:w-12 md:h-12 bg-primary-100 rounded-full flex items-center justify-center mx-auto mb-2">
                  <span className="text-xl md:text-2xl">🐾</span>
                </div>
                <div className="text-3xl md:text-5xl font-bold text-primary-600 mb-1">
                  {stats.petsAvailable}
                </div>
                <div className="text-gray-600 text-xs md:text-base">Pets Available</div>
              </div>
              <div className="text-center">
                <div className="w-10 h-10 md:w-12 md:h-12 bg-success-100 rounded-full flex items-center justify-center mx-auto mb-2">
                  <span className="text-xl md:text-2xl">❤️</span>
                </div>
                <div className="text-3xl md:text-5xl font-bold text-success-600 mb-1">
                  {stats.totalAdoptions}
                </div>
                <div className="text-gray-600 text-xs md:text-base">Happy Adoptions</div>
              </div>
              <div className="text-center">
                <div className="w-10 h-10 md:w-12 md:h-12 bg-secondary-100 rounded-full flex items-center justify-center mx-auto mb-2">
                  <span className="text-xl md:text-2xl">🏥</span>
                </div>
                <div className="text-3xl md:text-5xl font-bold text-secondary-600 mb-1">
                  {stats.totalRescues}
                </div>
                <div className="text-gray-600 text-xs md:text-base">Rescue Partners</div>
              </div>
            </div>
          </div>
        </section>
      )}

      {/* Featured Pets */}
      {featuredPets.length > 0 && (
        <section className="py-16">
          <div className="container-app">
            <div className="flex justify-between items-center mb-8">
              <h2 className="text-3xl font-bold text-gray-900">
                Featured Pets
              </h2>
              <Link to="/pets" className="text-primary-500 font-medium hover:underline">
                View All Pets →
              </Link>
            </div>
            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
              {featuredPets.slice(0, 6).map((pet) => (
                <PetCard key={pet.id} pet={pet} />
              ))}
            </div>
            {loading && (
              <div className="flex justify-center py-12">
                <div className="animate-spin rounded-full h-8 w-8 border-4 border-primary-500 border-t-transparent" />
              </div>
            )}
          </div>
        </section>
      )}

      {/* Show loading state if no pets yet */}
      {loading && featuredPets.length === 0 && (
        <section className="py-16">
          <div className="container-app">
            <h2 className="text-3xl font-bold text-gray-900 mb-8">Featured Pets</h2>
            <div className="flex justify-center py-12">
              <div className="animate-spin rounded-full h-8 w-8 border-4 border-primary-500 border-t-transparent" />
            </div>
          </div>
        </section>
      )}

      {/* How It Works - Only show for unauthenticated users */}
      {!isAuthenticated && (
        <section className="bg-secondary-50 py-16">
          <div className="container-app">
            <h2 className="text-3xl font-bold text-center text-gray-900 mb-12">
              How Forever Home Works
            </h2>
            <div className="grid md:grid-cols-3 gap-8">
              <div className="text-center">
                <div className="w-16 h-16 bg-primary-100 rounded-full flex items-center justify-center mx-auto mb-4">
                  <span className="text-3xl">🐾</span>
                </div>
                <h3 className="text-xl font-semibold text-gray-900 mb-2">Swipe Pets</h3>
                <p className="text-gray-600">
                  Swipe through adorable pets from verified rescue organizations.
                </p>
              </div>
              <div className="text-center">
                <div className="w-16 h-16 bg-primary-100 rounded-full flex items-center justify-center mx-auto mb-4">
                  <span className="text-3xl">📋</span>
                </div>
                <h3 className="text-xl font-semibold text-gray-900 mb-2">Apply to Adopt</h3>
                <p className="text-gray-600">
                  Submit your application and our rescue partners will review your request.
                </p>
              </div>
              <div className="text-center">
                <div className="w-16 h-16 bg-primary-100 rounded-full flex items-center justify-center mx-auto mb-4">
                  <span className="text-3xl">🏠</span>
                </div>
                <h3 className="text-xl font-semibold text-gray-900 mb-2">Welcome Home</h3>
                <p className="text-gray-600">
                  Once approved, bring your new family member to their forever home.
                </p>
              </div>
            </div>
          </div>
        </section>
      )}

      {/* For Different Users - Only show for unauthenticated users */}
      {!isAuthenticated && (
        <section className="py-16">
          <div className="container-app">
            <h2 className="text-3xl font-bold text-center text-gray-900 mb-12">
              Join Our Community
            </h2>
            <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
              <div className="card p-6">
                <span className="text-3xl mb-4 block">👤</span>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">Adopters</h3>
                <p className="text-gray-600 text-sm mb-4">
                  Looking to add a furry friend to your family? Browse verified pets ready for adoption.
                </p>
                <Link to="/register" className="text-primary-500 text-sm font-medium hover:underline">
                  Start Adopting →
                </Link>
              </div>
              <div className="card p-6">
                <span className="text-3xl mb-4 block">💚</span>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">Fosters</h3>
                <p className="text-gray-600 text-sm mb-4">
                  Have a pet that needs a new home? Connect with rescues to find loving families.
                </p>
                <Link to="/register" className="text-primary-500 text-sm font-medium hover:underline">
                  Register a Pet →
                </Link>
              </div>
              <div className="card p-6">
                <span className="text-3xl mb-4 block">🏥</span>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">Rescues</h3>
                <p className="text-gray-600 text-sm mb-4">
                  Manage adoptions, review applications, and grow your rescue organization.
                </p>
                <Link to="/register" className="text-primary-500 text-sm font-medium hover:underline">
                  Partner With Us →
                </Link>
              </div>
              <div className="card p-6">
                <span className="text-3xl mb-4 block">⚕️</span>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">Veterinarians</h3>
                <p className="text-gray-600 text-sm mb-4">
                  Verify pet health and help ensure safe adoptions in your community.
                </p>
                <Link to="/register" className="text-primary-500 text-sm font-medium hover:underline">
                  Join as Vet →
                </Link>
              </div>
            </div>
          </div>
        </section>
      )}

      {/* Footer - Simplified for authenticated users */}
      <footer className="bg-primary-700 text-white py-12">
        <div className="container-app">
          {isAuthenticated ? (
            /* Compact footer for logged-in users */
            <div className="flex flex-col md:flex-row justify-between items-center gap-4">
              <div className="flex items-center gap-2">
                <span className="text-2xl">🏠</span>
                <span className="font-serif text-xl font-semibold">Forever Home</span>
              </div>
              <div className="flex flex-wrap justify-center gap-6 text-sm text-primary-100">
                <Link to="/help" className="hover:text-white">Help</Link>
                <Link to="/contact" className="hover:text-white">Contact</Link>
                <Link to="/privacy" className="hover:text-white">Privacy</Link>
                <Link to="/faq" className="hover:text-white">FAQ</Link>
              </div>
              <div className="text-sm text-primary-200">
                &copy; {new Date().getFullYear()} Forever Home
              </div>
            </div>
          ) : (
            /* Full footer for guests */
            <>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
                <div className="col-span-2 md:col-span-1">
                  <h4 className="font-serif text-xl font-semibold mb-4">Forever Home</h4>
                  <p className="text-primary-100 text-sm">
                    Connecting pets with loving families since 2024.
                  </p>
                </div>
                <div>
                  <h5 className="font-semibold mb-4">For Adopters</h5>
                  <ul className="space-y-1 text-sm text-primary-100">
                    <li><Link to="/pets" className="hover:text-white block py-2">Swipe Pets</Link></li>
                    <li><Link to="/rescues" className="hover:text-white block py-2">Find Rescues</Link></li>
                    <li><Link to="/register" className="hover:text-white block py-2">Create Account</Link></li>
                  </ul>
                </div>
                <div className="hidden md:block">
                  <h5 className="font-semibold mb-4">For Partners</h5>
                  <ul className="space-y-1 text-sm text-primary-100">
                    <li><Link to="/register" className="hover:text-white block py-2">Foster Registration</Link></li>
                    <li><Link to="/register" className="hover:text-white block py-2">Rescue Partnership</Link></li>
                    <li><Link to="/register" className="hover:text-white block py-2">Vet Verification</Link></li>
                  </ul>
                </div>
                <div>
                  <h5 className="font-semibold mb-4">Support</h5>
                  <ul className="space-y-1 text-sm text-primary-100">
                    <li><Link to="/help" className="hover:text-white block py-2">Help Center</Link></li>
                    <li><Link to="/contact" className="hover:text-white block py-2">Contact Us</Link></li>
                    <li><Link to="/privacy" className="hover:text-white block py-2">Privacy Policy</Link></li>
                  </ul>
                </div>
              </div>
              <div className="border-t border-primary-600 mt-8 pt-8 text-center text-sm text-primary-200">
                &copy; {new Date().getFullYear()} Forever Home. All rights reserved.
              </div>
            </>
          )}
        </div>
      </footer>
    </div>
  );
}

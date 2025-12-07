import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Button } from '../components';

export function HomePage() {
  const { isAuthenticated } = useAuth();

  return (
    <div className="min-h-screen">
      {/* Hero Section */}
      <section className="container-app py-16 md:py-24">
        <div className="max-w-3xl mx-auto text-center">
          <h1 className="text-4xl md:text-5xl font-bold text-gray-900 mb-6">
            Find Your Forever Friend
          </h1>
          <p className="text-xl text-gray-600 mb-8">
            Connect with loving pets waiting for their forever homes. Our trusted network of
            rescue organizations and verified vets ensures every adoption is safe and successful.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link to="/pets">
              <Button variant="primary" size="lg">
                Browse Pets
              </Button>
            </Link>
            {!isAuthenticated && (
              <Link to="/register">
                <Button variant="outline" size="lg">
                  Get Started
                </Button>
              </Link>
            )}
          </div>
        </div>
      </section>

      {/* How It Works */}
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
              <h3 className="text-xl font-semibold text-gray-900 mb-2">Browse Pets</h3>
              <p className="text-gray-600">
                Explore our collection of adorable pets from verified rescue organizations.
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

      {/* For Different Users */}
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

      {/* Footer */}
      <footer className="bg-primary-700 text-white py-12">
        <div className="container-app">
          <div className="grid md:grid-cols-4 gap-8">
            <div>
              <h4 className="font-serif text-xl font-semibold mb-4">Forever Home</h4>
              <p className="text-primary-100 text-sm">
                Connecting pets with loving families since 2024.
              </p>
            </div>
            <div>
              <h5 className="font-semibold mb-4">For Adopters</h5>
              <ul className="space-y-2 text-sm text-primary-100">
                <li><Link to="/pets" className="hover:text-white">Browse Pets</Link></li>
                <li><Link to="/rescues" className="hover:text-white">Find Rescues</Link></li>
                <li><Link to="/register" className="hover:text-white">Create Account</Link></li>
              </ul>
            </div>
            <div>
              <h5 className="font-semibold mb-4">For Partners</h5>
              <ul className="space-y-2 text-sm text-primary-100">
                <li><Link to="/register" className="hover:text-white">Foster Registration</Link></li>
                <li><Link to="/register" className="hover:text-white">Rescue Partnership</Link></li>
                <li><Link to="/register" className="hover:text-white">Vet Verification</Link></li>
              </ul>
            </div>
            <div>
              <h5 className="font-semibold mb-4">Support</h5>
              <ul className="space-y-2 text-sm text-primary-100">
                <li><a href="#" className="hover:text-white">Help Center</a></li>
                <li><a href="#" className="hover:text-white">Contact Us</a></li>
                <li><a href="#" className="hover:text-white">Privacy Policy</a></li>
              </ul>
            </div>
          </div>
          <div className="border-t border-primary-600 mt-8 pt-8 text-center text-sm text-primary-200">
            &copy; 2024 Forever Home. All rights reserved.
          </div>
        </div>
      </footer>
    </div>
  );
}

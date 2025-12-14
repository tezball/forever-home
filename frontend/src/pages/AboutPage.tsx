import { Link } from 'react-router-dom';

export function AboutPage() {
  return (
    <div className="container-app py-8">
      <div className="max-w-3xl mx-auto">
        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">About Forever Home</h1>
          <p className="text-gray-600">Connecting pets with loving families</p>
        </div>

        <div className="space-y-8">
          {/* Mission */}
          <section className="card p-8">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Our Mission</h2>
            <p className="text-gray-600">
              Forever Home is a pet adoption platform that connects pet owners looking to rehome
              their pets with adopters through trusted rescue organizations. We believe every pet
              deserves a loving home, and every family deserves a trusted way to find their perfect companion.
            </p>
          </section>

          {/* How It Works */}
          <section className="card p-8">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">How It Works</h2>
            <div className="space-y-4">
              <div className="flex gap-4">
                <div className="w-8 h-8 bg-primary-100 rounded-full flex items-center justify-center flex-shrink-0">
                  <span className="text-primary-600 font-semibold">1</span>
                </div>
                <div>
                  <h3 className="font-medium text-gray-900">Foster Registration</h3>
                  <p className="text-gray-600 text-sm">
                    Pet owners (fosters) register their pets with detailed profiles and photos.
                  </p>
                </div>
              </div>
              <div className="flex gap-4">
                <div className="w-8 h-8 bg-primary-100 rounded-full flex items-center justify-center flex-shrink-0">
                  <span className="text-primary-600 font-semibold">2</span>
                </div>
                <div>
                  <h3 className="font-medium text-gray-900">Rescue Organization Review</h3>
                  <p className="text-gray-600 text-sm">
                    Verified rescue organizations review and accept pets into their adoption programs.
                  </p>
                </div>
              </div>
              <div className="flex gap-4">
                <div className="w-8 h-8 bg-primary-100 rounded-full flex items-center justify-center flex-shrink-0">
                  <span className="text-primary-600 font-semibold">3</span>
                </div>
                <div>
                  <h3 className="font-medium text-gray-900">Veterinary Verification</h3>
                  <p className="text-gray-600 text-sm">
                    Licensed veterinarians verify that pets are healthy, vaccinated, and neutered/spayed.
                  </p>
                </div>
              </div>
              <div className="flex gap-4">
                <div className="w-8 h-8 bg-primary-100 rounded-full flex items-center justify-center flex-shrink-0">
                  <span className="text-primary-600 font-semibold">4</span>
                </div>
                <div>
                  <h3 className="font-medium text-gray-900">Adoption Matching</h3>
                  <p className="text-gray-600 text-sm">
                    Adopters browse available pets, submit applications, and rescue organizations facilitate matches.
                  </p>
                </div>
              </div>
            </div>
          </section>

          {/* Why Choose Us */}
          <section className="card p-8">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Why Forever Home?</h2>
            <ul className="space-y-3 text-gray-600">
              <li className="flex gap-2">
                <svg className="w-5 h-5 text-success-500 flex-shrink-0 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                </svg>
                <span><strong>Verified Partners:</strong> All rescue organizations are verified by our admin team.</span>
              </li>
              <li className="flex gap-2">
                <svg className="w-5 h-5 text-success-500 flex-shrink-0 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                </svg>
                <span><strong>Health Guaranteed:</strong> Every pet is vet-verified before becoming available for adoption.</span>
              </li>
              <li className="flex gap-2">
                <svg className="w-5 h-5 text-success-500 flex-shrink-0 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                </svg>
                <span><strong>Transparent Process:</strong> Track your adoption application every step of the way.</span>
              </li>
              <li className="flex gap-2">
                <svg className="w-5 h-5 text-success-500 flex-shrink-0 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                </svg>
                <span><strong>Support for Fosters:</strong> We help pet owners find the right families for their beloved pets.</span>
              </li>
            </ul>
          </section>

          {/* Call to Action */}
          <section className="card p-8 text-center bg-primary-50">
            <h2 className="text-xl font-semibold text-gray-900 mb-2">Ready to Find Your Forever Friend?</h2>
            <p className="text-gray-600 mb-6">
              Browse our available pets or register to start your adoption journey.
            </p>
            <div className="flex gap-4 justify-center">
              <Link
                to="/pets"
                className="bg-primary-500 text-white px-6 py-2 rounded hover:bg-primary-600 transition-colors"
              >
                Browse Pets
              </Link>
              <Link
                to="/register"
                className="bg-white text-primary-500 border border-primary-500 px-6 py-2 rounded hover:bg-primary-50 transition-colors"
              >
                Create Account
              </Link>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}

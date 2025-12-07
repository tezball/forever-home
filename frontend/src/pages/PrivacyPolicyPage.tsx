export function PrivacyPolicyPage() {
  return (
    <div className="container-app py-8">
      <div className="max-w-3xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Privacy Policy</h1>
          <p className="text-gray-600">Last updated: {new Date().toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })}</p>
        </div>

        <div className="prose prose-gray max-w-none">
          <div className="card p-8 space-y-6">
            <section>
              <h2 className="text-xl font-semibold text-gray-900 mb-3">1. Information We Collect</h2>
              <p className="text-gray-600">
                We collect information you provide directly to us, such as when you create an account,
                submit an adoption application, or contact us for support. This may include:
              </p>
              <ul className="list-disc list-inside text-gray-600 mt-2 space-y-1">
                <li>Name and email address</li>
                <li>Phone number and physical address</li>
                <li>Information about your household and living situation</li>
                <li>Pet preferences and adoption history</li>
              </ul>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-gray-900 mb-3">2. How We Use Your Information</h2>
              <p className="text-gray-600">
                We use the information we collect to:
              </p>
              <ul className="list-disc list-inside text-gray-600 mt-2 space-y-1">
                <li>Provide, maintain, and improve our services</li>
                <li>Process adoption applications and facilitate matches</li>
                <li>Send you notifications about your account and applications</li>
                <li>Respond to your comments, questions, and requests</li>
                <li>Communicate with you about products, services, and events</li>
              </ul>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-gray-900 mb-3">3. Information Sharing</h2>
              <p className="text-gray-600">
                We share your information with rescue organizations and fosters to facilitate the adoption process.
                We do not sell your personal information to third parties. We may share information:
              </p>
              <ul className="list-disc list-inside text-gray-600 mt-2 space-y-1">
                <li>With rescue organizations processing your adoption application</li>
                <li>With service providers who assist in our operations</li>
                <li>To comply with legal obligations</li>
                <li>To protect our rights and prevent fraud</li>
              </ul>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-gray-900 mb-3">4. Data Security</h2>
              <p className="text-gray-600">
                We take reasonable measures to help protect your personal information from loss, theft,
                misuse, unauthorized access, disclosure, alteration, and destruction. However, no internet
                transmission is completely secure, and we cannot guarantee absolute security.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-gray-900 mb-3">5. Your Rights</h2>
              <p className="text-gray-600">
                You have the right to:
              </p>
              <ul className="list-disc list-inside text-gray-600 mt-2 space-y-1">
                <li>Access and receive a copy of your personal data</li>
                <li>Correct inaccurate personal data</li>
                <li>Request deletion of your personal data</li>
                <li>Object to processing of your personal data</li>
                <li>Withdraw consent at any time</li>
              </ul>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-gray-900 mb-3">6. Cookies</h2>
              <p className="text-gray-600">
                We use cookies and similar technologies to provide and improve our services,
                analyze usage, and personalize content. You can control cookies through your browser settings.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold text-gray-900 mb-3">7. Contact Us</h2>
              <p className="text-gray-600">
                If you have any questions about this Privacy Policy, please contact us at{' '}
                <a href="mailto:privacy@foreverhome.local" className="text-primary-500 hover:underline">
                  privacy@foreverhome.local
                </a>
              </p>
            </section>
          </div>
        </div>
      </div>
    </div>
  );
}

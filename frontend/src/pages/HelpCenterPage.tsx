import { Link } from 'react-router-dom';

export function HelpCenterPage() {
  const faqs = [
    {
      question: 'How do I adopt a pet?',
      answer: 'Browse our available pets, click on one you\'re interested in, and submit an adoption application. Our rescue partners will review your application and contact you.',
    },
    {
      question: 'What are the requirements to adopt?',
      answer: 'Requirements vary by rescue organization, but typically include being 18+, having a stable living situation, and being able to provide proper care for the pet.',
    },
    {
      question: 'How do I list a pet for adoption?',
      answer: 'Register as a foster, complete your profile, and submit your pet\'s information. A rescue organization will review and help facilitate the adoption.',
    },
    {
      question: 'Is there an adoption fee?',
      answer: 'Adoption fees vary by rescue organization and help cover veterinary care, vaccinations, and other costs associated with preparing pets for adoption.',
    },
    {
      question: 'How long does the adoption process take?',
      answer: 'The timeline varies depending on the rescue organization and specific circumstances, but typically ranges from a few days to a few weeks.',
    },
    {
      question: 'Can I return a pet if it doesn\'t work out?',
      answer: 'Most rescue organizations have return policies. Contact the rescue you adopted from to discuss options if you\'re having difficulties.',
    },
  ];

  return (
    <div className="container-app py-8">
      <div className="max-w-3xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Help Center</h1>
          <p className="text-gray-600">Find answers to common questions about Forever Home</p>
        </div>

        <div className="space-y-6">
          <section>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Frequently Asked Questions</h2>
            <div className="space-y-4">
              {faqs.map((faq, index) => (
                <div key={index} className="card p-6">
                  <h3 className="font-semibold text-gray-900 mb-2">{faq.question}</h3>
                  <p className="text-gray-600">{faq.answer}</p>
                </div>
              ))}
            </div>
          </section>

          <section className="card p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Still need help?</h2>
            <p className="text-gray-600 mb-4">
              Can't find what you're looking for? Our support team is here to help.
            </p>
            <Link
              to="/contact"
              className="inline-block bg-primary-500 text-white px-6 py-2 rounded hover:bg-primary-600 transition-colors"
            >
              Contact Support
            </Link>
          </section>
        </div>
      </div>
    </div>
  );
}

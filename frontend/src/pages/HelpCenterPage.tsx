import { useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { AdoptionJourney } from '../components';

type FaqCategory = 'general' | 'adopter' | 'foster' | 'vet' | 'rescue';

interface Faq {
  question: string;
  answer: string;
  category: FaqCategory;
}

const faqs: Faq[] = [
  // General FAQs
  {
    category: 'general',
    question: 'What is Forever Home?',
    answer: 'Forever Home is a pet adoption platform that connects pet owners looking to rehome their pets (fosters) with potential adopters through verified rescue organizations. We ensure every pet is properly vetted before being listed for adoption.',
  },
  {
    category: 'general',
    question: 'How does the adoption process work?',
    answer: 'The process has several steps: 1) A foster registers their pet and submits it to a rescue organization, 2) The rescue reviews and accepts the pet, 3) A vet verifies the pet\'s health status, 4) The pet becomes available for adoption, 5) Adopters apply and the rescue selects the best match, 6) The adoption is finalized.',
  },
  {
    category: 'general',
    question: 'Is there an adoption fee?',
    answer: 'Adoption fees vary by rescue organization and help cover veterinary care, vaccinations, and other costs associated with preparing pets for adoption. The rescue organization will provide fee details during the application process.',
  },
  {
    category: 'general',
    question: 'Can I return a pet if it doesn\'t work out?',
    answer: 'Most rescue organizations have return policies. Contact the rescue you adopted from to discuss options if you\'re having difficulties. They often provide support to help with the transition.',
  },

  // Adopter FAQs
  {
    category: 'adopter',
    question: 'How do I adopt a pet?',
    answer: 'Browse our available pets, click on one you\'re interested in, and submit an adoption application. Our rescue partners will review your application and contact you. You can have up to 3 active applications at a time.',
  },
  {
    category: 'adopter',
    question: 'What are the requirements to adopt?',
    answer: 'Requirements vary by rescue organization, but typically include being 18+, having a stable living situation, and being able to provide proper care for the pet. Some rescues may do home visits or reference checks.',
  },
  {
    category: 'adopter',
    question: 'How long does the adoption process take?',
    answer: 'The timeline varies depending on the rescue organization and specific circumstances, but typically ranges from a few days to a few weeks. Completing your profile thoroughly can speed up the process.',
  },
  {
    category: 'adopter',
    question: 'Why was my application rejected?',
    answer: 'Rejections can happen for various reasons - perhaps another applicant was a better match, or there were concerns about compatibility. You can always apply for other pets. If feedback was provided, use it to strengthen future applications.',
  },
  {
    category: 'adopter',
    question: 'Can I adopt if I live in an apartment?',
    answer: 'Yes! Many pets thrive in apartments. When applying, be sure to mention the size of your space, whether you have access to outdoor areas, and how you plan to exercise the pet. Some rescues may have specific requirements for certain breeds.',
  },

  // Foster FAQs
  {
    category: 'foster',
    question: 'How do I list a pet for adoption?',
    answer: 'Register as a foster, complete your profile, and submit your pet\'s information including photos and a description. You\'ll then select a rescue organization to work with, and they\'ll guide the pet through to adoption.',
  },
  {
    category: 'foster',
    question: 'Why do I need to select a rescue organization?',
    answer: 'Rescue organizations verify pets, coordinate vet checks, screen adopters, and facilitate safe adoptions. They\'re your partner in finding a great home for your pet and ensure the process is safe for everyone.',
  },
  {
    category: 'foster',
    question: 'What if my pet doesn\'t have a microchip?',
    answer: 'A microchip is required for all pets. Contact your local vet to have one implanted - it\'s a quick, safe procedure similar to a vaccination. The microchip helps vets verify pet identity and is important for their safety.',
  },
  {
    category: 'foster',
    question: 'Can I change which rescue organization is handling my pet?',
    answer: 'Yes, you can change rescue organizations while your pet is in the "Pending Rescue" status. Go to your dashboard and use the "Change Rescue Center" option. Note that you cannot change after the rescue has accepted your pet.',
  },
  {
    category: 'foster',
    question: 'How long will it take to find an adopter?',
    answer: 'This varies based on the pet\'s characteristics and demand in your area. Pets with complete profiles and multiple quality photos typically attract more interest. The rescue organization works actively to match your pet with suitable adopters.',
  },
  {
    category: 'foster',
    question: 'Can I withdraw my pet from adoption?',
    answer: 'Yes, you can withdraw your pet at any time before the adoption is finalized. Go to your dashboard and click "Withdraw" on the pet\'s card. Note that any active applications will be cancelled.',
  },

  // Vet FAQs
  {
    category: 'vet',
    question: 'How do I get approved to verify pets?',
    answer: 'Request approval from rescue organizations you work with via the "Request Approvals" page. Each rescue independently approves vets, so you may need to request approval from multiple organizations.',
  },
  {
    category: 'vet',
    question: 'What do I need to verify during a pet sign-off?',
    answer: 'You need to confirm: 1) The pet is neutered/spayed, 2) Vaccinations are up to date (core vaccines), 3) No major health concerns that would prevent adoption. You can add health notes for minor conditions.',
  },
  {
    category: 'vet',
    question: 'What if a pet doesn\'t pass the health check?',
    answer: 'If a pet doesn\'t meet requirements, you can decline them with a reason. The pet will be returned to the rescue organization, who will work with the foster to address the issues. The pet can be resubmitted after corrections.',
  },
  {
    category: 'vet',
    question: 'How do I look up a pet for verification?',
    answer: 'Use the microchip number to look up pets. Pets awaiting your verification from approved rescue organizations will also appear in your "Pending Sign-offs" queue automatically.',
  },

  // Rescue Organization FAQs
  {
    category: 'rescue',
    question: 'How do I register as a rescue organization?',
    answer: 'Register with your organization details and wait for admin verification. Once approved, you can accept pets from fosters, approve vets, and manage adoption applications.',
  },
  {
    category: 'rescue',
    question: 'How do I approve veterinarians?',
    answer: 'Vets request approval through the platform. You\'ll receive notifications of pending requests. Review their credentials and approve or deny as appropriate. Approved vets can then sign off on pets assigned to your organization.',
  },
  {
    category: 'rescue',
    question: 'How should I evaluate adoption applications?',
    answer: 'Consider the applicant\'s living situation, experience with pets, and compatibility with the specific pet. You can contact applicants for additional information or arrange home visits if your policies require them.',
  },
  {
    category: 'rescue',
    question: 'What happens when I approve an adoption?',
    answer: 'The pet moves to "In Progress" status and other applications are notified. Coordinate with the foster and approved adopter to arrange the pet handoff. Once complete, mark the adoption as finalized.',
  },
];

const categoryLabels: Record<FaqCategory, string> = {
  general: 'General',
  adopter: 'For Adopters',
  foster: 'For Fosters',
  vet: 'For Veterinarians',
  rescue: 'For Rescue Organizations',
};

const categoryIcons: Record<FaqCategory, ReactNode> = {
  general: (
    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
  ),
  adopter: (
    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
    </svg>
  ),
  foster: (
    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
    </svg>
  ),
  vet: (
    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
    </svg>
  ),
  rescue: (
    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
    </svg>
  ),
};

export function HelpCenterPage() {
  const [activeCategory, setActiveCategory] = useState<FaqCategory | 'all'>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedFaqs, setExpandedFaqs] = useState<Set<number>>(new Set());

  const toggleFaq = (index: number) => {
    const newExpanded = new Set(expandedFaqs);
    if (newExpanded.has(index)) {
      newExpanded.delete(index);
    } else {
      newExpanded.add(index);
    }
    setExpandedFaqs(newExpanded);
  };

  const filteredFaqs = faqs.filter((faq) => {
    const matchesCategory = activeCategory === 'all' || faq.category === activeCategory;
    const matchesSearch = searchQuery === '' ||
      faq.question.toLowerCase().includes(searchQuery.toLowerCase()) ||
      faq.answer.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  const categories: (FaqCategory | 'all')[] = ['all', 'general', 'adopter', 'foster', 'vet', 'rescue'];

  return (
    <div className="container-app py-8">
      <div className="max-w-4xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Help Center</h1>
          <p className="text-gray-600">Find answers to common questions about Forever Home</p>
        </div>

        {/* Adoption Journey Overview */}
        <div className="card p-6 mb-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">The Adoption Journey</h2>
          <p className="text-gray-600 mb-6">
            Every pet goes through these stages on their way to finding a forever home:
          </p>
          <AdoptionJourney currentStatus="AVAILABLE" variant="horizontal" />
        </div>

        {/* Search */}
        <div className="mb-6">
          <div className="relative">
            <svg
              className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
              />
            </svg>
            <input
              type="text"
              placeholder="Search help articles..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            />
          </div>
        </div>

        {/* Category Tabs */}
        <div className="flex flex-wrap gap-2 mb-6">
          {categories.map((category) => (
            <button
              key={category}
              onClick={() => setActiveCategory(category)}
              className={`px-4 py-2 rounded-full text-sm font-medium transition-colors flex items-center gap-2 ${
                activeCategory === category
                  ? 'bg-primary-500 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              {category !== 'all' && categoryIcons[category]}
              {category === 'all' ? 'All Topics' : categoryLabels[category]}
            </button>
          ))}
        </div>

        {/* FAQ List */}
        <div className="space-y-4">
          {filteredFaqs.length === 0 ? (
            <div className="card p-8 text-center">
              <p className="text-gray-600">No articles found matching your search.</p>
              <button
                onClick={() => {
                  setSearchQuery('');
                  setActiveCategory('all');
                }}
                className="text-primary-500 hover:underline mt-2"
              >
                Clear filters
              </button>
            </div>
          ) : (
            filteredFaqs.map((faq) => {
              const originalIndex = faqs.indexOf(faq);
              const isExpanded = expandedFaqs.has(originalIndex);

              return (
                <div key={originalIndex} className="card overflow-hidden">
                  <button
                    onClick={() => toggleFaq(originalIndex)}
                    className="w-full p-6 text-left flex items-start justify-between gap-4 hover:bg-gray-50 transition-colors"
                  >
                    <div className="flex items-start gap-3">
                      <span className={`flex-shrink-0 p-1.5 rounded-lg ${
                        activeCategory === 'all' ? 'bg-gray-100 text-gray-600' : 'bg-primary-100 text-primary-600'
                      }`}>
                        {categoryIcons[faq.category]}
                      </span>
                      <div>
                        <h3 className="font-semibold text-gray-900">{faq.question}</h3>
                        {activeCategory === 'all' && (
                          <span className="text-xs text-gray-500">{categoryLabels[faq.category]}</span>
                        )}
                      </div>
                    </div>
                    <svg
                      className={`w-5 h-5 text-gray-400 flex-shrink-0 transition-transform ${isExpanded ? 'rotate-180' : ''}`}
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                    </svg>
                  </button>
                  {isExpanded && (
                    <div className="px-6 pb-6 pt-0 ml-12">
                      <p className="text-gray-600">{faq.answer}</p>
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>

        {/* Still Need Help */}
        <section className="card p-6 mt-8">
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
  );
}

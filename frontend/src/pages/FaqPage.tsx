import { useState } from 'react';
import { Link } from 'react-router-dom';

interface FaqItem {
  question: string;
  answer: string;
}

function FaqAccordion({ items, colorClass }: { items: FaqItem[]; colorClass: string }) {
  const [expandedIndex, setExpandedIndex] = useState<number | null>(null);

  return (
    <div className="space-y-2">
      {items.map((faq, index) => (
        <div key={index} className={`card overflow-hidden border-l-4 ${colorClass}`}>
          <button
            onClick={() => setExpandedIndex(expandedIndex === index ? null : index)}
            className="w-full p-4 flex justify-between items-center text-left hover:bg-gray-50 transition-colors"
            aria-expanded={expandedIndex === index}
          >
            <h4 className="font-semibold text-gray-900 pr-4">{faq.question}</h4>
            <svg
              className={`w-5 h-5 text-gray-500 flex-shrink-0 transition-transform duration-200 ${
                expandedIndex === index ? 'rotate-180' : ''
              }`}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
            </svg>
          </button>
          {expandedIndex === index && (
            <div className="px-4 pb-4 text-gray-600 border-t border-gray-100 pt-3">
              {faq.answer}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

export function FaqPage() {
  const petJourneySteps = [
    {
      step: 1,
      title: 'Foster Creates Pet Profile',
      status: 'Draft',
      description: 'The current pet owner (Foster) creates a detailed profile including photos, health information, temperament, and the required microchip ID. The pet remains private during this stage.',
    },
    {
      step: 2,
      title: 'Submit to Rescue Organization',
      status: 'Pending Rescue',
      description: 'The Foster submits the pet to a verified rescue organization. The rescue reviews the pet information and decides whether to accept responsibility for facilitating the adoption.',
    },
    {
      step: 3,
      title: 'Veterinary Verification',
      status: 'Pending Vet',
      description: 'Once accepted by a rescue, the Foster takes the pet to a verified veterinarian. The vet confirms the pet is spayed/neutered, vaccinated, and healthy before signing off.',
    },
    {
      step: 4,
      title: 'Pet Becomes Available',
      status: 'Available',
      description: 'After vet sign-off, the pet is publicly listed on Forever Home. Potential adopters can now view the pet profile and submit adoption applications.',
    },
    {
      step: 5,
      title: 'Adoption In Progress',
      status: 'In Progress',
      description: 'When a rescue approves an adoption application, the pet moves to "In Progress". The rescue coordinates the handoff between the Foster and the approved Adopter.',
    },
    {
      step: 6,
      title: 'Adoption Complete',
      status: 'Adopted',
      description: 'The pet is transferred to their new forever home! The rescue finalizes the adoption, and the pet\'s journey is complete.',
    },
  ];

  const fosterFaqs = [
    {
      question: 'What is a Foster?',
      answer: 'A Foster is the current owner of a pet who wants to find a new loving home for their pet. Fosters create pet profiles, work with rescue organizations, and coordinate the handoff to the new adopter.',
    },
    {
      question: 'What information do I need to list my pet?',
      answer: 'You\'ll need basic details (name, species, breed, age, size, sex), a microchip ID (required for all pets), a description of your pet\'s personality, and up to 5 photos. You\'ll also provide health information about vaccinations and spay/neuter status.',
    },
    {
      question: 'Why is a microchip ID required?',
      answer: 'The microchip ID is essential for pet identification and safety. It allows veterinarians to look up your pet for health verification and ensures proper tracking throughout the adoption process.',
    },
    {
      question: 'Can I edit my pet\'s profile after submitting?',
      answer: 'While your pet is in Draft status, you can edit all fields. Once submitted to a rescue (Pending Rescue), you have limited editing ability. After rescue acceptance, editing is restricted to ensure information consistency.',
    },
    {
      question: 'What if the rescue declines my pet?',
      answer: 'If a rescue declines your submission, your pet returns to Draft status. You\'ll receive feedback explaining the decision, and you can make changes before resubmitting to the same or a different rescue.',
    },
  ];

  const rescueFaqs = [
    {
      question: 'What is a Rescue Organization?',
      answer: 'Rescue Organizations are verified non-profit groups that facilitate pet adoptions. They review foster submissions, approve veterinarians, screen adopter applications, and coordinate the entire adoption process.',
    },
    {
      question: 'How do rescues become verified?',
      answer: 'Rescue organizations must register and complete their profile with organization details. An admin reviews and verifies legitimate rescue organizations before they can accept pets.',
    },
    {
      question: 'What does a rescue do with my pet submission?',
      answer: 'The rescue reviews your pet\'s information, ensures it meets their criteria, and either accepts (moving to Pending Vet status) or declines with feedback. They then help manage the entire adoption process.',
    },
  ];

  const vetFaqs = [
    {
      question: 'What does a Vet do in the adoption process?',
      answer: 'Licensed veterinarians verify that pets meet health requirements before they can be listed for adoption. They confirm pets are spayed/neutered, have current vaccinations, and are in good health.',
    },
    {
      question: 'How does the vet find my pet?',
      answer: 'When you take your pet for verification, the vet looks up your pet using the microchip ID. This ensures the correct pet is being verified and creates a secure chain of custody.',
    },
    {
      question: 'What if my pet doesn\'t pass vet verification?',
      answer: 'If the vet cannot sign off (e.g., pet needs vaccinations or neutering), the pet returns to Pending Rescue status. You\'ll work with the rescue to address any health requirements before trying again.',
    },
    {
      question: 'What does the vet sign-off confirm?',
      answer: 'The vet verifies three things: the pet is spayed or neutered (with the date), vaccinations are current (with records), and the pet is in good health (or has documented conditions).',
    },
  ];

  const adopterFaqs = [
    {
      question: 'How do I find a pet to adopt?',
      answer: 'Browse our available pets page to view all pets with completed vet verification. You can filter by species, breed, size, age, and location to find your perfect match.',
    },
    {
      question: 'What happens when I submit an application?',
      answer: 'Your application goes to the rescue organization managing that pet. They review your information, may contact you for additional details, and will notify you of their decision.',
    },
    {
      question: 'How many applications can I have at once?',
      answer: 'You can have up to 3 active applications at a time. This ensures you can explore options while giving each application proper attention.',
    },
    {
      question: 'What are the general adoption requirements?',
      answer: 'Requirements vary by rescue, but typically include being 18+, having a stable living situation, and demonstrating the ability to provide proper care. Your application should explain why you\'d be a good match for the pet.',
    },
    {
      question: 'What happens after my application is approved?',
      answer: 'Once approved, the rescue coordinates the handoff between you and the current foster. You\'ll arrange a meeting, complete any final paperwork, and welcome your new pet to their forever home!',
    },
  ];

  const generalFaqs = [
    {
      question: 'Is there a fee to use Forever Home?',
      answer: 'Forever Home is free to use for fosters and adopters. Rescue organizations may have adoption fees that help cover veterinary care, vaccinations, and operational costs.',
    },
    {
      question: 'How long does the entire process take?',
      answer: 'The timeline varies depending on how quickly each step is completed. From listing to adoption, it typically takes 1-4 weeks, though some adoptions happen faster or take longer based on circumstances.',
    },
    {
      question: 'Can a pet be withdrawn from the process?',
      answer: 'Yes, fosters can withdraw their pet at any stage except after adoption is finalized. Withdrawn pets can be resubmitted later if circumstances change.',
    },
    {
      question: 'What if an adoption falls through?',
      answer: 'If an adoption doesn\'t work out during the In Progress stage, the pet returns to Available status and can receive new applications. The rescue handles these transitions.',
    },
  ];

  return (
    <div className="container-app py-8">
      <div className="max-w-4xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Pet Adoption Journey FAQ</h1>
          <p className="text-gray-600">
            Understanding how pets find their forever homes through our platform
          </p>
        </div>

        {/* Pet Journey Overview */}
        <section className="mb-10">
          <h2 className="text-2xl font-semibold text-gray-900 mb-6">The Pet's Journey: From Current Owner to Forever Home</h2>
          <p className="text-gray-600 mb-6">
            Every pet on Forever Home follows a carefully managed journey to ensure they find the right home.
            Here's how it works:
          </p>

          <div className="relative">
            {/* Journey Steps */}
            <div className="space-y-4">
              {petJourneySteps.map((step, index) => (
                <div key={step.step} className="card p-6">
                  <div className="flex items-start gap-4">
                    <div className="flex-shrink-0 w-10 h-10 bg-primary-500 text-white rounded-full flex items-center justify-center font-bold">
                      {step.step}
                    </div>
                    <div className="flex-grow">
                      <div className="flex items-center gap-3 mb-2">
                        <h3 className="font-semibold text-gray-900">{step.title}</h3>
                        <span className="text-xs px-2 py-1 bg-gray-100 text-gray-600 rounded-full">
                          Status: {step.status}
                        </span>
                      </div>
                      <p className="text-gray-600">{step.description}</p>
                    </div>
                  </div>
                  {index < petJourneySteps.length - 1 && (
                    <div className="ml-5 mt-4 h-4 border-l-2 border-dashed border-gray-300"></div>
                  )}
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Role-specific FAQs */}
        <section className="mb-10">
          <h2 className="text-2xl font-semibold text-gray-900 mb-6">Questions by Role</h2>

          {/* Foster FAQs */}
          <div className="mb-8">
            <h3 className="text-xl font-semibold text-gray-800 mb-4 flex items-center gap-2">
              <span className="w-3 h-3 bg-blue-500 rounded-full"></span>
              For Fosters (Current Pet Owners)
            </h3>
            <FaqAccordion items={fosterFaqs} colorClass="border-l-blue-500" />
          </div>

          {/* Rescue FAQs */}
          <div className="mb-8">
            <h3 className="text-xl font-semibold text-gray-800 mb-4 flex items-center gap-2">
              <span className="w-3 h-3 bg-green-500 rounded-full"></span>
              For Rescue Organizations
            </h3>
            <FaqAccordion items={rescueFaqs} colorClass="border-l-green-500" />
          </div>

          {/* Vet FAQs */}
          <div className="mb-8">
            <h3 className="text-xl font-semibold text-gray-800 mb-4 flex items-center gap-2">
              <span className="w-3 h-3 bg-purple-500 rounded-full"></span>
              For Veterinarians
            </h3>
            <FaqAccordion items={vetFaqs} colorClass="border-l-purple-500" />
          </div>

          {/* Adopter FAQs */}
          <div className="mb-8">
            <h3 className="text-xl font-semibold text-gray-800 mb-4 flex items-center gap-2">
              <span className="w-3 h-3 bg-orange-500 rounded-full"></span>
              For Adopters (Future Pet Owners)
            </h3>
            <FaqAccordion items={adopterFaqs} colorClass="border-l-orange-500" />
          </div>
        </section>

        {/* General FAQs */}
        <section className="mb-10">
          <h2 className="text-2xl font-semibold text-gray-900 mb-6">General Questions</h2>
          <FaqAccordion items={generalFaqs} colorClass="border-l-gray-400" />
        </section>

        {/* Still need help section */}
        <section className="card p-6 bg-gray-50">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Still have questions?</h2>
          <p className="text-gray-600 mb-4">
            Can't find what you're looking for? We're here to help.
          </p>
          <div className="flex gap-4 flex-wrap">
            <Link
              to="/help"
              className="inline-block bg-white border border-gray-300 text-gray-700 px-6 py-2 rounded hover:bg-gray-50 transition-colors"
            >
              Help Center
            </Link>
            <Link
              to="/contact"
              className="inline-block bg-primary-500 text-white px-6 py-2 rounded hover:bg-primary-600 transition-colors"
            >
              Contact Support
            </Link>
          </div>
        </section>
      </div>
    </div>
  );
}

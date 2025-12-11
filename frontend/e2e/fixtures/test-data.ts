/**
 * Test data generators and constants for e2e tests
 */

// Unique ID generator for test data
export const uniqueId = () => `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;

// Test pet data
export const generatePetData = (overrides = {}) => ({
  name: `TestPet-${uniqueId()}`,
  species: 'DOG',
  breed: 'Golden Retriever',
  sex: 'MALE',
  age: 3,
  size: 'LARGE',
  color: 'Golden',
  microchipId: `CHIP-${uniqueId()}`,
  description: 'A friendly golden retriever looking for a forever home.',
  medicalNotes: 'Up to date on vaccinations',
  behaviorNotes: 'Good with children and other pets',
  isNeutered: true,
  isVaccinated: true,
  isHouseTrained: true,
  goodWithKids: true,
  goodWithPets: true,
  ...overrides,
});

// Test user data for registration
export const generateUserData = (role: string, overrides = {}) => {
  const id = uniqueId();
  return {
    name: `Test User ${id}`,
    email: `test-${role.toLowerCase()}-${id}@example.com`,
    password: 'TestPass123!',
    role,
    ...overrides,
  };
};

// Profile data by role
export const generateProfileData = {
  foster: (overrides = {}) => ({
    firstName: 'Test',
    lastName: `Foster-${uniqueId()}`,
    phone: '555-123-4567',
    address: {
      street: '123 Test Street',
      city: 'Test City',
      state: 'CA',
      zipCode: '90210',
    },
    ...overrides,
  }),

  adopter: (overrides = {}) => ({
    firstName: 'Test',
    lastName: `Adopter-${uniqueId()}`,
    phone: '555-987-6543',
    livingSituation: 'HOUSE_OWNED',
    petExperience: 'Have owned multiple pets',
    hasYard: true,
    hasFence: true,
    address: {
      street: '456 Adopter Lane',
      city: 'Adoption City',
      state: 'NY',
      zipCode: '10001',
    },
    ...overrides,
  }),

  rescueOrg: (overrides = {}) => ({
    organizationName: `Test Rescue Org ${uniqueId()}`,
    phone: '555-456-7890',
    website: 'https://test-rescue.example.com',
    description: 'A test rescue organization dedicated to pet welfare.',
    contactName: 'Test Contact',
    contactEmail: `rescue-contact-${uniqueId()}@example.com`,
    address: {
      street: '789 Rescue Way',
      city: 'Rescue City',
      state: 'TX',
      zipCode: '75001',
    },
    ...overrides,
  }),

  vet: (overrides = {}) => ({
    clinicName: `Test Vet Clinic ${uniqueId()}`,
    licenseNumber: `VET-${uniqueId()}`,
    phone: '555-VET-CARE',
    website: 'https://test-vet.example.com',
    description: 'A test veterinary clinic providing excellent care.',
    address: {
      street: '321 Vet Blvd',
      city: 'Vet Town',
      state: 'FL',
      zipCode: '33101',
    },
    ...overrides,
  }),
};

// Adoption application data
export const generateApplicationData = (overrides = {}) => ({
  whyAdopt: 'I have always loved pets and want to provide a loving home.',
  experience: 'I have owned dogs for 10 years and have experience with large breeds.',
  homeDescription: 'Large house with fenced backyard, perfect for a dog.',
  veterinarianInfo: 'Dr. Smith at Local Vet Clinic - 555-VET-1234',
  references: 'John Doe (neighbor) - 555-111-2222',
  ...overrides,
});

// Vet sign-off data
export const generateVetSignOffData = (overrides = {}) => ({
  isNeutered: true,
  isVaccinated: true,
  isHealthy: true,
  notes: 'Pet is in excellent health and ready for adoption.',
  ...overrides,
});

// Test account credentials (matching backend TestDataSeeder)
export const TEST_ACCOUNTS = {
  admin: { email: 'admin@test.com', password: 'password123' },
  foster: { email: 'foster@test.com', password: 'password123' },
  adopter: { email: 'adopter@test.com', password: 'password123' },
  vet: { email: 'vet@test.com', password: 'password123' },
  rescue: { email: 'rescue@test.com', password: 'password123' },
};

// API endpoints for direct API calls in tests
export const API_ENDPOINTS = {
  auth: {
    login: '/api/auth/login',
    register: '/api/auth/register',
    refresh: '/api/auth/refresh',
    logout: '/api/auth/logout',
  },
  pets: {
    base: '/api/pets',
    byId: (id: string) => `/api/pets/${id}`,
    submit: (id: string) => `/api/pets/${id}/submit`,
    available: '/api/pets/available',
  },
  applications: {
    base: '/api/applications',
    byId: (id: string) => `/api/applications/${id}`,
  },
  vet: {
    lookup: '/api/vet/pets/lookup',
    signOff: (id: string) => `/api/vet/pets/${id}/sign-off`,
  },
  rescueOrg: {
    pets: '/api/rescue-org/pets',
    acceptPet: (id: string) => `/api/rescue-org/pets/${id}/accept`,
    applications: '/api/rescue-org/applications',
  },
  admin: {
    analytics: '/api/admin/analytics',
    users: '/api/admin/users',
  },
};

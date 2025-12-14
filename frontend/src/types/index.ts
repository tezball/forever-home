export type UserRole = 'ADMIN' | 'FOSTER' | 'ADOPTER' | 'VET' | 'RESCUE_ORG';
export type AccountStatus = 'PENDING' | 'ACTIVE' | 'SUSPENDED';
export type PetStatus = 'DRAFT' | 'PENDING_RESCUE' | 'PENDING_VET' | 'AVAILABLE' | 'IN_PROGRESS' | 'ADOPTED' | 'WITHDRAWN' | 'ON_HOLD';
export type Species = 'DOG' | 'CAT';
export type PetSize = 'SMALL' | 'MEDIUM' | 'LARGE';
export type PetSex = 'MALE' | 'FEMALE';
export type AgeUnit = 'MONTHS' | 'YEARS';
export type ApplicationStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN' | 'FINALIZED';

export interface User {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  status: AccountStatus;
  profileComplete: boolean;
}

export interface Pet {
  id: string;
  name: string;
  species: Species;
  breed: string | null;
  age: number;
  ageUnit: AgeUnit;
  sex: PetSex;
  size: PetSize;
  microchipId: string;
  description: string | null;
  healthNotes: string | null;
  status: PetStatus;
  fosterId: string;
  rescueOrgId: string | null;
  createdAt: string;
  imageUrls: string[];
  canSignOff?: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
  role: UserRole;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

export interface RegisterResponse {
  email: string;
  message: string;
}

export interface CreatePetRequest {
  name: string;
  species: Species;
  breed?: string;
  age: number;
  ageUnit: AgeUnit;
  sex: PetSex;
  size: PetSize;
  microchipId: string;
  description?: string;
  healthNotes?: string;
}

export interface AdoptionApplication {
  id: string;
  petId: string;
  petName: string;
  petImageUrl: string | null;
  adopterId: string;
  adopterName: string;
  adopterPhone: string | null;
  status: ApplicationStatus;
  livingSituation: string | null;
  petExperience: string | null;
  whyAdopt: string | null;
  submittedAt: string;
  reviewedAt: string | null;
  rejectionReason: string | null;
}

export interface Notification {
  id: string;
  userId: string;
  type: string;
  title: string;
  message: string;
  link: string | null;
  read: boolean;
  createdAt: string;
}

export interface PetImage {
  id: string;
  petId: string;
  url: string;
  isPrimary: boolean;
  displayOrder: number;
  uploadedAt: string;
}

export interface RescueOrganization {
  id: string;
  name: string;
  description: string | null;
  location: string | null;
  website: string | null;
  email: string | null;
  phone: string | null;
  logoUrl: string | null;
  petCount: number;
}

export interface VetSignOffHistory {
  id: string;
  petId: string;
  petName: string;
  petSpecies: Species | null;
  petBreed: string | null;
  petMicrochipId: string | null;
  petImageUrl: string | null;
  healthStatus: string;
  healthNotes: string | null;
  signedOffAt: string;
}

export interface Vet {
  id: string;
  userId: string;
  clinicName: string;
  licenseNumber: string;
  phone: string | null;
  website: string | null;
  description: string | null;
  logoUrl: string | null;
  verified: boolean;
}

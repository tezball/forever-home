export type UserRole = 'ADMIN' | 'FOSTER' | 'ADOPTER' | 'VET' | 'RESCUE_ORG';
export type AccountStatus = 'PENDING' | 'ACTIVE' | 'SUSPENDED';
export type PetStatus = 'DRAFT' | 'PENDING_RESCUE' | 'PENDING_VET' | 'AVAILABLE' | 'IN_PROGRESS' | 'ADOPTED' | 'WITHDRAWN' | 'ON_HOLD';
export type Species = 'DOG' | 'CAT';
export type PetSize = 'SMALL' | 'MEDIUM' | 'LARGE';
export type PetSex = 'MALE' | 'FEMALE';
export type AgeUnit = 'MONTHS' | 'YEARS';
export type ApplicationStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN';

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
  adopterId: string;
  status: ApplicationStatus;
  submittedAt: string;
  reviewedAt: string | null;
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

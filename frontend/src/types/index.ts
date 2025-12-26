export type UserRole = 'ADMIN' | 'FOSTER' | 'ADOPTER' | 'VET' | 'RESCUE_ORG';
export type AccountStatus = 'PENDING' | 'ACTIVE' | 'SUSPENDED';
export type PetStatus = 'DRAFT' | 'PENDING_RESCUE' | 'PENDING_VET' | 'AVAILABLE' | 'IN_PROGRESS' | 'ADOPTED' | 'WITHDRAWN' | 'ON_HOLD';
export type Species = 'DOG' | 'CAT';
export type PetSize = 'SMALL' | 'MEDIUM' | 'LARGE';
export type PetSex = 'MALE' | 'FEMALE';
export type AgeUnit = 'MONTHS' | 'YEARS';
export type ApplicationStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN' | 'FINALIZED';

// Pet breeds - Dog breeds
export type DogBreed = 'LABRADOR' | 'GOLDEN_RETRIEVER' | 'GERMAN_SHEPHERD' | 'FRENCH_BULLDOG' |
  'BULLDOG' | 'BEAGLE' | 'POODLE' | 'POODLE_MIX' | 'ROTTWEILER' | 'YORKSHIRE_TERRIER' |
  'BOXER' | 'DACHSHUND' | 'HUSKY' | 'SIBERIAN_HUSKY' | 'GREAT_DANE' | 'DOBERMAN' |
  'AUSTRALIAN_SHEPHERD' | 'CAVALIER_KING_CHARLES' | 'SHIH_TZU' | 'BOSTON_TERRIER' |
  'BERNESE_MOUNTAIN_DOG' | 'POMERANIAN' | 'HAVANESE' | 'SHETLAND_SHEEPDOG' | 'CORGI' |
  'PEMBROKE_WELSH_CORGI' | 'COCKER_SPANIEL' | 'BORDER_COLLIE' | 'CHIHUAHUA' |
  'MINIATURE_SCHNAUZER' | 'PIT_BULL' | 'MALTESE' | 'JACK_RUSSELL_TERRIER' | 'BICHON_FRISE' |
  'MIXED_DOG';

// Pet breeds - Cat breeds
export type CatBreed = 'TABBY' | 'DOMESTIC_SHORTHAIR' | 'DOMESTIC_LONGHAIR' | 'SIAMESE' |
  'MAINE_COON' | 'PERSIAN' | 'RAGDOLL' | 'BENGAL' | 'BRITISH_SHORTHAIR' | 'ABYSSINIAN' |
  'BIRMAN' | 'ORIENTAL_SHORTHAIR' | 'SPHYNX' | 'DEVON_REX' | 'SCOTTISH_FOLD' | 'RUSSIAN_BLUE' |
  'AMERICAN_SHORTHAIR' | 'NORWEGIAN_FOREST_CAT' | 'EXOTIC_SHORTHAIR' | 'BURMESE' | 'TONKINESE' |
  'HIMALAYAN' | 'TURKISH_ANGORA' | 'RAGAMUFFIN' | 'MIXED_CAT';

export type Breed = DogBreed | CatBreed;

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
  breed: Breed | null;
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
  holdReason?: string | null;
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
  breed?: Breed;
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
  fullAddress: string | null;
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
  petBreed: Breed | null;
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

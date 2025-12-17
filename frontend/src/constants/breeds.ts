import type { Species } from '../types';

export interface BreedOption {
  value: string;
  label: string;
  species: Species;
}

export const BREEDS: BreedOption[] = [
  // Dog breeds
  { value: 'LABRADOR', label: 'Labrador', species: 'DOG' },
  { value: 'GOLDEN_RETRIEVER', label: 'Golden Retriever', species: 'DOG' },
  { value: 'GERMAN_SHEPHERD', label: 'German Shepherd', species: 'DOG' },
  { value: 'FRENCH_BULLDOG', label: 'French Bulldog', species: 'DOG' },
  { value: 'BULLDOG', label: 'Bulldog', species: 'DOG' },
  { value: 'BEAGLE', label: 'Beagle', species: 'DOG' },
  { value: 'POODLE', label: 'Poodle', species: 'DOG' },
  { value: 'POODLE_MIX', label: 'Poodle Mix', species: 'DOG' },
  { value: 'ROTTWEILER', label: 'Rottweiler', species: 'DOG' },
  { value: 'YORKSHIRE_TERRIER', label: 'Yorkshire Terrier', species: 'DOG' },
  { value: 'BOXER', label: 'Boxer', species: 'DOG' },
  { value: 'DACHSHUND', label: 'Dachshund', species: 'DOG' },
  { value: 'HUSKY', label: 'Husky', species: 'DOG' },
  { value: 'SIBERIAN_HUSKY', label: 'Siberian Husky', species: 'DOG' },
  { value: 'GREAT_DANE', label: 'Great Dane', species: 'DOG' },
  { value: 'DOBERMAN', label: 'Doberman', species: 'DOG' },
  { value: 'AUSTRALIAN_SHEPHERD', label: 'Australian Shepherd', species: 'DOG' },
  { value: 'CAVALIER_KING_CHARLES', label: 'Cavalier King Charles', species: 'DOG' },
  { value: 'SHIH_TZU', label: 'Shih Tzu', species: 'DOG' },
  { value: 'BOSTON_TERRIER', label: 'Boston Terrier', species: 'DOG' },
  { value: 'BERNESE_MOUNTAIN_DOG', label: 'Bernese Mountain Dog', species: 'DOG' },
  { value: 'POMERANIAN', label: 'Pomeranian', species: 'DOG' },
  { value: 'HAVANESE', label: 'Havanese', species: 'DOG' },
  { value: 'SHETLAND_SHEEPDOG', label: 'Shetland Sheepdog', species: 'DOG' },
  { value: 'CORGI', label: 'Corgi', species: 'DOG' },
  { value: 'PEMBROKE_WELSH_CORGI', label: 'Pembroke Welsh Corgi', species: 'DOG' },
  { value: 'COCKER_SPANIEL', label: 'Cocker Spaniel', species: 'DOG' },
  { value: 'BORDER_COLLIE', label: 'Border Collie', species: 'DOG' },
  { value: 'CHIHUAHUA', label: 'Chihuahua', species: 'DOG' },
  { value: 'MINIATURE_SCHNAUZER', label: 'Miniature Schnauzer', species: 'DOG' },
  { value: 'PIT_BULL', label: 'Pit Bull', species: 'DOG' },
  { value: 'MALTESE', label: 'Maltese', species: 'DOG' },
  { value: 'JACK_RUSSELL_TERRIER', label: 'Jack Russell Terrier', species: 'DOG' },
  { value: 'BICHON_FRISE', label: 'Bichon Frise', species: 'DOG' },
  { value: 'MIXED_DOG', label: 'Mixed Breed', species: 'DOG' },

  // Cat breeds
  { value: 'TABBY', label: 'Tabby', species: 'CAT' },
  { value: 'DOMESTIC_SHORTHAIR', label: 'Domestic Shorthair', species: 'CAT' },
  { value: 'DOMESTIC_LONGHAIR', label: 'Domestic Longhair', species: 'CAT' },
  { value: 'SIAMESE', label: 'Siamese', species: 'CAT' },
  { value: 'MAINE_COON', label: 'Maine Coon', species: 'CAT' },
  { value: 'PERSIAN', label: 'Persian', species: 'CAT' },
  { value: 'RAGDOLL', label: 'Ragdoll', species: 'CAT' },
  { value: 'BENGAL', label: 'Bengal', species: 'CAT' },
  { value: 'BRITISH_SHORTHAIR', label: 'British Shorthair', species: 'CAT' },
  { value: 'ABYSSINIAN', label: 'Abyssinian', species: 'CAT' },
  { value: 'BIRMAN', label: 'Birman', species: 'CAT' },
  { value: 'ORIENTAL_SHORTHAIR', label: 'Oriental Shorthair', species: 'CAT' },
  { value: 'SPHYNX', label: 'Sphynx', species: 'CAT' },
  { value: 'DEVON_REX', label: 'Devon Rex', species: 'CAT' },
  { value: 'SCOTTISH_FOLD', label: 'Scottish Fold', species: 'CAT' },
  { value: 'RUSSIAN_BLUE', label: 'Russian Blue', species: 'CAT' },
  { value: 'AMERICAN_SHORTHAIR', label: 'American Shorthair', species: 'CAT' },
  { value: 'NORWEGIAN_FOREST_CAT', label: 'Norwegian Forest Cat', species: 'CAT' },
  { value: 'EXOTIC_SHORTHAIR', label: 'Exotic Shorthair', species: 'CAT' },
  { value: 'BURMESE', label: 'Burmese', species: 'CAT' },
  { value: 'TONKINESE', label: 'Tonkinese', species: 'CAT' },
  { value: 'HIMALAYAN', label: 'Himalayan', species: 'CAT' },
  { value: 'TURKISH_ANGORA', label: 'Turkish Angora', species: 'CAT' },
  { value: 'RAGAMUFFIN', label: 'Ragamuffin', species: 'CAT' },
  { value: 'MIXED_CAT', label: 'Mixed Breed', species: 'CAT' },
];

export const getBreedsBySpecies = (species: Species): BreedOption[] =>
  BREEDS.filter((b) => b.species === species);

export const getBreedLabel = (value: string): string =>
  BREEDS.find((b) => b.value === value)?.label ?? value;

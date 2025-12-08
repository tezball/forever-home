import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { Button, Input, Select, ImageUpload } from '../components';
import type { Pet, PetImage, Species, PetSize, PetSex, AgeUnit, CreatePetRequest } from '../types';
import apiClient from '../api/client';

const speciesOptions = [
  { value: 'DOG', label: 'Dog' },
  { value: 'CAT', label: 'Cat' },
];

const sizeOptions = [
  { value: 'SMALL', label: 'Small (up to 20 lbs)' },
  { value: 'MEDIUM', label: 'Medium (21-60 lbs)' },
  { value: 'LARGE', label: 'Large (61+ lbs)' },
];

const sexOptions = [
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
];

const ageUnitOptions = [
  { value: 'MONTHS', label: 'Months' },
  { value: 'YEARS', label: 'Years' },
];

interface FormData {
  name: string;
  species: Species;
  breed: string;
  age: string;
  ageUnit: AgeUnit;
  sex: PetSex;
  size: PetSize;
  microchipId: string;
  description: string;
  healthNotes: string;
}

const initialFormData: FormData = {
  name: '',
  species: 'DOG',
  breed: '',
  age: '',
  ageUnit: 'YEARS',
  sex: 'MALE',
  size: 'MEDIUM',
  microchipId: '',
  description: '',
  healthNotes: '',
};

export function PetFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEditing = Boolean(id);

  const [formData, setFormData] = useState<FormData>(initialFormData);
  const [images, setImages] = useState<PetImage[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState<Partial<Record<keyof FormData, string>>>({});
  const [generalError, setGeneralError] = useState('');
  const [petId, setPetId] = useState<string | null>(id || null);

  useEffect(() => {
    if (id) {
      fetchPet(id);
    }
  }, [id]);

  const fetchPet = async (petId: string) => {
    setLoading(true);
    try {
      const [petResponse, imagesResponse] = await Promise.all([
        apiClient.get<Pet>(`/pets/${petId}`),
        apiClient.get<PetImage[]>(`/pets/${petId}/images`),
      ]);

      const pet = petResponse.data;
      setFormData({
        name: pet.name,
        species: pet.species,
        breed: pet.breed || '',
        age: pet.age.toString(),
        ageUnit: pet.ageUnit,
        sex: pet.sex,
        size: pet.size,
        microchipId: pet.microchipId,
        description: pet.description || '',
        healthNotes: pet.healthNotes || '',
      });
      setImages(imagesResponse.data);
      setPetId(petId);
    } catch {
      setGeneralError('Failed to load pet details');
    } finally {
      setLoading(false);
    }
  };

  const validateForm = (): boolean => {
    const newErrors: Partial<Record<keyof FormData, string>> = {};

    if (!formData.name.trim()) {
      newErrors.name = 'Name is required';
    }

    if (!formData.age || parseInt(formData.age) < 0) {
      newErrors.age = 'Valid age is required';
    }

    if (!formData.microchipId.trim()) {
      newErrors.microchipId = 'Microchip ID is required';
    }

    if (formData.description && formData.description.length > 500) {
      newErrors.description = 'Description must be 500 characters or less';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name as keyof FormData]) {
      setErrors((prev) => ({ ...prev, [name]: undefined }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;

    setSaving(true);
    setGeneralError('');

    try {
      if (isEditing && petId) {
        // Update existing pet
        await apiClient.put(`/pets/${petId}`, {
          name: formData.name.trim(),
          description: formData.description.trim() || null,
          healthNotes: formData.healthNotes.trim() || null,
        });
        navigate(`/pets/${petId}`);
      } else {
        // Create new pet
        const createRequest: CreatePetRequest = {
          name: formData.name.trim(),
          species: formData.species,
          breed: formData.breed.trim() || undefined,
          age: parseInt(formData.age),
          ageUnit: formData.ageUnit,
          sex: formData.sex,
          size: formData.size,
          microchipId: formData.microchipId.trim(),
          description: formData.description.trim() || undefined,
          healthNotes: formData.healthNotes.trim() || undefined,
        };

        const response = await apiClient.post<Pet>('/pets', createRequest);
        setPetId(response.data.id);
        // After creating, stay on page to allow image uploads
        navigate(`/foster/pets/${response.data.id}/edit`, { replace: true });
      }
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      setGeneralError(error.response?.data?.message || 'Failed to save pet. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="container-app py-8">
      {/* Back link */}
      <Link
        to="/foster/dashboard"
        className="inline-flex items-center text-gray-600 hover:text-primary-500 mb-6"
      >
        <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
        </svg>
        Back to Dashboard
      </Link>

      <div className="max-w-2xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">
          {isEditing ? 'Edit Pet' : 'Register a New Pet'}
        </h1>

        {generalError && (
          <div className="bg-error-50 border border-error-200 text-error-700 px-4 py-3 rounded-lg mb-6">
            {generalError}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Basic Info */}
          <div className="card p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Basic Information</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <Input
                label="Pet Name *"
                name="name"
                value={formData.name}
                onChange={handleChange}
                error={errors.name}
                placeholder="Enter pet's name"
              />

              <Select
                label="Species *"
                name="species"
                value={formData.species}
                onChange={handleChange}
                options={speciesOptions}
                disabled={isEditing}
              />

              <Input
                label="Breed"
                name="breed"
                value={formData.breed}
                onChange={handleChange}
                placeholder="e.g., Golden Retriever"
                disabled={isEditing}
              />

              <Select
                label="Sex *"
                name="sex"
                value={formData.sex}
                onChange={handleChange}
                options={sexOptions}
                disabled={isEditing}
              />

              <div className="flex gap-2">
                <div className="flex-1">
                  <Input
                    label="Age *"
                    name="age"
                    type="number"
                    min="0"
                    value={formData.age}
                    onChange={handleChange}
                    error={errors.age}
                    placeholder="Age"
                    disabled={isEditing}
                  />
                </div>
                <div className="w-32">
                  <Select
                    label="&nbsp;"
                    name="ageUnit"
                    value={formData.ageUnit}
                    onChange={handleChange}
                    options={ageUnitOptions}
                    disabled={isEditing}
                  />
                </div>
              </div>

              <Select
                label="Size *"
                name="size"
                value={formData.size}
                onChange={handleChange}
                options={sizeOptions}
                disabled={isEditing}
              />

              <Input
                label="Microchip ID *"
                name="microchipId"
                value={formData.microchipId}
                onChange={handleChange}
                error={errors.microchipId}
                placeholder="Enter microchip ID"
                disabled={isEditing}
              />
            </div>
          </div>

          {/* Description */}
          <div className="card p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">About the Pet</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Description ({formData.description.length}/500)
                </label>
                <textarea
                  name="description"
                  value={formData.description}
                  onChange={handleChange}
                  rows={4}
                  maxLength={500}
                  className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500 ${
                    errors.description ? 'border-error-500' : 'border-gray-300'
                  }`}
                  placeholder="Tell potential adopters about this pet's personality, history, and what makes them special..."
                />
                {errors.description && (
                  <p className="mt-1 text-sm text-error-500">{errors.description}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Health Notes
                </label>
                <textarea
                  name="healthNotes"
                  value={formData.healthNotes}
                  onChange={handleChange}
                  rows={3}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                  placeholder="Vaccination status, medical history, special needs, etc."
                />
              </div>
            </div>
          </div>

          {/* Images - Only show after pet is created */}
          {petId && (
            <div className="card p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">Photos</h2>
              <ImageUpload
                petId={petId}
                images={images}
                maxImages={5}
                onImagesChange={setImages}
              />
            </div>
          )}

          {!petId && (
            <div className="card p-6 bg-secondary-50">
              <h2 className="text-lg font-semibold text-gray-900 mb-2">Photos</h2>
              <p className="text-gray-600">
                You'll be able to upload photos after saving the pet's basic information.
              </p>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-4">
            <Button
              type="button"
              variant="outline"
              onClick={() => navigate('/foster/dashboard')}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button type="submit" variant="primary" loading={saving} className="flex-1">
              {isEditing ? 'Save Changes' : 'Create Pet'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

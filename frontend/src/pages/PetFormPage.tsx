import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Button, Input, Select, ImageUpload, Textarea, Breadcrumb, Combobox } from '../components';
import { useAuth } from '../contexts/AuthContext';
import type { Pet, PetImage, Species, PetSize, PetSex, AgeUnit, CreatePetRequest, Breed } from '../types';
import apiClient from '../api/client';
import { getBreedsBySpecies } from '../constants/breeds';

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
  const { user } = useAuth();
  const isEditing = Boolean(id);
  const isRescueOrg = user?.role === 'RESCUE_ORG';
  const basePath = isRescueOrg ? '/rescue' : '/foster';
  const dashboardPath = `${basePath}/dashboard`;

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

  // Clear breed when species changes (if current breed isn't valid for new species)
  useEffect(() => {
    if (formData.breed) {
      const validBreeds = getBreedsBySpecies(formData.species);
      if (!validBreeds.find((b) => b.value === formData.breed)) {
        setFormData((prev) => ({ ...prev, breed: '' }));
      }
    }
  }, [formData.species]);

  const handleBreedChange = (value: string) => {
    setFormData((prev) => ({ ...prev, breed: value }));
    if (errors.breed) {
      setErrors((prev) => ({ ...prev, breed: undefined }));
    }
  };

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
          breed: (formData.breed as Breed) || undefined,
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
        navigate(`${basePath}/pets/${response.data.id}/edit`, { replace: true });
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
      <Breadcrumb items={[{ label: isEditing ? 'Edit Pet' : 'Register New Pet' }]} />

      <div className="max-w-2xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          {isEditing ? 'Edit Pet' : 'Register a New Pet'}
        </h1>
        <p className="text-gray-600 mb-8">
          {isEditing
            ? 'Update the pet\'s information below.'
            : 'Fill in the details below to register a pet for adoption. Fields marked with * are required.'}
        </p>

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
                label="Pet Name"
                name="name"
                value={formData.name}
                onChange={handleChange}
                error={errors.name}
                placeholder="Enter pet's name"
                required
              />

              <Select
                label="Species"
                name="species"
                value={formData.species}
                onChange={handleChange}
                options={speciesOptions}
                disabled={isEditing}
                required
              />

              <Combobox
                label="Breed"
                value={formData.breed}
                onChange={handleBreedChange}
                options={getBreedsBySpecies(formData.species)}
                placeholder="Select or type to filter..."
                disabled={isEditing}
                hint="Optional - helps adopters find the right match"
              />

              <Select
                label="Sex"
                name="sex"
                value={formData.sex}
                onChange={handleChange}
                options={sexOptions}
                disabled={isEditing}
                required
              />

              <div className="flex gap-2">
                <div className="flex-1">
                  <Input
                    label="Age"
                    name="age"
                    type="number"
                    min="0"
                    value={formData.age}
                    onChange={handleChange}
                    error={errors.age}
                    placeholder="Age"
                    disabled={isEditing}
                    required
                  />
                </div>
                <div className="w-32">
                  <Select
                    label="Unit"
                    name="ageUnit"
                    value={formData.ageUnit}
                    onChange={handleChange}
                    options={ageUnitOptions}
                    disabled={isEditing}
                  />
                </div>
              </div>

              <Select
                label="Size"
                name="size"
                value={formData.size}
                onChange={handleChange}
                options={sizeOptions}
                disabled={isEditing}
                required
              />

              <Input
                label="Microchip ID"
                name="microchipId"
                value={formData.microchipId}
                onChange={handleChange}
                error={errors.microchipId}
                placeholder="Enter microchip ID"
                disabled={isEditing}
                hint="Required for vet verification"
                required
              />
            </div>
          </div>

          {/* Description */}
          <div className="card p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">About the Pet</h2>
            <div className="space-y-4">
              <Textarea
                label={`Description (${formData.description.length}/500)`}
                name="description"
                value={formData.description}
                onChange={handleChange}
                rows={4}
                maxLength={500}
                error={errors.description}
                placeholder="Tell potential adopters about this pet's personality, history, and what makes them special..."
                hint="A good description helps adopters connect with the pet"
              />

              <Textarea
                label="Health Notes"
                name="healthNotes"
                value={formData.healthNotes}
                onChange={handleChange}
                rows={3}
                placeholder="Vaccination status, medical history, special needs, etc."
                hint="This information will be shared with the vet during sign-off"
              />
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
              onClick={() => navigate(dashboardPath)}
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

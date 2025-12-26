import { useState, useRef, useCallback } from 'react';
import type { PetImage } from '../types';
import apiClient from '../api/client';

interface ImageUploadProps {
  petId: string;
  images: PetImage[];
  maxImages?: number;
  onImagesChange: (images: PetImage[]) => void;
  disabled?: boolean;
}

export function ImageUpload({
  petId,
  images,
  maxImages = 5,
  onImagesChange,
  disabled = false,
}: ImageUploadProps) {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleUpload = useCallback(async (files: FileList | null) => {
    if (!files || files.length === 0) return;

    const remainingSlots = maxImages - images.length;
    if (remainingSlots <= 0) {
      setError(`Maximum ${maxImages} images allowed`);
      return;
    }

    const filesToUpload = Array.from(files).slice(0, remainingSlots);
    setUploading(true);
    setError(null);

    try {
      const newImages: PetImage[] = [];
      for (const file of filesToUpload) {
        const formData = new FormData();
        formData.append('file', file);

        const response = await apiClient.post<PetImage>(
          `/pets/${petId}/images`,
          formData,
          {
            headers: {
              'Content-Type': 'multipart/form-data',
            },
          }
        );
        newImages.push(response.data);
      }
      onImagesChange([...images, ...newImages]);
    } catch {
      setError('Failed to upload image. Please try again.');
    } finally {
      setUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  }, [petId, images, maxImages, onImagesChange]);

  const handleDelete = useCallback(async (imageId: string) => {
    try {
      await apiClient.delete(`/pets/${petId}/images/${imageId}`);
      const updatedImages = images.filter((img) => img.id !== imageId);
      onImagesChange(updatedImages);
    } catch {
      setError('Failed to delete image. Please try again.');
    }
  }, [petId, images, onImagesChange]);

  const handleSetPrimary = useCallback(async (imageId: string) => {
    try {
      await apiClient.put(`/pets/${petId}/images/${imageId}/primary`);
      const updatedImages = images.map((img) => ({
        ...img,
        isPrimary: img.id === imageId,
      }));
      onImagesChange(updatedImages);
    } catch {
      setError('Failed to set primary image. Please try again.');
    }
  }, [petId, images, onImagesChange]);

  const handleDrag = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  }, []);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleUpload(e.dataTransfer.files);
    }
  }, [handleUpload]);

  const canUpload = images.length < maxImages && !disabled;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <label className="block text-sm font-medium text-gray-700">
          Pet Photos ({images.length}/{maxImages})
        </label>
        {images.length > 0 && (
          <span className="text-xs text-gray-500">
            Click star to set main photo
          </span>
        )}
      </div>

      {error && (
        <div className="bg-error-50 text-error-600 px-3 py-2 rounded-lg text-sm">
          {error}
        </div>
      )}

      {/* Image Grid */}
      {images.length > 0 && (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-4">
          {images.map((image) => (
            <div
              key={image.id}
              className={`relative group aspect-square rounded-lg overflow-hidden border-2 ${
                image.isPrimary ? 'border-primary-500' : 'border-transparent'
              }`}
            >
              <img
                src={image.url}
                alt="Pet"
                className="w-full h-full object-cover"
              />
              {image.isPrimary && (
                <div className="absolute top-1 left-1 bg-primary-500 text-white text-xs px-2 py-0.5 rounded">
                  Main
                </div>
              )}
              <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-40 transition-all flex items-center justify-center opacity-0 group-hover:opacity-100">
                <div className="flex gap-2">
                  {!image.isPrimary && !disabled && (
                    <button
                      onClick={() => handleSetPrimary(image.id)}
                      className="p-2 bg-white rounded-full hover:bg-gray-100 transition-colors"
                      title="Set as main photo"
                    >
                      <svg
                        className="w-5 h-5 text-yellow-500"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z"
                        />
                      </svg>
                    </button>
                  )}
                  {!disabled && (
                    <button
                      onClick={() => handleDelete(image.id)}
                      className="p-2 bg-white rounded-full hover:bg-gray-100 transition-colors"
                      title="Delete photo"
                    >
                      <svg
                        className="w-5 h-5 text-error-500"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                        />
                      </svg>
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Upload Area */}
      {canUpload && (
        <div
          className={`relative border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
            dragActive
              ? 'border-primary-500 bg-primary-50'
              : 'border-gray-300 hover:border-gray-400'
          } ${uploading ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
          onDragEnter={handleDrag}
          onDragLeave={handleDrag}
          onDragOver={handleDrag}
          onDrop={handleDrop}
          onClick={() => !uploading && fileInputRef.current?.click()}
        >
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            multiple
            className="hidden"
            onChange={(e) => handleUpload(e.target.files)}
            disabled={uploading}
          />
          {uploading ? (
            <div className="flex flex-col items-center">
              <svg
                className="animate-spin h-10 w-10 text-primary-500 mb-3"
                fill="none"
                viewBox="0 0 24 24"
              >
                <circle
                  className="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  strokeWidth="4"
                />
                <path
                  className="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
                />
              </svg>
              <p className="text-gray-600">Uploading...</p>
            </div>
          ) : (
            <>
              <svg
                className="mx-auto h-12 w-12 text-gray-400"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
                />
              </svg>
              <p className="mt-2 text-sm text-gray-600">
                <span className="font-medium text-primary-500">Click to upload</span>
                {' or drag and drop'}
              </p>
              <p className="mt-1 text-xs text-gray-500">
                PNG, JPG, GIF or WebP up to 5MB
              </p>
            </>
          )}
        </div>
      )}

      {images.length >= maxImages && (
        <p className="text-sm text-gray-500 text-center">
          Maximum number of photos reached. Delete a photo to upload a new one.
        </p>
      )}
    </div>
  );
}

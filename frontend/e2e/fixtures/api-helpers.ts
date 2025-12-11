/**
 * API helper functions for e2e tests
 * These helpers allow tests to set up state via API calls before testing UI
 */
import { APIRequestContext } from '@playwright/test';
import {
  generatePetData,
  generateApplicationData,
  generateVetSignOffData,
  API_ENDPOINTS,
} from './test-data';

const API_BASE = process.env.API_URL || 'http://localhost:8080';

interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

/**
 * Login via API and return tokens
 */
export async function apiLogin(
  request: APIRequestContext,
  email: string,
  password: string
): Promise<AuthTokens> {
  const response = await request.post(`${API_BASE}${API_ENDPOINTS.auth.login}`, {
    data: { email, password },
  });

  if (!response.ok()) {
    throw new Error(`Login failed: ${response.status()} ${await response.text()}`);
  }

  return response.json();
}

/**
 * Create a pet via API (as foster)
 */
export async function apiCreatePet(
  request: APIRequestContext,
  accessToken: string,
  petData = generatePetData()
) {
  const response = await request.post(`${API_BASE}${API_ENDPOINTS.pets.base}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    data: petData,
  });

  if (!response.ok()) {
    throw new Error(`Create pet failed: ${response.status()} ${await response.text()}`);
  }

  return response.json();
}

/**
 * Submit pet to rescue org via API
 */
export async function apiSubmitPet(
  request: APIRequestContext,
  accessToken: string,
  petId: string,
  rescueOrgId: string
) {
  const response = await request.post(
    `${API_BASE}${API_ENDPOINTS.pets.submit(petId)}`,
    {
      headers: { Authorization: `Bearer ${accessToken}` },
      data: { rescueOrgId },
    }
  );

  if (!response.ok()) {
    throw new Error(`Submit pet failed: ${response.status()} ${await response.text()}`);
  }

  return response.json();
}

/**
 * Accept pet via API (as rescue org)
 */
export async function apiAcceptPet(
  request: APIRequestContext,
  accessToken: string,
  petId: string
) {
  const response = await request.post(
    `${API_BASE}${API_ENDPOINTS.rescueOrg.acceptPet(petId)}`,
    {
      headers: { Authorization: `Bearer ${accessToken}` },
    }
  );

  if (!response.ok()) {
    throw new Error(`Accept pet failed: ${response.status()} ${await response.text()}`);
  }

  return response.json();
}

/**
 * Vet sign-off via API
 */
export async function apiVetSignOff(
  request: APIRequestContext,
  accessToken: string,
  petId: string,
  signOffData = generateVetSignOffData()
) {
  const response = await request.post(
    `${API_BASE}${API_ENDPOINTS.vet.signOff(petId)}`,
    {
      headers: { Authorization: `Bearer ${accessToken}` },
      data: signOffData,
    }
  );

  if (!response.ok()) {
    throw new Error(`Vet sign-off failed: ${response.status()} ${await response.text()}`);
  }

  return response.json();
}

/**
 * Submit adoption application via API
 */
export async function apiSubmitApplication(
  request: APIRequestContext,
  accessToken: string,
  petId: string,
  applicationData = generateApplicationData()
) {
  const response = await request.post(`${API_BASE}${API_ENDPOINTS.applications.base}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    data: { petId, ...applicationData },
  });

  if (!response.ok()) {
    throw new Error(`Submit application failed: ${response.status()} ${await response.text()}`);
  }

  return response.json();
}

/**
 * Get available pets via API
 */
export async function apiGetAvailablePets(request: APIRequestContext) {
  const response = await request.get(`${API_BASE}${API_ENDPOINTS.pets.available}`);

  if (!response.ok()) {
    throw new Error(`Get pets failed: ${response.status()} ${await response.text()}`);
  }

  return response.json();
}

/**
 * Get pet by microchip (as vet)
 */
export async function apiLookupPetByMicrochip(
  request: APIRequestContext,
  accessToken: string,
  microchipId: string
) {
  const response = await request.get(
    `${API_BASE}${API_ENDPOINTS.vet.lookup}?microchipId=${encodeURIComponent(microchipId)}`,
    {
      headers: { Authorization: `Bearer ${accessToken}` },
    }
  );

  if (!response.ok()) {
    throw new Error(`Pet lookup failed: ${response.status()} ${await response.text()}`);
  }

  return response.json();
}

/**
 * Setup helper: Create a pet and move it through the adoption pipeline to a specific status
 */
export async function setupPetAtStatus(
  request: APIRequestContext,
  tokens: {
    foster: AuthTokens;
    rescue: AuthTokens;
    vet: AuthTokens;
  },
  targetStatus: 'DRAFT' | 'PENDING_RESCUE' | 'PENDING_VET' | 'AVAILABLE',
  rescueOrgId: string
) {
  const petData = generatePetData();
  const pet = await apiCreatePet(request, tokens.foster.accessToken, petData);

  if (targetStatus === 'DRAFT') {
    return { pet, petData };
  }

  await apiSubmitPet(request, tokens.foster.accessToken, pet.id, rescueOrgId);

  if (targetStatus === 'PENDING_RESCUE') {
    return { pet: { ...pet, status: 'PENDING_RESCUE' }, petData };
  }

  await apiAcceptPet(request, tokens.rescue.accessToken, pet.id);

  if (targetStatus === 'PENDING_VET') {
    return { pet: { ...pet, status: 'PENDING_VET' }, petData };
  }

  await apiVetSignOff(request, tokens.vet.accessToken, pet.id);

  return { pet: { ...pet, status: 'AVAILABLE' }, petData };
}

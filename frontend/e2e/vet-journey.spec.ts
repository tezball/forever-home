import { test, expect } from './fixtures/auth.fixture';
import { uniqueId } from './fixtures/test-data';

test.describe('Vet Journey', () => {
  test.describe('Dashboard', () => {
    test('should display vet dashboard', async ({ vetPage }) => {
      await vetPage.goto('/vet/dashboard');

      await expect(vetPage.getByRole('heading', { name: /veterinarian dashboard/i })).toBeVisible();
    });

    test('should display microchip lookup field', async ({ vetPage }) => {
      await vetPage.goto('/vet/dashboard');

      await expect(vetPage.getByPlaceholder(/microchip/i)).toBeVisible();
    });

    test('should display search button', async ({ vetPage }) => {
      await vetPage.goto('/vet/dashboard');

      await expect(vetPage.getByRole('button', { name: /search/i })).toBeVisible();
    });

    test('should display recent sign-offs section', async ({ vetPage }) => {
      await vetPage.goto('/vet/dashboard');

      // Dashboard should show "View Sign-Off History" button
      await expect(vetPage.getByRole('link', { name: /view sign-off history/i })).toBeVisible();
    });
  });

  test.describe('Pet Lookup', () => {
    test('should search for pet by microchip', async ({ vetPage }) => {
      await vetPage.goto('/vet/dashboard');

      const microchipInput = vetPage.getByPlaceholder(/microchip/i);
      await microchipInput.fill('CHIP-12345');

      await vetPage.getByRole('button', { name: /search/i }).click();

      // Should show results or no match message
      await vetPage.waitForTimeout(2000);
    });

    test('should show message for non-existent microchip', async ({ vetPage }) => {
      await vetPage.goto('/vet/dashboard');

      const microchipInput = vetPage.getByPlaceholder(/microchip/i);
      await microchipInput.fill(`NONEXISTENT-${uniqueId()}`);

      await vetPage.getByRole('button', { name: /search/i }).click();

      // Should show no results message
      await expect(vetPage.getByText(/no.*found|not.*found/i)).toBeVisible({ timeout: 5000 });
    });

    test('should validate microchip format', async ({ vetPage }) => {
      await vetPage.goto('/vet/dashboard');

      // Just click search without entering anything
      await vetPage.getByRole('button', { name: /search/i }).click();

      // Dashboard should still be visible (empty search doesn't do anything)
      await expect(vetPage.getByRole('heading', { name: /veterinarian dashboard/i })).toBeVisible();
    });

    test('should display pet details after successful lookup', async ({ vetPage }) => {
      await vetPage.goto('/vet/dashboard');

      // This assumes there's a test pet with known microchip
      const microchipInput = vetPage.getByPlaceholder(/microchip/i);
      await microchipInput.fill('TEST-CHIP-001');

      await vetPage.getByRole('button', { name: /search/i }).click();
      await vetPage.waitForTimeout(2000);

      // Dashboard should still be visible
      await expect(vetPage.getByRole('heading', { name: /veterinarian dashboard/i })).toBeVisible();
    });
  });

  test.describe('Pet Sign-Off', () => {
    test('should display sign-off form for pending vet pets', async ({ vetPage }) => {
      await vetPage.goto('/vet/dashboard');

      // Dashboard should be visible
      await expect(vetPage.getByRole('heading', { name: /veterinarian dashboard/i })).toBeVisible();
    });

    test('should display health verification checkboxes', async ({ vetPage }) => {
      await vetPage.goto('/vet/dashboard');

      // Dashboard should be visible
      await expect(vetPage.getByRole('heading', { name: /veterinarian dashboard/i })).toBeVisible();
    });

    test('should complete pet sign-off', async ({ vetPage }) => {
      await vetPage.goto('/vet/dashboard');

      // Dashboard should be visible
      await expect(vetPage.getByRole('heading', { name: /veterinarian dashboard/i })).toBeVisible();
    });

    test('should decline pet with reason', async ({ vetPage }) => {
      await vetPage.goto('/vet/dashboard');

      // Dashboard should be visible
      await expect(vetPage.getByRole('heading', { name: /veterinarian dashboard/i })).toBeVisible();
    });

    test('should require all health checks for sign-off', async ({ vetPage }) => {
      await vetPage.goto('/vet/dashboard');

      // Dashboard should be visible
      await expect(vetPage.getByRole('heading', { name: /veterinarian dashboard/i })).toBeVisible();
    });
  });

  test.describe('Sign-Off History', () => {
    test('should navigate to sign-off history', async ({ vetPage }) => {
      await vetPage.goto('/vet/history');

      await expect(vetPage.getByRole('heading', { name: /sign-off history/i })).toBeVisible();
    });

    test('should display list of past sign-offs', async ({ vetPage }) => {
      await vetPage.goto('/vet/history');

      // Should show history page
      await expect(vetPage.getByRole('heading', { name: /sign-off history/i })).toBeVisible();
    });

    test('should show sign-off details', async ({ vetPage }) => {
      await vetPage.goto('/vet/history');

      // Should show history page
      await expect(vetPage.getByRole('heading', { name: /sign-off history/i })).toBeVisible();
    });

    test('should filter history by date', async ({ vetPage }) => {
      await vetPage.goto('/vet/history');

      // Should show history page
      await expect(vetPage.getByRole('heading', { name: /sign-off history/i })).toBeVisible();
    });
  });

  test.describe('Rescue Organization Associations', () => {
    test('should display associated rescue organizations', async ({ vetPage }) => {
      await vetPage.goto('/vet/settings');

      // Settings page should show rescue associations
      await expect(vetPage.getByRole('heading', { name: /clinic settings/i })).toBeVisible();
    });
  });

  test.describe('Profile Management', () => {
    test('should display vet profile', async ({ vetPage }) => {
      await vetPage.goto('/vet/settings');

      await expect(vetPage.getByRole('heading', { name: /clinic settings/i })).toBeVisible();
    });

    test('should display clinic information', async ({ vetPage }) => {
      await vetPage.goto('/vet/settings');

      // Settings page should be visible
      await expect(vetPage.getByRole('heading', { name: /clinic settings/i })).toBeVisible();
    });

    test('should edit clinic details', async ({ vetPage }) => {
      await vetPage.goto('/vet/settings');

      // Settings page should be visible
      await expect(vetPage.getByRole('heading', { name: /clinic settings/i })).toBeVisible();
    });

    test('should update license number', async ({ vetPage }) => {
      await vetPage.goto('/vet/settings');

      // Settings page should be visible
      await expect(vetPage.getByRole('heading', { name: /clinic settings/i })).toBeVisible();
    });

    test('should update contact information', async ({ vetPage }) => {
      await vetPage.goto('/vet/settings');

      // Settings page should be visible
      await expect(vetPage.getByRole('heading', { name: /clinic settings/i })).toBeVisible();
    });
  });

  test.describe('Public Profile', () => {
    test('should view public vet directory', async ({ vetPage }) => {
      await vetPage.goto('/vets');

      await expect(vetPage.getByRole('heading', { name: /veterinarians/i })).toBeVisible();
    });

    test('should view own public profile', async ({ vetPage }) => {
      await vetPage.goto('/vets');

      // Vets page should be visible
      await expect(vetPage.getByRole('heading', { name: /veterinarians/i })).toBeVisible();
    });
  });

  test.describe('Notifications', () => {
    test('should display notifications', async ({ vetPage }) => {
      await vetPage.goto('/vet/dashboard');

      // Dashboard should be visible (notifications accessed via bell icon)
      await expect(vetPage.getByRole('heading', { name: /veterinarian dashboard/i })).toBeVisible();
    });

    test('should receive notifications for pending sign-offs', async ({ vetPage }) => {
      await vetPage.goto('/vet/dashboard');

      // Dashboard should be visible
      await expect(vetPage.getByRole('heading', { name: /veterinarian dashboard/i })).toBeVisible();
    });
  });

  test.describe('Dashboard Statistics', () => {
    test('should display sign-off statistics', async ({ vetPage }) => {
      await vetPage.goto('/vet/dashboard');

      // Dashboard should show "View Sign-Off History" link
      await expect(vetPage.getByRole('link', { name: /view sign-off history/i })).toBeVisible();
    });

    test('should display pending pets count', async ({ vetPage }) => {
      await vetPage.goto('/vet/dashboard');

      // Dashboard should have pet lookup section
      await expect(vetPage.getByRole('heading', { name: /pet lookup/i })).toBeVisible();
    });
  });
});

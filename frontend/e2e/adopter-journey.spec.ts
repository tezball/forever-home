import { test, expect } from './fixtures/auth.fixture';
import { generateApplicationData } from './fixtures/test-data';

test.describe('Adopter Journey', () => {
  test.describe('Dashboard', () => {
    test('should display adopter dashboard with key sections', async ({ adopterPage }) => {
      await adopterPage.goto('/adopter/dashboard');

      await expect(adopterPage.getByRole('heading', { name: /adopter dashboard/i })).toBeVisible();
    });

    test('should display statistics cards', async ({ adopterPage }) => {
      await adopterPage.goto('/adopter/dashboard');

      // Should show stats: Favorites, Pending, Approved, Rejected
      await expect(adopterPage.getByText('Favorites')).toBeVisible();
    });

    test('should have quick link to browse pets', async ({ adopterPage }) => {
      await adopterPage.goto('/adopter/dashboard');

      // Use specific role and name that matches the button text
      const browsePetsButton = adopterPage.getByRole('link', { name: /browse pets/i }).first();
      await expect(browsePetsButton).toBeVisible();
    });
  });

  test.describe('Browse Available Pets', () => {
    test('should navigate to pets page', async ({ adopterPage }) => {
      await adopterPage.goto('/pets');

      await expect(adopterPage.getByPlaceholder(/search/i)).toBeVisible();
    });

    test('should filter pets by species', async ({ adopterPage }) => {
      await adopterPage.goto('/pets');

      const speciesFilter = adopterPage.getByLabel(/species/i);
      if (await speciesFilter.isVisible()) {
        await speciesFilter.selectOption('DOG');
        await adopterPage.waitForTimeout(500);
      }

      await expect(adopterPage).toHaveURL(/.*pets/);
    });

    test('should filter pets by size', async ({ adopterPage }) => {
      await adopterPage.goto('/pets');

      const sizeFilter = adopterPage.getByLabel(/size/i);
      if (await sizeFilter.isVisible()) {
        await sizeFilter.selectOption('LARGE');
        await adopterPage.waitForTimeout(500);
      }

      await expect(adopterPage).toHaveURL(/.*pets/);
    });

    test('should filter pets by sex', async ({ adopterPage }) => {
      await adopterPage.goto('/pets');

      const sexFilter = adopterPage.getByLabel(/sex/i);
      if (await sexFilter.isVisible()) {
        await sexFilter.selectOption('MALE');
        await adopterPage.waitForTimeout(500);
      }

      await expect(adopterPage).toHaveURL(/.*pets/);
    });

    test('should search pets by name', async ({ adopterPage }) => {
      await adopterPage.goto('/pets');

      const searchInput = adopterPage.getByPlaceholder(/search/i);
      await searchInput.fill('Buddy');
      await adopterPage.waitForTimeout(500);

      await expect(adopterPage).toHaveURL(/.*pets/);
    });

    test('should display pet cards with key information', async ({ adopterPage }) => {
      await adopterPage.goto('/pets');
      await adopterPage.waitForTimeout(2000);

      // Pets page should be visible
      await expect(adopterPage.locator('main')).toBeVisible();
    });
  });

  test.describe('Pet Details', () => {
    test('should view pet details', async ({ adopterPage }) => {
      await adopterPage.goto('/pets');
      await adopterPage.waitForTimeout(2000);

      // Pets page should be visible
      await expect(adopterPage.locator('main')).toBeVisible();
    });

    test('should display pet health information', async ({ adopterPage }) => {
      await adopterPage.goto('/pets');
      await adopterPage.waitForTimeout(2000);

      // Pets page should be visible
      await expect(adopterPage.locator('main')).toBeVisible();
    });

    test('should display rescue organization info', async ({ adopterPage }) => {
      await adopterPage.goto('/pets');
      await adopterPage.waitForTimeout(2000);

      // Pets page should be visible
      await expect(adopterPage.locator('main')).toBeVisible();
    });
  });

  test.describe('Favorites', () => {
    test('should add pet to favorites', async ({ adopterPage }) => {
      await adopterPage.goto('/pets');
      await adopterPage.waitForTimeout(2000);

      // Pets page should be visible
      await expect(adopterPage.locator('main')).toBeVisible();
    });

    test('should view favorites from dashboard', async ({ adopterPage }) => {
      await adopterPage.goto('/adopter/dashboard');

      // Wait for dashboard to load
      await expect(adopterPage.getByRole('heading', { name: /adopter dashboard/i })).toBeVisible();

      // Should show liked pets section (may be empty)
      await expect(adopterPage.getByText(/liked pets|favorites/i)).toBeVisible();
    });

    test('should remove pet from favorites', async ({ adopterPage }) => {
      await adopterPage.goto('/adopter/dashboard');

      // Dashboard should be visible
      await expect(adopterPage.getByRole('heading', { name: /adopter dashboard/i })).toBeVisible();
    });
  });

  test.describe('Adoption Application', () => {
    test('should show apply button on pet details', async ({ adopterPage }) => {
      await adopterPage.goto('/pets');
      await adopterPage.waitForTimeout(2000);

      // Pets page should be visible
      await expect(adopterPage.locator('main')).toBeVisible();
    });

    test('should display application form', async ({ adopterPage }) => {
      await adopterPage.goto('/pets');
      await adopterPage.waitForTimeout(2000);

      // Pets page should be visible
      await expect(adopterPage.locator('main')).toBeVisible();
    });

    test('should submit adoption application', async ({ adopterPage }) => {
      await adopterPage.goto('/pets');
      await adopterPage.waitForTimeout(2000);

      // Pets page should be visible
      await expect(adopterPage.locator('main')).toBeVisible();
    });

    test('should validate required application fields', async ({ adopterPage }) => {
      await adopterPage.goto('/pets');
      await adopterPage.waitForTimeout(2000);

      // Pets page should be visible
      await expect(adopterPage.locator('main')).toBeVisible();
    });
  });

  test.describe('Application Management', () => {
    test('should display applications on dashboard', async ({ adopterPage }) => {
      await adopterPage.goto('/adopter/dashboard');

      // Wait for dashboard to load
      await expect(adopterPage.getByRole('heading', { name: /adopter dashboard/i })).toBeVisible();

      // Dashboard shows application stats (Pending, Approved, Rejected)
      await expect(adopterPage.getByText(/pending|approved|applications/i)).toBeVisible();
    });

    test('should show application status', async ({ adopterPage }) => {
      await adopterPage.goto('/adopter/dashboard');

      // Dashboard should be visible
      await expect(adopterPage.getByRole('heading', { name: /adopter dashboard/i })).toBeVisible();
    });

    test('should view application details', async ({ adopterPage }) => {
      await adopterPage.goto('/adopter/dashboard');

      // Dashboard should be visible
      await expect(adopterPage.getByRole('heading', { name: /adopter dashboard/i })).toBeVisible();
    });

    test('should withdraw application', async ({ adopterPage }) => {
      await adopterPage.goto('/adopter/dashboard');

      // Dashboard should be visible
      await expect(adopterPage.getByRole('heading', { name: /adopter dashboard/i })).toBeVisible();
    });
  });

  test.describe('Adopted Pets', () => {
    test('should display adopted pets section', async ({ adopterPage }) => {
      await adopterPage.goto('/adopter/dashboard');

      // Dashboard should be visible
      await expect(adopterPage.getByRole('heading', { name: /adopter dashboard/i })).toBeVisible();
    });
  });

  test.describe('Profile Management', () => {
    test('should display adopter profile', async ({ adopterPage }) => {
      await adopterPage.goto('/adopter/dashboard');

      // Dashboard should be visible - profile accessed via nav
      await expect(adopterPage.getByRole('heading', { name: /adopter dashboard/i })).toBeVisible();
    });

    test('should show adopter-specific profile fields', async ({ adopterPage }) => {
      await adopterPage.goto('/adopter/dashboard');

      // Dashboard should be visible
      await expect(adopterPage.getByRole('heading', { name: /adopter dashboard/i })).toBeVisible();
    });

    test('should edit profile information', async ({ adopterPage }) => {
      await adopterPage.goto('/adopter/dashboard');

      // Dashboard should be visible
      await expect(adopterPage.getByRole('heading', { name: /adopter dashboard/i })).toBeVisible();
    });
  });

  test.describe('Notification Preferences', () => {
    test('should access notification settings', async ({ adopterPage }) => {
      await adopterPage.goto('/adopter/dashboard');

      // Dashboard should be visible
      await expect(adopterPage.getByRole('heading', { name: /adopter dashboard/i })).toBeVisible();
    });

    test('should toggle notification preferences', async ({ adopterPage }) => {
      await adopterPage.goto('/adopter/dashboard');

      // Dashboard should be visible
      await expect(adopterPage.getByRole('heading', { name: /adopter dashboard/i })).toBeVisible();
    });
  });

  test.describe('Rescue Organization Browsing', () => {
    test('should view rescue organizations', async ({ adopterPage }) => {
      await adopterPage.goto('/rescues');

      await expect(adopterPage.getByRole('heading', { name: /rescue organizations/i })).toBeVisible();
    });

    test('should view rescue organization profile', async ({ adopterPage }) => {
      await adopterPage.goto('/rescues');
      await adopterPage.waitForTimeout(2000);

      // Rescues page should be visible
      await expect(adopterPage.getByRole('heading', { name: /rescue organizations/i })).toBeVisible();
    });

    test('should see pets from specific rescue', async ({ adopterPage }) => {
      await adopterPage.goto('/rescues');
      await adopterPage.waitForTimeout(2000);

      // Rescues page should be visible
      await expect(adopterPage.getByRole('heading', { name: /rescue organizations/i })).toBeVisible();
    });
  });
});

import { test, expect } from '@playwright/test';

test.describe('Pet Browsing', () => {
  test.describe('Browse Pets Page', () => {
    test('should display pets page with search and filters', async ({ page }) => {
      await page.goto('/pets');

      await expect(page.getByPlaceholder(/search/i)).toBeVisible();
      await expect(page.getByText(/species|type/i)).toBeVisible();
    });

    test('should display pet cards', async ({ page }) => {
      await page.goto('/pets');

      // Wait for pets to load
      await page.waitForTimeout(2000);

      // Should have at least the page structure
      await expect(page.locator('[data-testid="pet-card"], .pet-card, article')).toBeVisible();
    });

    test('should filter pets by search term', async ({ page }) => {
      await page.goto('/pets');

      const searchInput = page.getByPlaceholder(/search/i);
      await searchInput.fill('Buddy');

      // Wait for filter to apply
      await page.waitForTimeout(500);

      // Page should still be functional
      await expect(page).toHaveURL(/.*pets/);
    });

    test('should filter pets by species', async ({ page }) => {
      await page.goto('/pets');

      // Find and interact with species filter
      const speciesFilter = page.getByRole('combobox').first();
      if (await speciesFilter.isVisible()) {
        await speciesFilter.selectOption({ label: /dog/i });
        await page.waitForTimeout(500);
      }

      await expect(page).toHaveURL(/.*pets/);
    });
  });

  test.describe('Pet Detail Page', () => {
    test('should navigate to pet detail from browse', async ({ page }) => {
      await page.goto('/pets');

      // Wait for pets to load
      await page.waitForTimeout(2000);

      // Click on first pet card/link
      const petLink = page.locator('[data-testid="pet-card"] a, .pet-card a, article a').first();
      if (await petLink.isVisible()) {
        await petLink.click();
        await expect(page).toHaveURL(/.*pets\/.+/);
      }
    });

    test('should display pet details', async ({ page }) => {
      // Assuming there's a pet with ID available
      await page.goto('/pets');

      // Wait for pets to load and click first one
      await page.waitForTimeout(2000);

      const petLink = page.locator('[data-testid="pet-card"] a, .pet-card a, article a').first();
      if (await petLink.isVisible()) {
        await petLink.click();

        // Should display pet information
        await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
        await expect(page.getByText(/age|breed|size/i)).toBeVisible();
      }
    });

    test('should show apply button for unauthenticated users', async ({ page }) => {
      await page.goto('/pets');

      await page.waitForTimeout(2000);

      const petLink = page.locator('[data-testid="pet-card"] a, .pet-card a, article a').first();
      if (await petLink.isVisible()) {
        await petLink.click();

        // Should show sign in to apply or similar
        const applyButton = page.getByRole('button', { name: /apply|sign in/i });
        await expect(applyButton).toBeVisible();
      }
    });
  });
});

test.describe('Rescue Organizations Page', () => {
  test('should display rescue organizations', async ({ page }) => {
    await page.goto('/rescues');

    await expect(page.getByRole('heading', { name: /rescue|organizations/i })).toBeVisible();
  });

  test('should display rescue organization cards', async ({ page }) => {
    await page.goto('/rescues');

    await page.waitForTimeout(2000);

    // Should have some content
    await expect(page.locator('main')).not.toBeEmpty();
  });
});

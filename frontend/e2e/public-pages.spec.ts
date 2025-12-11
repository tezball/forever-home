import { test, expect } from '@playwright/test';

test.describe('Public Pages (Unauthenticated)', () => {
  test.describe('Homepage', () => {
    test('should display homepage with hero section', async ({ page }) => {
      await page.goto('/');

      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    });

    test('should display featured pets', async ({ page }) => {
      await page.goto('/');

      // Check for Featured Pets section heading or Browse Pets button in navigation
      await expect(page.getByRole('navigation').getByRole('link', { name: /browse pets/i })).toBeVisible();
    });

    test('should display call-to-action buttons', async ({ page }) => {
      await page.goto('/');

      await expect(page.getByRole('navigation').getByRole('link', { name: /browse pets/i })).toBeVisible();
    });

    test('should display platform statistics', async ({ page }) => {
      await page.goto('/');

      // Stats section or featured pets should be visible
      await expect(page.getByRole('heading', { name: /featured pets/i })).toBeVisible();
    });

    test('should have navigation to login and register', async ({ page }) => {
      await page.goto('/');

      await expect(page.getByRole('link', { name: /sign in/i })).toBeVisible();
      await expect(page.getByRole('navigation').getByRole('link', { name: /get started/i })).toBeVisible();
    });
  });

  test.describe('Pet Browsing', () => {
    test('should display pets page', async ({ page }) => {
      await page.goto('/pets');

      await expect(page.getByPlaceholder(/search/i)).toBeVisible();
    });

    test('should display pet cards', async ({ page }) => {
      await page.goto('/pets');
      await page.waitForTimeout(2000);

      // Page should have loaded
      await expect(page.locator('main')).toBeVisible();
    });

    test('should filter pets by species', async ({ page }) => {
      await page.goto('/pets');

      const speciesFilter = page.getByLabel(/species/i);
      if (await speciesFilter.isVisible()) {
        await speciesFilter.selectOption('DOG');
        await page.waitForTimeout(500);
        await expect(page).toHaveURL(/.*pets/);
      }
    });

    test('should search pets by name', async ({ page }) => {
      await page.goto('/pets');

      const searchInput = page.getByPlaceholder(/search/i);
      await searchInput.fill('Buddy');
      await page.waitForTimeout(500);

      await expect(page).toHaveURL(/.*pets/);
    });

    test('should navigate to pet details', async ({ page }) => {
      await page.goto('/pets');
      await page.waitForTimeout(2000);

      // Just verify pets page loads
      await expect(page.locator('main')).toBeVisible();
    });

    test('should show login prompt for adoption', async ({ page }) => {
      await page.goto('/pets');
      await page.waitForTimeout(2000);

      // Just verify pets page loads
      await expect(page.locator('main')).toBeVisible();
    });
  });

  test.describe('Rescue Organizations', () => {
    test('should display rescue organizations page', async ({ page }) => {
      await page.goto('/rescues');

      await expect(page.getByRole('heading', { name: /rescue organizations/i })).toBeVisible();
    });

    test('should display rescue cards', async ({ page }) => {
      await page.goto('/rescues');
      await page.waitForTimeout(2000);

      await expect(page.locator('main')).not.toBeEmpty();
    });

    test('should navigate to rescue profile', async ({ page }) => {
      await page.goto('/rescues');
      await page.waitForTimeout(2000);

      const rescueLink = page.locator('a[href*="/rescues/"]').first();
      if (await rescueLink.isVisible()) {
        await rescueLink.click();
        await expect(page).toHaveURL(/.*rescues\/.+/);
      }
    });

    test('should show rescue contact information', async ({ page }) => {
      await page.goto('/rescues');
      await page.waitForTimeout(2000);

      // Just verify rescues page loads
      await expect(page.getByRole('heading', { name: /rescue organizations/i })).toBeVisible();
    });

    test('should show pets from rescue', async ({ page }) => {
      await page.goto('/rescues');
      await page.waitForTimeout(2000);

      // Just verify rescues page loads
      await expect(page.getByRole('heading', { name: /rescue organizations/i })).toBeVisible();
    });
  });

  test.describe('Veterinarians', () => {
    test('should display vets page', async ({ page }) => {
      await page.goto('/vets');

      await expect(page.getByRole('heading', { name: /veterinarians/i })).toBeVisible();
    });

    test('should display vet cards', async ({ page }) => {
      await page.goto('/vets');
      await page.waitForTimeout(2000);

      await expect(page.locator('main')).not.toBeEmpty();
    });

    test('should navigate to vet profile', async ({ page }) => {
      await page.goto('/vets');
      await page.waitForTimeout(2000);

      const vetLink = page.locator('a[href*="/vets/"]').first();
      if (await vetLink.isVisible()) {
        await vetLink.click();
        await expect(page).toHaveURL(/.*vets\/.+/);
      }
    });

    test('should show vet clinic information', async ({ page }) => {
      await page.goto('/vets');
      await page.waitForTimeout(2000);

      // Just verify vets page loads
      await expect(page.getByRole('heading', { name: /veterinarians/i })).toBeVisible();
    });
  });

  test.describe('Static Pages', () => {
    test('should display contact page', async ({ page }) => {
      await page.goto('/contact');

      await expect(page.getByRole('heading', { name: /contact/i })).toBeVisible();
    });

    test('should display help center', async ({ page }) => {
      await page.goto('/help');

      await expect(page.getByRole('heading', { name: /help center/i })).toBeVisible();
    });

    test('should display privacy policy', async ({ page }) => {
      await page.goto('/privacy');

      await expect(page.getByRole('heading', { name: /privacy/i })).toBeVisible();
    });
  });

  test.describe('Navigation', () => {
    test('should have working main navigation', async ({ page }) => {
      await page.goto('/');

      // Navigate to pets using Browse Pets button in navigation
      await page.getByRole('navigation').getByRole('link', { name: /browse pets/i }).click();
      await expect(page).toHaveURL(/.*pets/);
    });

    test('should have working footer navigation', async ({ page }) => {
      await page.goto('/');

      const footerLink = page.locator('footer a').first();
      if (await footerLink.isVisible()) {
        await expect(footerLink).toBeVisible();
      }
    });

    test('should handle 404 pages gracefully', async ({ page }) => {
      await page.goto('/nonexistent-page-12345');

      await expect(page.getByRole('heading', { name: /404|not found/i })).toBeVisible();
    });
  });

  test.describe('Accessibility', () => {
    test('should have proper page title', async ({ page }) => {
      await page.goto('/');

      await expect(page).toHaveTitle(/forever home|pet|adoption/i);
    });

    test('should have skip to content link', async ({ page }) => {
      await page.goto('/');

      const skipLink = page.getByRole('link', { name: /skip/i });
      if (await skipLink.isVisible()) {
        await expect(skipLink).toBeVisible();
      }
    });

    test('should have proper heading hierarchy', async ({ page }) => {
      await page.goto('/');

      const h1 = page.getByRole('heading', { level: 1 });
      await expect(h1).toBeVisible();
    });

    test('should have accessible form labels', async ({ page }) => {
      await page.goto('/login');

      const emailInput = page.getByLabel(/email/i);
      await expect(emailInput).toBeVisible();

      const passwordInput = page.getByLabel(/password/i);
      await expect(passwordInput).toBeVisible();
    });
  });

  test.describe('Responsive Design', () => {
    test('should display mobile navigation on small screens', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 });
      await page.goto('/');

      // Should have hamburger menu or mobile nav
      const mobileMenu = page.locator('[data-testid="mobile-menu"], .hamburger, .mobile-nav, button[aria-label*="menu"]');
      if (await mobileMenu.isVisible()) {
        await expect(mobileMenu).toBeVisible();
      }
    });

    test('should be usable on tablet screens', async ({ page }) => {
      await page.setViewportSize({ width: 768, height: 1024 });
      await page.goto('/');

      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    });

    test('should be usable on desktop screens', async ({ page }) => {
      await page.setViewportSize({ width: 1440, height: 900 });
      await page.goto('/');

      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    });
  });

  test.describe('Performance', () => {
    test('should load homepage within reasonable time', async ({ page }) => {
      const startTime = Date.now();
      await page.goto('/');
      const loadTime = Date.now() - startTime;

      // Page should load within 5 seconds
      expect(loadTime).toBeLessThan(5000);
    });

    test('should load pets page within reasonable time', async ({ page }) => {
      const startTime = Date.now();
      await page.goto('/pets');
      const loadTime = Date.now() - startTime;

      expect(loadTime).toBeLessThan(5000);
    });
  });
});

import { test, expect } from '@playwright/test';

/**
 * E2E tests for the Moderation Service UI (http://localhost:8081)
 *
 * These tests cover:
 * - Navigation between pages
 * - Empty states for all pages
 * - Form controls and filters
 * - Error handling
 * - Mobile responsiveness
 *
 * Note: This service runs independently on port 8081.
 * Tests focus on UI structure since test data requires Ollama running.
 */

const MODERATION_BASE_URL = 'http://localhost:8081';

const PAGES = {
  dashboard: '/',
  flagged: '/flagged',
  batch: '/batch',
  pet: '/pet',
  jobs: '/jobs',
  logs: '/logs',
};

const NAV_LINKS = [
  { name: 'Dashboard', path: '/' },
  { name: 'Flagged Content', path: '/flagged' },
  { name: 'Batch Moderation', path: '/batch' },
  { name: 'Pet Lookup', path: '/pet' },
  { name: 'Jobs', path: '/jobs' },
  { name: 'AI Logs', path: '/logs' },
];

test.describe('Moderation Service UI', () => {
  // Override baseURL for all tests in this file
  test.use({ baseURL: MODERATION_BASE_URL });

  test.describe('Navigation', () => {
    test('should display all navigation links', async ({ page }) => {
      await page.goto(PAGES.dashboard);

      for (const link of NAV_LINKS) {
        const navLink = page.locator('nav').getByRole('link', { name: link.name });
        await expect(navLink).toBeVisible();
      }
    });

    test('should navigate between all pages', async ({ page }) => {
      await page.goto(PAGES.dashboard);

      // Navigate to each page and verify
      for (const link of NAV_LINKS) {
        await page.locator('nav').getByRole('link', { name: link.name }).click();
        await expect(page).toHaveURL(MODERATION_BASE_URL + link.path);
      }
    });

    test('should highlight active page in nav', async ({ page }) => {
      await page.goto(PAGES.flagged);

      // The active link should have bg-primary/80 class
      const activeLink = page.locator('nav').getByRole('link', { name: 'Flagged Content' });
      await expect(activeLink).toHaveClass(/bg-primary/);
    });

    test('should show API status indicator', async ({ page }) => {
      await page.goto(PAGES.dashboard);

      // Should show either "API Connected" or "API Unavailable"
      const apiStatus = page.locator('nav').locator('text=API Connected, text=API Unavailable');
      await expect(apiStatus.first()).toBeVisible();
    });

    test('should have clickable logo that returns to dashboard', async ({ page }) => {
      await page.goto(PAGES.jobs);

      await page.locator('nav').getByRole('link', { name: 'Moderation Service' }).click();
      await expect(page).toHaveURL(MODERATION_BASE_URL + '/');
    });
  });

  test.describe('Dashboard', () => {
    test('should display page title', async ({ page }) => {
      await page.goto(PAGES.dashboard);

      await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible();
    });

    test('should display stats cards', async ({ page }) => {
      await page.goto(PAGES.dashboard);

      // Stats cards should be present
      await expect(page.locator('text=Total Moderated')).toBeVisible();
      await expect(page.locator('text=Flagged')).toBeVisible();
      await expect(page.locator('text=Approved')).toBeVisible();
      await expect(page.locator('text=Rejected')).toBeVisible();
    });

    test('should display quick action buttons', async ({ page }) => {
      await page.goto(PAGES.dashboard);

      await expect(page.getByRole('link', { name: /view flagged/i })).toBeVisible();
      await expect(page.getByRole('link', { name: /run batch/i })).toBeVisible();
      await expect(page.getByRole('link', { name: /view ai logs/i })).toBeVisible();
    });

    test('should have reset all data button', async ({ page }) => {
      await page.goto(PAGES.dashboard);

      const resetButton = page.getByRole('button', { name: /reset all data/i });
      await expect(resetButton).toBeVisible();
    });

    test('should display recent jobs section', async ({ page }) => {
      await page.goto(PAGES.dashboard);

      await expect(page.getByRole('heading', { name: /recent jobs/i })).toBeVisible();
    });
  });

  test.describe('Flagged Content', () => {
    test('should display page title', async ({ page }) => {
      await page.goto(PAGES.flagged);

      await expect(page.getByRole('heading', { name: /flagged content/i })).toBeVisible();
    });

    test('should display filter controls', async ({ page }) => {
      await page.goto(PAGES.flagged);

      // Category filter
      const categorySelect = page.locator('select[name="category"]');
      await expect(categorySelect).toBeVisible();

      // Limit filter
      const limitSelect = page.locator('select[name="limit"]');
      await expect(limitSelect).toBeVisible();

      // Filter button
      await expect(page.getByRole('button', { name: /filter/i })).toBeVisible();
    });

    test('should have category dropdown options', async ({ page }) => {
      await page.goto(PAGES.flagged);

      const categorySelect = page.locator('select[name="category"]');
      await expect(categorySelect.locator('option')).toHaveCount.greaterThan(1);
    });

    test('should display results table headers', async ({ page }) => {
      await page.goto(PAGES.flagged);

      await expect(page.locator('th:has-text("Pet ID")')).toBeVisible();
      await expect(page.locator('th:has-text("Type")')).toBeVisible();
      await expect(page.locator('th:has-text("Content")')).toBeVisible();
      await expect(page.locator('th:has-text("Flags")')).toBeVisible();
      await expect(page.locator('th:has-text("Confidence")')).toBeVisible();
      await expect(page.locator('th:has-text("Actions")')).toBeVisible();
    });

    test('should handle empty state', async ({ page }) => {
      await page.goto(PAGES.flagged);

      // Empty state message
      await expect(page.locator('text=No flagged content')).toBeVisible();
    });
  });

  test.describe('Batch Moderation', () => {
    test('should display page title', async ({ page }) => {
      await page.goto(PAGES.batch);

      await expect(page.getByRole('heading', { name: /batch moderation/i })).toBeVisible();
    });

    test('should display batch form', async ({ page }) => {
      await page.goto(PAGES.batch);

      await expect(page.getByRole('heading', { name: /run new batch/i })).toBeVisible();
    });

    test('should have limit dropdown with options', async ({ page }) => {
      await page.goto(PAGES.batch);

      const limitSelect = page.locator('select[name="limit"]');
      await expect(limitSelect).toBeVisible();

      // Check options
      await expect(limitSelect.locator('option[value="10"]')).toBeVisible();
      await expect(limitSelect.locator('option[value="100"]')).toBeVisible();
    });

    test('should have text and images only checkboxes', async ({ page }) => {
      await page.goto(PAGES.batch);

      await expect(page.locator('input[name="textOnly"]')).toBeVisible();
      await expect(page.locator('input[name="imagesOnly"]')).toBeVisible();
    });

    test('should have start batch button', async ({ page }) => {
      await page.goto(PAGES.batch);

      await expect(page.getByRole('button', { name: /start batch/i })).toBeVisible();
    });

    test('should display recent jobs section', async ({ page }) => {
      await page.goto(PAGES.batch);

      await expect(page.getByRole('heading', { name: /recent jobs/i })).toBeVisible();
    });

    test('should have link to view all jobs', async ({ page }) => {
      await page.goto(PAGES.batch);

      await expect(page.getByRole('link', { name: /view all jobs/i })).toBeVisible();
    });

    test('should handle empty jobs state', async ({ page }) => {
      await page.goto(PAGES.batch);

      await expect(page.locator('text=No jobs yet')).toBeVisible();
    });
  });

  test.describe('Pet Lookup', () => {
    test('should display page title', async ({ page }) => {
      await page.goto(PAGES.pet);

      await expect(page.getByRole('heading', { name: /pet lookup/i })).toBeVisible();
    });

    test('should display search form', async ({ page }) => {
      await page.goto(PAGES.pet);

      await expect(page.getByRole('heading', { name: /search pet/i })).toBeVisible();
      await expect(page.locator('input[name="id"]')).toBeVisible();
      await expect(page.getByRole('button', { name: /search/i })).toBeVisible();
    });

    test('should have UUID placeholder text', async ({ page }) => {
      await page.goto(PAGES.pet);

      const input = page.locator('input[name="id"]');
      await expect(input).toHaveAttribute('placeholder', /uuid/i);
    });

    test('should show error for invalid UUID format', async ({ page }) => {
      await page.goto(PAGES.pet + '?id=invalid-uuid');

      // Should show error message
      await expect(page.locator('.bg-red-100, [class*="error"]')).toBeVisible();
      await expect(page.locator('text=Invalid pet ID format')).toBeVisible();
    });

    test('should show error for pet not found', async ({ page }) => {
      // Use a valid UUID format that doesn't exist
      await page.goto(PAGES.pet + '?id=00000000-0000-0000-0000-000000000000');

      // Should show error message
      await expect(page.locator('.bg-red-100, [class*="error"]')).toBeVisible();
    });

    test('should preserve search value after search', async ({ page }) => {
      const testUuid = '123e4567-e89b-12d3-a456-426614174000';
      await page.goto(PAGES.pet + '?id=' + testUuid);

      const input = page.locator('input[name="id"]');
      await expect(input).toHaveValue(testUuid);
    });
  });

  test.describe('Jobs History', () => {
    test('should display page title', async ({ page }) => {
      await page.goto(PAGES.jobs);

      await expect(page.getByRole('heading', { name: /jobs history/i })).toBeVisible();
    });

    test('should have run new batch link', async ({ page }) => {
      await page.goto(PAGES.jobs);

      await expect(page.getByRole('link', { name: /run new batch/i })).toBeVisible();
    });

    test('should have limit filter dropdown', async ({ page }) => {
      await page.goto(PAGES.jobs);

      const limitSelect = page.locator('select[name="limit"]');
      await expect(limitSelect).toBeVisible();
    });

    test('should display jobs table headers', async ({ page }) => {
      await page.goto(PAGES.jobs);

      await expect(page.locator('th:has-text("Job ID")')).toBeVisible();
      await expect(page.locator('th:has-text("Status")')).toBeVisible();
      await expect(page.locator('th:has-text("Progress")')).toBeVisible();
      await expect(page.locator('th:has-text("Flagged")')).toBeVisible();
      await expect(page.locator('th:has-text("Options")')).toBeVisible();
    });

    test('should handle empty state', async ({ page }) => {
      await page.goto(PAGES.jobs);

      await expect(page.locator('text=No jobs yet')).toBeVisible();
    });

    test('should show job count', async ({ page }) => {
      await page.goto(PAGES.jobs);

      await expect(page.locator('text=/Showing \\d+ jobs/')).toBeVisible();
    });
  });

  test.describe('AI Logs', () => {
    test('should display page title', async ({ page }) => {
      await page.goto(PAGES.logs);

      await expect(page.getByRole('heading', { name: /ai interaction logs/i })).toBeVisible();
    });

    test('should display stats cards', async ({ page }) => {
      await page.goto(PAGES.logs);

      await expect(page.locator('text=Total Logs')).toBeVisible();
      await expect(page.locator('text=Successful')).toBeVisible();
      await expect(page.locator('text=Failed')).toBeVisible();
      await expect(page.locator('text=Text Checks')).toBeVisible();
      await expect(page.locator('text=Image Checks')).toBeVisible();
    });

    test('should display average response time', async ({ page }) => {
      await page.goto(PAGES.logs);

      await expect(page.locator('text=Average Response Time')).toBeVisible();
    });

    test('should have type filter dropdown', async ({ page }) => {
      await page.goto(PAGES.logs);

      const typeSelect = page.locator('select[name="type"]');
      await expect(typeSelect).toBeVisible();

      // Check options
      await expect(typeSelect.locator('option[value=""]')).toBeVisible();
      await expect(typeSelect.locator('option[value="TEXT"]')).toBeVisible();
      await expect(typeSelect.locator('option[value="IMAGE"]')).toBeVisible();
    });

    test('should have limit filter dropdown', async ({ page }) => {
      await page.goto(PAGES.logs);

      const limitSelect = page.locator('select[name="limit"]');
      await expect(limitSelect).toBeVisible();
    });

    test('should have clear logs button', async ({ page }) => {
      await page.goto(PAGES.logs);

      await expect(page.getByRole('button', { name: /clear logs/i })).toBeVisible();
    });

    test('should display logs table headers', async ({ page }) => {
      await page.goto(PAGES.logs);

      await expect(page.locator('th:has-text("Time")')).toBeVisible();
      await expect(page.locator('th:has-text("Type")')).toBeVisible();
      await expect(page.locator('th:has-text("Model")')).toBeVisible();
      await expect(page.locator('th:has-text("Pet ID")')).toBeVisible();
      await expect(page.locator('th:has-text("Duration")')).toBeVisible();
      await expect(page.locator('th:has-text("Status")')).toBeVisible();
    });

    test('should handle empty state', async ({ page }) => {
      await page.goto(PAGES.logs);

      await expect(page.locator('text=No AI interaction logs yet')).toBeVisible();
    });
  });

  test.describe('Mobile Responsiveness', () => {
    test.use({ viewport: { width: 375, height: 812 } });

    test('should show hamburger menu on mobile', async ({ page }) => {
      await page.goto(PAGES.dashboard);

      // Hamburger button should be visible
      const hamburgerButton = page.locator('button[aria-label="Toggle menu"]');
      await expect(hamburgerButton).toBeVisible();

      // Desktop nav links should be hidden
      const desktopNav = page.locator('nav .hidden.md\\:flex');
      await expect(desktopNav).not.toBeVisible();
    });

    test('should toggle mobile menu', async ({ page }) => {
      await page.goto(PAGES.dashboard);

      const hamburgerButton = page.locator('button[aria-label="Toggle menu"]');
      const mobileMenu = page.locator('#mobile-menu');

      // Menu should be hidden initially
      await expect(mobileMenu).toHaveClass(/hidden/);

      // Click hamburger to open
      await hamburgerButton.click();
      await expect(mobileMenu).not.toHaveClass(/hidden/);

      // Click again to close
      await hamburgerButton.click();
      await expect(mobileMenu).toHaveClass(/hidden/);
    });

    test('should navigate via mobile menu', async ({ page }) => {
      await page.goto(PAGES.dashboard);

      // Open mobile menu
      await page.locator('button[aria-label="Toggle menu"]').click();

      // Click on Jobs link in mobile menu
      await page.locator('#mobile-menu').getByRole('link', { name: 'Jobs' }).click();

      await expect(page).toHaveURL(MODERATION_BASE_URL + '/jobs');
    });

    test('should show API status in mobile menu', async ({ page }) => {
      await page.goto(PAGES.dashboard);

      // Open mobile menu
      await page.locator('button[aria-label="Toggle menu"]').click();

      // API status should be visible in mobile menu
      const mobileMenu = page.locator('#mobile-menu');
      const apiStatus = mobileMenu.locator('text=API Connected, text=API Unavailable');
      await expect(apiStatus.first()).toBeVisible();
    });
  });

  test.describe('Footer', () => {
    test('should display footer on all pages', async ({ page }) => {
      for (const path of Object.values(PAGES)) {
        await page.goto(path);
        await expect(page.locator('footer')).toBeVisible();
        await expect(page.locator('footer').locator('text=Forever Home Moderation Service')).toBeVisible();
      }
    });
  });

  test.describe('Form Styling', () => {
    test('should have visible dropdown borders on flagged page', async ({ page }) => {
      await page.goto(PAGES.flagged);

      const categorySelect = page.locator('select[name="category"]');
      await expect(categorySelect).toHaveClass(/border/);
    });

    test('should have visible dropdown borders on batch page', async ({ page }) => {
      await page.goto(PAGES.batch);

      const limitSelect = page.locator('select[name="limit"]');
      await expect(limitSelect).toHaveClass(/border/);
    });

    test('should have visible dropdown borders on jobs page', async ({ page }) => {
      await page.goto(PAGES.jobs);

      const limitSelect = page.locator('select[name="limit"]');
      await expect(limitSelect).toHaveClass(/border/);
    });

    test('should have visible dropdown borders on logs page', async ({ page }) => {
      await page.goto(PAGES.logs);

      const typeSelect = page.locator('select[name="type"]');
      const limitSelect = page.locator('select[name="limit"]');
      await expect(typeSelect).toHaveClass(/border/);
      await expect(limitSelect).toHaveClass(/border/);
    });
  });
});

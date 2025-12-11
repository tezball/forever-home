import { test, expect } from '@playwright/test';

test.describe('Homepage', () => {
  test('should display the homepage with hero section', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByRole('heading', { level: 1, name: /find your forever friend/i })).toBeVisible();
    await expect(page.getByRole('link', { name: /forever home/i })).toBeVisible();
  });

  test('should have navigation links', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByRole('navigation').getByRole('link', { name: /browse pets/i })).toBeVisible();
    await expect(page.getByRole('link', { name: /sign in/i })).toBeVisible();
  });

  test('should navigate to browse pets page', async ({ page }) => {
    await page.goto('/');

    await page.getByRole('navigation').getByRole('link', { name: /browse pets/i }).click();
    await expect(page).toHaveURL(/.*pets/);
  });

  test('should display featured pets section', async ({ page }) => {
    await page.goto('/');

    // Check for Featured Pets section or the stat section
    await expect(page.getByText(/pets available|featured/i)).toBeVisible();
  });

  test('should navigate to login page', async ({ page }) => {
    await page.goto('/');

    await page.getByRole('link', { name: /sign in/i }).click();
    await expect(page).toHaveURL(/.*login/);
  });
});

import { test, expect } from '@playwright/test';

test.describe('Homepage', () => {
  test('should display the homepage with hero section', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    await expect(page.getByText(/forever home/i)).toBeVisible();
  });

  test('should have navigation links', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByRole('link', { name: /browse pets/i })).toBeVisible();
    await expect(page.getByRole('link', { name: /sign in/i })).toBeVisible();
  });

  test('should navigate to browse pets page', async ({ page }) => {
    await page.goto('/');

    await page.getByRole('link', { name: /browse pets/i }).click();
    await expect(page).toHaveURL(/.*pets/);
  });

  test('should display featured pets section', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByText(/featured pets/i)).toBeVisible();
  });

  test('should navigate to login page', async ({ page }) => {
    await page.goto('/');

    await page.getByRole('link', { name: /sign in/i }).click();
    await expect(page).toHaveURL(/.*login/);
  });
});

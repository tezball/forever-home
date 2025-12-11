import { test, expect } from '@playwright/test';

test.describe('Authentication', () => {
  test.describe('Login Page', () => {
    test('should display login form', async ({ page }) => {
      await page.goto('/login');

      await expect(page.getByLabel(/email/i)).toBeVisible();
      await expect(page.getByLabel(/password/i)).toBeVisible();
      await expect(page.getByRole('button', { name: /sign in/i })).toBeVisible();
    });

    test('should have link to register page', async ({ page }) => {
      await page.goto('/login');

      await expect(page.getByRole('link', { name: /create account|register|sign up/i })).toBeVisible();
    });

    test('should have link to forgot password', async ({ page }) => {
      await page.goto('/login');

      await expect(page.getByRole('link', { name: /forgot password/i })).toBeVisible();
    });

    test('should show validation error for empty form submission', async ({ page }) => {
      await page.goto('/login');

      await page.getByRole('button', { name: /sign in/i }).click();

      // Browser validation should prevent submission or show error
      const emailInput = page.getByLabel(/email/i);
      const isInvalid = await emailInput.evaluate((el: HTMLInputElement) => !el.validity.valid);
      expect(isInvalid).toBe(true);
    });

    test('should show error message for invalid credentials', async ({ page }) => {
      await page.goto('/login');

      await page.getByLabel(/email/i).fill('invalid@test.com');
      await page.getByLabel(/password/i).fill('wrongpassword');
      await page.getByRole('button', { name: /sign in/i }).click();

      // Should show an error message
      await expect(page.getByText(/invalid|incorrect|error/i)).toBeVisible({ timeout: 10000 });
    });
  });

  test.describe('Register Page', () => {
    test('should display registration form', async ({ page }) => {
      await page.goto('/register');

      await expect(page.getByLabel(/full name/i)).toBeVisible();
      await expect(page.getByLabel(/email/i)).toBeVisible();
      await expect(page.getByLabel(/^password$/i)).toBeVisible();
      await expect(page.getByRole('button', { name: /create account/i })).toBeVisible();
    });

    test('should have role selection', async ({ page }) => {
      await page.goto('/register');

      // Look for role dropdown - labeled "I want to..."
      const roleField = page.getByLabel(/i want to/i);
      await expect(roleField).toBeVisible();
    });

    test('should have link to login page', async ({ page }) => {
      await page.goto('/register');

      // Check for the "Sign in" link in the form section (not the nav bar)
      await expect(page.locator('main').getByRole('link', { name: /sign in/i })).toBeVisible();
    });
  });

  test.describe('Forgot Password Page', () => {
    test('should display forgot password form', async ({ page }) => {
      await page.goto('/forgot-password');

      await expect(page.getByLabel(/email/i)).toBeVisible();
      await expect(page.getByRole('button', { name: /reset|send|submit/i })).toBeVisible();
    });

    test('should have link back to login', async ({ page }) => {
      await page.goto('/forgot-password');

      // Check for the "Sign in" link in the main content (not the nav bar)
      await expect(page.locator('main').getByRole('link', { name: /sign in/i })).toBeVisible();
    });
  });
});

test.describe('Protected Routes', () => {
  test('should redirect unauthenticated users from dashboard to login', async ({ page }) => {
    await page.goto('/adopter/dashboard');

    // Should redirect to login
    await expect(page).toHaveURL(/.*login/);
  });

  test('should redirect unauthenticated users from foster dashboard to login', async ({ page }) => {
    await page.goto('/foster/dashboard');

    await expect(page).toHaveURL(/.*login/);
  });

  test('should redirect unauthenticated users from rescue dashboard to login', async ({ page }) => {
    await page.goto('/rescue/dashboard');

    await expect(page).toHaveURL(/.*login/);
  });

  test('should redirect unauthenticated users from vet dashboard to login', async ({ page }) => {
    await page.goto('/vet/dashboard');

    await expect(page).toHaveURL(/.*login/);
  });

  test('should redirect unauthenticated users from admin dashboard to login', async ({ page }) => {
    await page.goto('/admin/dashboard');

    await expect(page).toHaveURL(/.*login/);
  });
});

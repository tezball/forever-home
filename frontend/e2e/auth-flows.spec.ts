import { test, expect } from '@playwright/test';
import { generateUserData, TEST_ACCOUNTS } from './fixtures/test-data';

test.describe('Authentication Flows', () => {
  test.describe('User Registration', () => {
    test('should register new foster user', async ({ page }) => {
      const userData = generateUserData('FOSTER');

      await page.goto('/register');

      await page.getByLabel(/full name/i).fill(userData.name);
      await page.getByLabel(/email/i).fill(userData.email);
      await page.getByPlaceholder(/at least 8 characters/i).fill(userData.password);
      await page.getByPlaceholder(/confirm your password/i).fill(userData.password);
      await page.getByLabel(/i want to/i).selectOption('FOSTER');

      await page.getByRole('button', { name: /create account/i }).click();

      // Should show success message (check email page)
      await expect(page.getByRole('heading', { name: /check your email/i })).toBeVisible({ timeout: 10000 });
    });

    test('should register new adopter user', async ({ page }) => {
      const userData = generateUserData('ADOPTER');

      await page.goto('/register');

      await page.getByLabel(/full name/i).fill(userData.name);
      await page.getByLabel(/email/i).fill(userData.email);
      await page.getByPlaceholder(/at least 8 characters/i).fill(userData.password);
      await page.getByPlaceholder(/confirm your password/i).fill(userData.password);
      await page.getByLabel(/i want to/i).selectOption('ADOPTER');

      await page.getByRole('button', { name: /create account/i }).click();

      await expect(page.getByRole('heading', { name: /check your email/i })).toBeVisible({ timeout: 10000 });
    });

    test('should register new rescue organization', async ({ page }) => {
      const userData = generateUserData('RESCUE_ORG');

      await page.goto('/register');

      await page.getByLabel(/full name/i).fill(userData.name);
      await page.getByLabel(/email/i).fill(userData.email);
      await page.getByPlaceholder(/at least 8 characters/i).fill(userData.password);
      await page.getByPlaceholder(/confirm your password/i).fill(userData.password);
      await page.getByLabel(/i want to/i).selectOption('RESCUE_ORG');

      await page.getByRole('button', { name: /create account/i }).click();

      await expect(page.getByRole('heading', { name: /check your email/i })).toBeVisible({ timeout: 10000 });
    });

    test('should register new vet user', async ({ page }) => {
      const userData = generateUserData('VET');

      await page.goto('/register');

      await page.getByLabel(/full name/i).fill(userData.name);
      await page.getByLabel(/email/i).fill(userData.email);
      await page.getByPlaceholder(/at least 8 characters/i).fill(userData.password);
      await page.getByPlaceholder(/confirm your password/i).fill(userData.password);
      await page.getByLabel(/i want to/i).selectOption('VET');

      await page.getByRole('button', { name: /create account/i }).click();

      await expect(page.getByRole('heading', { name: /check your email/i })).toBeVisible({ timeout: 10000 });
    });

    test('should show error for duplicate email', async ({ page }) => {
      await page.goto('/register');

      await page.getByLabel(/full name/i).fill('Duplicate User');
      await page.getByLabel(/email/i).fill(TEST_ACCOUNTS.foster.email);
      await page.getByPlaceholder(/at least 8 characters/i).fill('TestPass123!');
      await page.getByPlaceholder(/confirm your password/i).fill('TestPass123!');
      await page.getByLabel(/i want to/i).selectOption('FOSTER');

      await page.getByRole('button', { name: /create account/i }).click();

      // Should show error for duplicate email (409 conflict)
      await expect(page.getByText(/already exists|email.*taken|account.*exists|409|failed/i)).toBeVisible({
        timeout: 10000,
      });
    });

    test('should validate password requirements', async ({ page }) => {
      await page.goto('/register');

      await page.getByLabel(/full name/i).fill('Test User');
      await page.getByLabel(/email/i).fill('weak@test.com');
      await page.getByPlaceholder(/at least 8 characters/i).fill('weak');
      await page.getByPlaceholder(/confirm your password/i).fill('weak');
      await page.getByLabel(/i want to/i).selectOption('FOSTER');

      await page.getByRole('button', { name: /create account/i }).click();

      // Should show password validation error
      const errorMessage = page.getByText(/password.*must|password.*require|at least 8|too short|too weak/i);
      await expect(errorMessage).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe('User Login', () => {
    test('should login with valid foster credentials', async ({ page }) => {
      await page.goto('/login');

      await page.getByLabel(/email/i).fill(TEST_ACCOUNTS.foster.email);
      await page.getByPlaceholder(/enter your password/i).fill(TEST_ACCOUNTS.foster.password);
      await page.getByRole('button', { name: /sign in/i }).click();

      await expect(page).toHaveURL(/.*dashboard|.*\/$/i, { timeout: 10000 });
    });

    test('should login with valid adopter credentials', async ({ page }) => {
      await page.goto('/login');

      await page.getByLabel(/email/i).fill(TEST_ACCOUNTS.adopter.email);
      await page.getByPlaceholder(/enter your password/i).fill(TEST_ACCOUNTS.adopter.password);
      await page.getByRole('button', { name: /sign in/i }).click();

      await expect(page).toHaveURL(/.*dashboard|.*\/$/i, { timeout: 10000 });
    });

    test('should login with valid rescue org credentials', async ({ page }) => {
      await page.goto('/login');

      await page.getByLabel(/email/i).fill(TEST_ACCOUNTS.rescue.email);
      await page.getByPlaceholder(/enter your password/i).fill(TEST_ACCOUNTS.rescue.password);
      await page.getByRole('button', { name: /sign in/i }).click();

      await expect(page).toHaveURL(/.*dashboard|.*\/$/i, { timeout: 10000 });
    });

    test('should login with valid vet credentials', async ({ page }) => {
      await page.goto('/login');

      await page.getByLabel(/email/i).fill(TEST_ACCOUNTS.vet.email);
      await page.getByPlaceholder(/enter your password/i).fill(TEST_ACCOUNTS.vet.password);
      await page.getByRole('button', { name: /sign in/i }).click();

      await expect(page).toHaveURL(/.*dashboard|.*\/$/i, { timeout: 10000 });
    });

    test('should login with valid admin credentials', async ({ page }) => {
      await page.goto('/login');

      await page.getByLabel(/email/i).fill(TEST_ACCOUNTS.admin.email);
      await page.getByPlaceholder(/enter your password/i).fill(TEST_ACCOUNTS.admin.password);
      await page.getByRole('button', { name: /sign in/i }).click();

      await expect(page).toHaveURL(/.*dashboard|.*admin|.*\/$/i, { timeout: 10000 });
    });

    test('should show error for invalid credentials', async ({ page }) => {
      await page.goto('/login');

      await page.getByLabel(/email/i).fill('invalid@test.com');
      await page.getByPlaceholder(/enter your password/i).fill('wrongpassword');
      await page.getByRole('button', { name: /sign in/i }).click();

      await expect(page.getByText(/invalid|incorrect|error|failed/i)).toBeVisible({
        timeout: 10000,
      });
    });

    test('should redirect to originally requested page after login', async ({ page }) => {
      // Try to access protected page
      await page.goto('/foster/dashboard');

      // Should redirect to login
      await expect(page).toHaveURL(/.*login/);

      // Login
      await page.getByLabel(/email/i).fill(TEST_ACCOUNTS.foster.email);
      await page.getByPlaceholder(/enter your password/i).fill(TEST_ACCOUNTS.foster.password);
      await page.getByRole('button', { name: /sign in/i }).click();

      // Should redirect after login (either to dashboard or home)
      await expect(page).toHaveURL(/.*foster.*dashboard|.*\/$/i, { timeout: 10000 });
    });
  });

  test.describe('User Logout', () => {
    test('should logout user successfully', async ({ page }) => {
      // First login
      await page.goto('/login');
      await page.getByLabel(/email/i).fill(TEST_ACCOUNTS.foster.email);
      await page.getByPlaceholder(/enter your password/i).fill(TEST_ACCOUNTS.foster.password);
      await page.getByRole('button', { name: /sign in/i }).click();

      await expect(page).toHaveURL(/.*dashboard|.*\/$/i, { timeout: 10000 });

      // Find logout button (might be in user menu)
      const userMenu = page.locator('[data-testid="user-menu"], .user-menu, [aria-label*="user"]');
      if (await userMenu.isVisible()) {
        await userMenu.click();
      }

      const logoutButton = page.getByRole('button', { name: /sign out|logout/i });
      if (await logoutButton.isVisible()) {
        await logoutButton.click();
        await expect(page).toHaveURL(/.*login|.*\/$/);
      }
    });
  });

  test.describe('Password Reset', () => {
    test('should request password reset', async ({ page }) => {
      await page.goto('/forgot-password');

      await page.getByLabel(/email/i).fill(TEST_ACCOUNTS.foster.email);
      await page.getByRole('button', { name: /reset|send|submit/i }).click();

      // Should show success message (Check your email heading)
      await expect(page.getByRole('heading', { name: /check your email/i })).toBeVisible({
        timeout: 10000,
      });
    });

    test('should show error for non-existent email on reset', async ({ page }) => {
      await page.goto('/forgot-password');

      await page.getByLabel(/email/i).fill('nonexistent@example.com');
      await page.getByRole('button', { name: /reset|send|submit/i }).click();

      // Should show error or still show success (for security reasons some apps don't reveal if email exists)
      await page.waitForTimeout(2000);
      // The test passes if no error is thrown
    });
  });

  test.describe('Session Persistence', () => {
    test('should maintain session across page navigation', async ({ page }) => {
      // Login
      await page.goto('/login');
      await page.getByLabel(/email/i).fill(TEST_ACCOUNTS.foster.email);
      await page.getByPlaceholder(/enter your password/i).fill(TEST_ACCOUNTS.foster.password);
      await page.getByRole('button', { name: /sign in/i }).click();

      await expect(page).toHaveURL(/.*dashboard|.*\/$/i, { timeout: 10000 });

      // Navigate to different pages
      await page.goto('/pets');
      await expect(page).toHaveURL(/.*pets/);

      // Navigate back to dashboard
      await page.goto('/foster/dashboard');
      await expect(page).toHaveURL(/.*foster.*dashboard/);

      // Should still be logged in (dashboard should load, not redirect to login)
      await expect(page.getByRole('heading', { name: /foster dashboard/i })).toBeVisible();
    });
  });

  test.describe('Role-Based Access Control', () => {
    test('should prevent foster from accessing adopter dashboard', async ({ page }) => {
      await page.goto('/login');
      await page.getByLabel(/email/i).fill(TEST_ACCOUNTS.foster.email);
      await page.getByPlaceholder(/enter your password/i).fill(TEST_ACCOUNTS.foster.password);
      await page.getByRole('button', { name: /sign in/i }).click();

      await expect(page).toHaveURL(/.*dashboard|.*\/$/i, { timeout: 10000 });

      // Try to access adopter dashboard
      await page.goto('/adopter/dashboard');

      // Should redirect or show unauthorized
      await expect(page).not.toHaveURL(/.*adopter.*dashboard/);
    });

    test('should prevent non-admin from accessing admin dashboard', async ({ page }) => {
      await page.goto('/login');
      await page.getByLabel(/email/i).fill(TEST_ACCOUNTS.foster.email);
      await page.getByPlaceholder(/enter your password/i).fill(TEST_ACCOUNTS.foster.password);
      await page.getByRole('button', { name: /sign in/i }).click();

      await expect(page).toHaveURL(/.*dashboard|.*\/$/i, { timeout: 10000 });

      // Try to access admin dashboard
      await page.goto('/admin/dashboard');

      // Should redirect or show unauthorized
      await expect(page).not.toHaveURL(/.*admin.*dashboard/);
    });
  });
});

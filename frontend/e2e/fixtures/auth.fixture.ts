import { test as base, Page } from '@playwright/test';

// Test accounts from the application
export const TEST_ACCOUNTS = {
  admin: { email: 'admin@test.com', password: 'password123' },
  foster: { email: 'foster@test.com', password: 'password123' },
  adopter: { email: 'adopter@test.com', password: 'password123' },
  vet: { email: 'vet@test.com', password: 'password123' },
  rescue: { email: 'rescue@test.com', password: 'password123' },
};

async function login(page: Page, email: string, password: string, retries = 3): Promise<void> {
  for (let attempt = 1; attempt <= retries; attempt++) {
    await page.goto('/login');

    await page.getByLabel(/email/i).fill(email);
    await page.getByLabel(/password/i).fill(password);
    await page.getByRole('button', { name: /sign in/i }).click();

    try {
      // Wait for navigation away from login page (successful auth redirects to home or dashboard)
      await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 10000 });
      return; // Success, exit the function
    } catch {
      // Check if there's an error message visible
      const errorVisible = await page.getByText(/invalid email or password/i).isVisible().catch(() => false);
      if (errorVisible && attempt < retries) {
        // Wait a bit before retrying
        await page.waitForTimeout(1000);
        continue;
      }
      if (attempt === retries) {
        throw new Error(`Login failed for ${email} after ${retries} attempts`);
      }
    }
  }
}

export const test = base.extend<{
  authenticatedPage: Page;
  adopterPage: Page;
  fosterPage: Page;
  rescuePage: Page;
  vetPage: Page;
  adminPage: Page;
}>({
  authenticatedPage: async ({ page }, use) => {
    await login(page, TEST_ACCOUNTS.adopter.email, TEST_ACCOUNTS.adopter.password);
    await use(page);
  },
  adopterPage: async ({ page }, use) => {
    await login(page, TEST_ACCOUNTS.adopter.email, TEST_ACCOUNTS.adopter.password);
    await use(page);
  },
  fosterPage: async ({ page }, use) => {
    await login(page, TEST_ACCOUNTS.foster.email, TEST_ACCOUNTS.foster.password);
    await use(page);
  },
  rescuePage: async ({ page }, use) => {
    await login(page, TEST_ACCOUNTS.rescue.email, TEST_ACCOUNTS.rescue.password);
    await use(page);
  },
  vetPage: async ({ page }, use) => {
    await login(page, TEST_ACCOUNTS.vet.email, TEST_ACCOUNTS.vet.password);
    await use(page);
  },
  adminPage: async ({ page }, use) => {
    await login(page, TEST_ACCOUNTS.admin.email, TEST_ACCOUNTS.admin.password);
    await use(page);
  },
});

export { expect } from '@playwright/test';

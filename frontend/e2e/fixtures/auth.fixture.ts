import { test as base, Page } from '@playwright/test';

// Test accounts from the application
export const TEST_ACCOUNTS = {
  superAdmin: { email: 'superadmin@test.com', password: 'password123' },
  admin: { email: 'admin@test.com', password: 'password123' },
  foster: { email: 'foster@test.com', password: 'password123' },
  adopter: { email: 'adopter@test.com', password: 'password123' },
  vet: { email: 'vet@test.com', password: 'password123' },
  rescue: { email: 'rescue@test.com', password: 'password123' },
};

async function login(page: Page, email: string, password: string, retries = 5): Promise<void> {
  const baseDelay = 500; // Start with 500ms

  for (let attempt = 1; attempt <= retries; attempt++) {
    try {
      await page.goto('/login', { waitUntil: 'networkidle' });

      // Wait for the form to be fully loaded
      await page.waitForSelector('[data-testid="login-form"], form', { timeout: 5000 }).catch(() => {});

      await page.getByLabel(/email/i).fill(email);
      await page.getByPlaceholder(/enter your password/i).fill(password);
      await page.getByRole('button', { name: 'Sign In', exact: true }).click();

      // Wait for navigation away from login page (successful auth redirects to home or dashboard)
      await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 15000 });
      return; // Success, exit the function
    } catch (error) {
      // Check if there's an error message visible
      const errorVisible = await page.getByText(/invalid email or password/i).isVisible().catch(() => false);

      if (attempt < retries) {
        // Exponential backoff: 500ms, 1s, 2s, 4s, 8s
        const delay = baseDelay * Math.pow(2, attempt - 1);
        console.log(`Login attempt ${attempt} failed for ${email}, retrying in ${delay}ms...`);
        await page.waitForTimeout(delay);
        continue;
      }

      const errorMessage = errorVisible
        ? 'Invalid credentials'
        : error instanceof Error ? error.message : 'Unknown error';
      throw new Error(`Login failed for ${email} after ${retries} attempts: ${errorMessage}`);
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
  superAdminPage: Page;
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
  superAdminPage: async ({ page }, use) => {
    await login(page, TEST_ACCOUNTS.superAdmin.email, TEST_ACCOUNTS.superAdmin.password);
    await use(page);
  },
});

export { expect } from '@playwright/test';

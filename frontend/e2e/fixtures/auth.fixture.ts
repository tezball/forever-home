import { test as base, Page } from '@playwright/test';

// Test accounts from the application
export const TEST_ACCOUNTS = {
  admin: { email: 'admin@test.com', password: 'password123' },
  foster: { email: 'foster@test.com', password: 'password123' },
  adopter: { email: 'adopter@test.com', password: 'password123' },
  vet: { email: 'vet@test.com', password: 'password123' },
  rescue: { email: 'rescue@test.com', password: 'password123' },
};

async function login(page: Page, email: string, password: string): Promise<void> {
  await page.goto('/login');

  await page.getByLabel(/email/i).fill(email);
  await page.getByLabel(/password/i).fill(password);
  await page.getByRole('button', { name: /sign in/i }).click();

  // Wait for authentication to complete (redirect or dashboard load)
  await page.waitForURL(/.*dashboard|.*\/$/);
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

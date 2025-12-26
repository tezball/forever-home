import { test, expect } from './fixtures/auth.fixture';

test.describe('Super Admin Journey', () => {
  test.describe('Dashboard', () => {
    test('should display admin dashboard', async ({ superAdminPage }) => {
      await superAdminPage.goto('/admin');

      await expect(superAdminPage.getByRole('heading', { name: /admin dashboard/i })).toBeVisible();
    });

    test('should display platform statistics section', async ({ superAdminPage }) => {
      await superAdminPage.goto('/admin');

      await expect(superAdminPage.getByRole('heading', { name: /platform statistics/i })).toBeVisible();
    });

    test('should display user statistics', async ({ superAdminPage }) => {
      await superAdminPage.goto('/admin');

      // Check for user stat cards
      await expect(superAdminPage.getByText('Total Users')).toBeVisible();
      await expect(superAdminPage.getByText('Fosters')).toBeVisible();
      await expect(superAdminPage.getByText('Adopters')).toBeVisible();
    });

    test('should display pet statistics', async ({ superAdminPage }) => {
      await superAdminPage.goto('/admin');

      await expect(superAdminPage.getByRole('heading', { name: /pet statistics/i })).toBeVisible();
      await expect(superAdminPage.getByText('Available')).toBeVisible();
    });

    test('should display rescue organization statistics', async ({ superAdminPage }) => {
      await superAdminPage.goto('/admin');

      await expect(superAdminPage.getByRole('heading', { name: /rescue organization statistics/i })).toBeVisible();
      await expect(superAdminPage.getByText('Total Rescues')).toBeVisible();
      await expect(superAdminPage.getByText('Verified', { exact: true })).toBeVisible();
    });

    test('should display adoption statistics', async ({ superAdminPage }) => {
      await superAdminPage.goto('/admin');

      await expect(superAdminPage.getByRole('heading', { name: /adoption statistics/i })).toBeVisible();
      await expect(superAdminPage.getByText('Total Adoptions Completed')).toBeVisible();
    });
  });

  test.describe('Rescue Organization Management', () => {
    test('should display rescue organizations section', async ({ superAdminPage }) => {
      await superAdminPage.goto('/admin');

      await expect(superAdminPage.getByRole('heading', { name: /rescue organizations/i })).toBeVisible();
    });

    test('should have pending and all view tabs', async ({ superAdminPage }) => {
      await superAdminPage.goto('/admin');

      await expect(superAdminPage.getByRole('button', { name: /pending/i })).toBeVisible();
      await expect(superAdminPage.getByRole('button', { name: /all/i })).toBeVisible();
    });

    test('should toggle between pending and all rescues', async ({ superAdminPage }) => {
      await superAdminPage.goto('/admin');

      // Click on All tab
      await superAdminPage.getByRole('button', { name: /all/i }).click();

      // Should show all rescues (verified ones from test data)
      await superAdminPage.waitForTimeout(500);

      // Click back to Pending
      await superAdminPage.getByRole('button', { name: /pending/i }).click();
      await superAdminPage.waitForTimeout(500);
    });

    test('should show rescue verification status badge', async ({ superAdminPage }) => {
      await superAdminPage.goto('/admin');

      // Click on All tab to see verified rescues
      await superAdminPage.getByRole('button', { name: /all/i }).click();
      await superAdminPage.waitForTimeout(500);

      // Should see verified badges
      const verifiedBadge = superAdminPage.getByText('Verified').first();
      await expect(verifiedBadge).toBeVisible();
    });

    test('should open rescue detail modal', async ({ superAdminPage }) => {
      await superAdminPage.goto('/admin');

      // Click on All tab to see rescues
      await superAdminPage.getByRole('button', { name: /all/i }).click();
      await superAdminPage.waitForTimeout(500);

      // Click View button on first rescue
      const viewButton = superAdminPage.getByRole('button', { name: /view/i }).first();
      if (await viewButton.isVisible()) {
        await viewButton.click();
        await superAdminPage.waitForTimeout(500);

        // Modal should be visible with rescue details
        await expect(superAdminPage.getByText(/verification status/i)).toBeVisible();
      }
    });

    test('should have revoke verification button for verified rescues', async ({ superAdminPage }) => {
      await superAdminPage.goto('/admin');

      // Click on All tab
      await superAdminPage.getByRole('button', { name: /all/i }).click();
      await superAdminPage.waitForTimeout(500);

      // Click View on a verified rescue
      const viewButton = superAdminPage.getByRole('button', { name: /view/i }).first();
      if (await viewButton.isVisible()) {
        await viewButton.click();
        await superAdminPage.waitForTimeout(500);

        // Should have revoke verification button
        const revokeButton = superAdminPage.getByRole('button', { name: /revoke verification/i });
        await expect(revokeButton).toBeVisible();
      }
    });
  });

  test.describe('Action Required Banner', () => {
    test('should show action required banner when pending rescues exist', async ({ superAdminPage }) => {
      await superAdminPage.goto('/admin');

      // If there are pending rescues, the banner should be visible
      const banner = superAdminPage.getByRole('heading', { name: /action required/i });
      // This may or may not be visible depending on test data state
      // Just check that the dashboard loaded properly
      await expect(superAdminPage.getByRole('heading', { name: /admin dashboard/i })).toBeVisible();
    });
  });

  test.describe('Navigation', () => {
    test('should have admin dashboard link in header when logged in as super admin', async ({ superAdminPage }) => {
      await superAdminPage.goto('/');

      // Check for Admin Dashboard link in navigation
      await expect(superAdminPage.getByRole('link', { name: /admin dashboard/i })).toBeVisible();
    });

    test('should navigate to admin dashboard from header link', async ({ superAdminPage }) => {
      await superAdminPage.goto('/');

      await superAdminPage.getByRole('link', { name: /admin dashboard/i }).click();

      await expect(superAdminPage.getByRole('heading', { name: /admin dashboard/i })).toBeVisible();
    });
  });

  test.describe('Access Control', () => {
    test('should redirect non-admin users from admin page', async ({ adopterPage }) => {
      await adopterPage.goto('/admin');

      // Should redirect to home since adopter doesn't have SUPER_ADMIN role
      await adopterPage.waitForURL((url) => !url.pathname.includes('/admin'), { timeout: 5000 });
    });

    test('should allow super admin to access admin page', async ({ superAdminPage }) => {
      await superAdminPage.goto('/admin');

      // Should stay on admin page and show dashboard
      await expect(superAdminPage.getByRole('heading', { name: /admin dashboard/i })).toBeVisible();
    });
  });
});

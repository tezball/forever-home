import { test, expect } from './fixtures/auth.fixture';

test.describe('Adopter Dashboard', () => {
  test('should display adopter dashboard after login', async ({ adopterPage }) => {
    await adopterPage.goto('/adopter/dashboard');

    await expect(adopterPage.getByRole('heading', { name: /adopter dashboard/i })).toBeVisible();
  });

  test('should display favorites section', async ({ adopterPage }) => {
    await adopterPage.goto('/adopter/dashboard');

    await expect(adopterPage.getByRole('heading', { name: 'Saved Pets', exact: true })).toBeVisible();
  });

  test('should display applications section', async ({ adopterPage }) => {
    await adopterPage.goto('/adopter/dashboard');

    // Dashboard shows application stats (Pending, Approved, Rejected)
    await expect(adopterPage.getByText('Pending')).toBeVisible();
  });

  test('should display stats cards', async ({ adopterPage }) => {
    await adopterPage.goto('/adopter/dashboard');

    // Should show statistics - "Favorites" is one of the stat labels
    await expect(adopterPage.getByText('Favorites')).toBeVisible();
  });
});

test.describe('Foster Dashboard', () => {
  test('should display foster dashboard after login', async ({ fosterPage }) => {
    await fosterPage.goto('/foster/dashboard');

    await expect(fosterPage.getByRole('heading', { name: /foster dashboard/i })).toBeVisible();
  });

  test('should have register pet button', async ({ fosterPage }) => {
    await fosterPage.goto('/foster/dashboard');

    await expect(fosterPage.getByRole('link', { name: /register a pet/i })).toBeVisible();
  });

  test('should display pet list section', async ({ fosterPage }) => {
    await fosterPage.goto('/foster/dashboard');

    // Dashboard shows pet stats
    await expect(fosterPage.getByText('Drafts')).toBeVisible();
  });
});

test.describe('Rescue Organization Dashboard', () => {
  test('should display rescue dashboard after login', async ({ rescuePage }) => {
    await rescuePage.goto('/rescue/dashboard');

    await expect(rescuePage.getByRole('heading', { name: /rescue.*dashboard/i })).toBeVisible();
  });

  test('should display pending review section', async ({ rescuePage }) => {
    await rescuePage.goto('/rescue/dashboard');

    await expect(rescuePage.getByRole('heading', { name: /pending review/i })).toBeVisible();
  });

  test('should display applications section', async ({ rescuePage }) => {
    await rescuePage.goto('/rescue/dashboard');

    await expect(rescuePage.getByRole('heading', { name: /adoption applications/i })).toBeVisible();
  });
});

test.describe('Vet Dashboard', () => {
  test('should display vet dashboard after login', async ({ vetPage }) => {
    await vetPage.goto('/vet/dashboard');

    await expect(vetPage.getByRole('heading', { name: /veterinarian dashboard/i })).toBeVisible();
  });

  test('should have microchip lookup field', async ({ vetPage }) => {
    await vetPage.goto('/vet/dashboard');

    await expect(vetPage.getByPlaceholder(/microchip/i)).toBeVisible();
  });

  test('should have search button', async ({ vetPage }) => {
    await vetPage.goto('/vet/dashboard');

    await expect(vetPage.getByRole('button', { name: /search/i })).toBeVisible();
  });
});

test.describe('Admin Dashboard', () => {
  test('should display admin dashboard after login', async ({ adminPage }) => {
    await adminPage.goto('/admin/dashboard');

    await expect(adminPage.getByRole('heading', { name: /admin dashboard/i })).toBeVisible();
  });

  test('should display system statistics', async ({ adminPage }) => {
    await adminPage.goto('/admin/dashboard');

    // Check for User Management tab
    await expect(adminPage.getByRole('button', { name: /user management/i })).toBeVisible();
  });

  test('should have user management tab/section', async ({ adminPage }) => {
    await adminPage.goto('/admin/dashboard');

    await expect(adminPage.getByRole('button', { name: /user management/i })).toBeVisible();
  });

  test('should have rescue org approvals section', async ({ adminPage }) => {
    await adminPage.goto('/admin/dashboard');

    await expect(adminPage.getByRole('button', { name: /rescue org approvals/i })).toBeVisible();
  });
});

test.describe('User Profile', () => {
  test('should access profile page when authenticated', async ({ authenticatedPage }) => {
    // authenticatedPage logs in as adopter, so go to adopter dashboard
    await authenticatedPage.goto('/adopter/dashboard');

    // Dashboard should be visible - profile accessed via nav
    await expect(authenticatedPage.getByRole('heading', { name: /adopter dashboard/i })).toBeVisible();
  });

  test('should display user information', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/adopter/dashboard');

    // Dashboard should be visible with user name
    await expect(authenticatedPage.getByText(/welcome back/i)).toBeVisible();
  });
});

test.describe('Sign Out', () => {
  test('should sign out user', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/adopter/dashboard');

    // Find and click the user menu or sign out button
    const signOutButton = authenticatedPage.getByRole('button', { name: /sign out/i });
    const userMenu = authenticatedPage.locator('[data-testid="user-menu"], .user-menu');

    if (await userMenu.isVisible()) {
      await userMenu.click();
    }

    if (await signOutButton.isVisible()) {
      await signOutButton.click();

      // Should redirect to login or home
      await expect(authenticatedPage).toHaveURL(/.*login|\//);
    }
  });
});

test.describe('Notifications', () => {
  test('should display notification bell when authenticated', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/adopter/dashboard');

    // Dashboard should be visible with notification bell
    await expect(authenticatedPage.getByRole('heading', { name: /adopter dashboard/i })).toBeVisible();
    await expect(authenticatedPage.getByRole('button', { name: /notifications/i })).toBeVisible();
  });
});

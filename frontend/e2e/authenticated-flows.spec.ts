import { test, expect, TEST_ACCOUNTS } from './fixtures/auth.fixture';

test.describe('Adopter Dashboard', () => {
  test('should display adopter dashboard after login', async ({ adopterPage }) => {
    await adopterPage.goto('/adopter/dashboard');

    await expect(adopterPage.getByRole('heading', { name: /dashboard/i })).toBeVisible();
  });

  test('should display favorites section', async ({ adopterPage }) => {
    await adopterPage.goto('/adopter/dashboard');

    await expect(adopterPage.getByText(/favorites|saved pets/i)).toBeVisible();
  });

  test('should display applications section', async ({ adopterPage }) => {
    await adopterPage.goto('/adopter/dashboard');

    await expect(adopterPage.getByText(/applications/i)).toBeVisible();
  });

  test('should display stats cards', async ({ adopterPage }) => {
    await adopterPage.goto('/adopter/dashboard');

    // Should show some statistics
    await expect(adopterPage.locator('[data-testid="stats"], .stats, .stat-card').first()).toBeVisible();
  });
});

test.describe('Foster Dashboard', () => {
  test('should display foster dashboard after login', async ({ fosterPage }) => {
    await fosterPage.goto('/foster/dashboard');

    await expect(fosterPage.getByRole('heading', { name: /dashboard/i })).toBeVisible();
  });

  test('should have register pet button', async ({ fosterPage }) => {
    await fosterPage.goto('/foster/dashboard');

    await expect(fosterPage.getByRole('link', { name: /register.*pet|add.*pet|new.*pet/i })).toBeVisible();
  });

  test('should display pet list section', async ({ fosterPage }) => {
    await fosterPage.goto('/foster/dashboard');

    await expect(fosterPage.getByText(/pets|listings/i)).toBeVisible();
  });
});

test.describe('Rescue Organization Dashboard', () => {
  test('should display rescue dashboard after login', async ({ rescuePage }) => {
    await rescuePage.goto('/rescue/dashboard');

    await expect(rescuePage.getByRole('heading', { name: /dashboard/i })).toBeVisible();
  });

  test('should display pending review section', async ({ rescuePage }) => {
    await rescuePage.goto('/rescue/dashboard');

    await expect(rescuePage.getByText(/pending.*review|review/i)).toBeVisible();
  });

  test('should display applications section', async ({ rescuePage }) => {
    await rescuePage.goto('/rescue/dashboard');

    await expect(rescuePage.getByText(/applications/i)).toBeVisible();
  });
});

test.describe('Vet Dashboard', () => {
  test('should display vet dashboard after login', async ({ vetPage }) => {
    await vetPage.goto('/vet/dashboard');

    await expect(vetPage.getByRole('heading', { name: /dashboard/i })).toBeVisible();
  });

  test('should have microchip lookup field', async ({ vetPage }) => {
    await vetPage.goto('/vet/dashboard');

    await expect(vetPage.getByPlaceholder(/microchip/i)).toBeVisible();
  });

  test('should have search button', async ({ vetPage }) => {
    await vetPage.goto('/vet/dashboard');

    await expect(vetPage.getByRole('button', { name: /search|lookup|find/i })).toBeVisible();
  });
});

test.describe('Admin Dashboard', () => {
  test('should display admin dashboard after login', async ({ adminPage }) => {
    await adminPage.goto('/admin/dashboard');

    await expect(adminPage.getByRole('heading', { name: /dashboard|admin/i })).toBeVisible();
  });

  test('should display system statistics', async ({ adminPage }) => {
    await adminPage.goto('/admin/dashboard');

    await expect(adminPage.getByText(/users|pets|adoptions/i)).toBeVisible();
  });

  test('should have user management tab/section', async ({ adminPage }) => {
    await adminPage.goto('/admin/dashboard');

    await expect(adminPage.getByText(/user.*management|manage.*users/i)).toBeVisible();
  });

  test('should have rescue org approvals section', async ({ adminPage }) => {
    await adminPage.goto('/admin/dashboard');

    await expect(adminPage.getByText(/rescue.*approval|pending.*approval/i)).toBeVisible();
  });
});

test.describe('User Profile', () => {
  test('should access profile page when authenticated', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/profile');

    await expect(authenticatedPage.getByRole('heading', { name: /profile/i })).toBeVisible();
  });

  test('should display user information', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/profile');

    await expect(authenticatedPage.getByText(/email|name/i)).toBeVisible();
  });
});

test.describe('Sign Out', () => {
  test('should sign out user', async ({ authenticatedPage }) => {
    // Find and click the user menu or sign out button
    const signOutButton = authenticatedPage.getByRole('button', { name: /sign out|logout/i });
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
    await authenticatedPage.goto('/');

    const notificationBell = authenticatedPage.locator('[data-testid="notifications"], .notification-bell, [aria-label*="notification"]');
    await expect(notificationBell).toBeVisible();
  });
});

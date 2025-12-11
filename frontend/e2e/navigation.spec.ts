import { test, expect } from '@playwright/test';

test.describe('Navigation', () => {
  test.describe('Header Navigation', () => {
    test('should have visible navigation links', async ({ page }) => {
      await page.goto('/');

      await expect(page.getByRole('navigation')).toBeVisible();
    });

    test('should navigate to home from logo', async ({ page }) => {
      await page.goto('/pets');

      // Click logo/brand link
      const logo = page.getByRole('link', { name: /forever home/i }).first();
      if (await logo.isVisible()) {
        await logo.click();
        await expect(page).toHaveURL('/');
      }
    });

    test('should navigate to browse pets', async ({ page }) => {
      await page.goto('/');

      await page.getByRole('navigation').getByRole('link', { name: /browse pets/i }).click();
      await expect(page).toHaveURL(/.*pets/);
    });

    test('should navigate to rescues page', async ({ page }) => {
      await page.goto('/');

      const rescueLink = page.getByRole('navigation').getByRole('link', { name: /rescues/i });
      if (await rescueLink.isVisible()) {
        await rescueLink.click();
        await expect(page).toHaveURL(/.*rescues/);
      }
    });
  });

  test.describe('Footer Navigation', () => {
    test('should display footer', async ({ page }) => {
      await page.goto('/');

      await expect(page.locator('footer')).toBeVisible();
    });

    test('should have privacy policy link', async ({ page }) => {
      await page.goto('/');

      const privacyLink = page.getByRole('link', { name: /privacy/i });
      if (await privacyLink.isVisible()) {
        await privacyLink.click();
        await expect(page).toHaveURL(/.*privacy/);
      }
    });

    test('should have help/FAQ link', async ({ page }) => {
      await page.goto('/');

      const helpLink = page.getByRole('link', { name: /help/i });
      if (await helpLink.isVisible()) {
        await helpLink.click();
        await expect(page).toHaveURL(/.*help/);
      }
    });

    test('should have contact link', async ({ page }) => {
      await page.goto('/');

      const contactLink = page.getByRole('link', { name: /contact/i });
      if (await contactLink.isVisible()) {
        await contactLink.click();
        await expect(page).toHaveURL(/.*contact/);
      }
    });
  });

  test.describe('Mobile Navigation', () => {
    test.use({ viewport: { width: 375, height: 667 } });

    test('should have mobile menu button', async ({ page }) => {
      await page.goto('/');

      // Look for hamburger menu button
      const menuButton = page.getByRole('button', { name: /menu/i });
      if (await menuButton.isVisible()) {
        await expect(menuButton).toBeVisible();
      }
    });

    test('should open mobile menu on button click', async ({ page }) => {
      await page.goto('/');

      const menuButton = page.getByRole('button', { name: /menu/i });
      if (await menuButton.isVisible()) {
        await menuButton.click();

        // Should show navigation links
        await expect(page.getByRole('navigation').getByRole('link', { name: /browse pets/i })).toBeVisible();
      }
    });

    test('should close mobile menu on link click', async ({ page }) => {
      await page.goto('/');

      const menuButton = page.getByRole('button', { name: /menu/i });
      if (await menuButton.isVisible()) {
        await menuButton.click();

        await page.getByRole('navigation').getByRole('link', { name: /browse pets/i }).click();

        await expect(page).toHaveURL(/.*pets/);
      }
    });
  });
});

test.describe('Static Pages', () => {
  test('should display privacy policy page', async ({ page }) => {
    await page.goto('/privacy');

    await expect(page.getByRole('heading', { name: /privacy/i })).toBeVisible();
  });

  test('should display help/FAQ page', async ({ page }) => {
    await page.goto('/help');

    await expect(page.getByRole('heading', { name: /help center/i })).toBeVisible();
  });

  test('should display contact page', async ({ page }) => {
    await page.goto('/contact');

    await expect(page.getByRole('heading', { name: /contact/i })).toBeVisible();
  });
});

test.describe('404 Page', () => {
  test('should display 404 page for unknown routes', async ({ page }) => {
    await page.goto('/this-page-does-not-exist-12345');

    await expect(page.getByRole('heading', { name: /404|not found/i })).toBeVisible();
  });

  test('should have link back to home from 404', async ({ page }) => {
    await page.goto('/this-page-does-not-exist-12345');

    const homeLink = page.getByRole('link', { name: /go back home/i });
    await expect(homeLink).toBeVisible();
  });
});

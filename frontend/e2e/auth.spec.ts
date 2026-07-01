import { test, expect } from '@playwright/test';

/**
 * E2E: Authentication guard behaviour.
 * API calls are intercepted so no backend is required.
 */
test.describe('RouteGuard', () => {
  test('redirige a /login cuando no hay token', async ({ page }) => {
    // Arrange — ensure localStorage is empty
    await page.addInitScript(() => {
      localStorage.clear();
    });

    // Act
    await page.goto('/cotizaciones');

    // Assert — should land on /login (page renders "Login Page" stub or URL changes)
    await expect(page).toHaveURL(/\/login/);
  });

  test('permite acceder a /cotizaciones con token válido', async ({ page }) => {
    // Arrange — inject token before page load, mock the API
    await page.addInitScript(() => {
      localStorage.setItem('access_token', 'eyJhbGciOiJIUzI1NiJ9.test.token');
    });
    await page.route('**/api/v1/cotizaciones', async (route) => {
      await route.fulfill({ json: [] });
    });

    // Act
    await page.goto('/cotizaciones');

    // Assert — RouteGuard passes through; page renders content
    await expect(page.getByText('Mis Cotizaciones')).toBeVisible();
    await expect(page).not.toHaveURL(/\/login/);
  });
});

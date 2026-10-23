import { test, expect } from '@playwright/test';

// Runs against a locally started server. API responses are deterministic here;
// Spring integration tests separately exercise persistence and provider failures.
test.beforeEach(async ({page}) => {
  const offer = {storeId: '1', storeName: 'Steam', price: 5, regularPrice: 20, discount: 75, dealRating: 9, url: 'https://www.cheapshark.com/redirect?dealID=test', checkedAt: new Date().toISOString()};
  const game = {id: '123', title: 'Test Adventure', steamAppId: '', image: '', reviewPercent: 95, reviewCount: 2000, reviewText: 'Very Positive', offers: [offer]};
  let wishlist: unknown[] = [];
  await page.route('**/api/catalog', route => route.fulfill({json: {games: [game], stores: [{id: '1', name: 'Steam'}, {id: '25', name: 'Epic Games'}, {id: '7', name: 'GOG'}], status: {lastSuccess: new Date().toISOString(), lastAttempt: null, syncing: false, stale: false, message: null}}}));
  await page.route('**/api/games/123', route => route.fulfill({json: {game, history: [{observedAt: new Date().toISOString(), price: 5}], providerHistoricalLow: null, comparisonFresh: true, note: 'Test offers in USD.'}}));
  await page.route('**/api/wishlist', route => route.fulfill({json: wishlist}));
  await page.route('**/api/wishlist/123', route => {
    if (route.request().method() === 'DELETE') {wishlist = []; return route.fulfill({status: 204});}
    const entry = {game, targetPrice: route.request().postDataJSON().targetPrice, createdAt: new Date().toISOString()};
    wishlist = [entry]; return route.fulfill({json: entry});
  });
  await page.route('**/api/auth/session', route => route.fulfill({json: {authenticated:false, name:'', googleEnabled:false, csrfToken:'test-token', csrfHeader:'X-CSRF-TOKEN'}}));
  await page.route('**/api/currencies', route => route.fulfill({json: {rates:{USD:1,RUB:80,KZT:500},date:'2026-10-23',stale:false}}));
  await page.goto('/');
});

test('search and category filters show useful empty states', async ({page}) => {
  await page.getByRole('searchbox', {name: 'Search games'}).fill('unknown game');
  await expect(page.getByRole('heading', {name: 'No games on this frequency.'})).toBeVisible();
  await page.getByRole('button', {name: 'Clear all filters'}).click();
  await expect(page.getByTestId('game-card')).toHaveCount(1);
  await page.getByRole('tab', {name: 'Free games', exact: true}).click();
  await expect(page.getByTestId('game-card')).toHaveCount(0);
});

test('compare a game and save a persistent target', async ({page}) => {
  await page.getByRole('button', {name: 'Compare prices for Test Adventure', exact: true}).click();
  await expect(page.getByRole('dialog', {name: 'Test Adventure'})).toBeVisible();
  await expect(page.getByRole('link', {name: 'View deal'})).toHaveAttribute('href', /cheapshark\.com\/redirect/);
  await page.getByRole('spinbutton', {name: 'Target price in USD'}).fill('3.50');
  await page.getByRole('button', {name: 'Save target', exact: true}).click();
  await expect(page.getByText('Saved to your wishlist.', {exact: true})).toBeVisible();
  await page.getByRole('button', {name: 'Close game details'}).click();
  await page.reload();
  if (await page.getByRole('button', {name: 'Open navigation'}).isVisible()) await page.getByRole('button', {name: 'Open navigation'}).click();
  await page.getByRole('button', {name: 'Wishlist 1', exact: true}).click();
  await expect(page.getByText('Target: $3.50', {exact: true})).toBeVisible();
  await page.getByRole('button', {name: 'Remove Test Adventure from wishlist'}).click();
  await expect(page.getByRole('heading', {name: 'Your next adventure starts with a heart.'})).toBeVisible();
});

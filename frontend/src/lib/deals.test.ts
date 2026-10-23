import { describe, expect, it } from 'vitest';
import { DEFAULT_FILTERS, filterGames, money } from './deals';
import type { Game, Offer } from './types';
const offer = (storeId: string, price: number, discount = 50): Offer => ({storeId, price, discount, storeName: storeId, regularPrice: 40, dealRating: 9, url: '', checkedAt: ''});
const game = (id: string, offers: Offer[]): Game => ({id, title: 'Adventure ' + id, steamAppId: '', image: '', reviewCount: 100, reviewPercent: 90, reviewText: '', offers});
describe('deal discovery', () => {
  it('uses the selected store price when applying a budget', () => {
    const games = [game('1', [offer('25', 2), offer('1', 20)])];
    expect(filterGames(games, {...DEFAULT_FILTERS, stores: ['1'], maxPrice: 10})).toEqual([]);
    expect(filterGames(games, {...DEFAULT_FILTERS, stores: ['1']} )[0].offers[0].price).toBe(20);
  });
  it('distinguishes zero-price offers from cheap games', () => {
    const games = [game('1', [offer('1', 0, 100)]), game('2', [offer('25', 0.99, 98)])];
    expect(filterGames(games, {...DEFAULT_FILTERS, tab: 'free'}).map(g => g.id)).toEqual(['1']);
    expect(money(0)).toBe('Free');
  });
  it('handles empty store selections and case-insensitive trimmed searches', () => {
    const games = [game('1', [offer('1', 10)])];
    expect(filterGames(games, {...DEFAULT_FILTERS, stores: []})).toEqual([]);
    expect(filterGames(games, {...DEFAULT_FILTERS, query: ' ADVENTURE '})).toHaveLength(1);
  });
  it('sorts and filters without mutating provider data', () => {
    const games = [game('1', [offer('1', 30), offer('25', 15)]), game('2', [offer('7', 3, 80)])];
    expect(filterGames(games, {...DEFAULT_FILTERS, sort: 'price'}).map(g => g.id)).toEqual(['2', '1']);
    expect(games[0].offers[0].price).toBe(30);
    expect(filterGames(games, {...DEFAULT_FILTERS, tab: 'big'}).map(g => g.id)).toEqual(['2']);
  });
});

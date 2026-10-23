import type { Filters, Game } from './types';
export const money = (value: number) => value === 0 ? 'Free' : new Intl.NumberFormat('en-US', {style: 'currency', currency: 'USD'}).format(value);
export const DEFAULT_FILTERS: Filters = {query: '', stores: ['1', '25', '7'], maxPrice: 100, tab: 'all', sort: 'best'};
export function filterGames(games: Game[], filters: Filters): Game[] {
  return games.filter(game => game.title.toLowerCase().includes(filters.query.trim().toLowerCase()))
    .map(game => ({...game, offers: game.offers.filter(offer => filters.stores.includes(offer.storeId)
      && offer.price <= filters.maxPrice
      && (filters.tab !== 'under10' || offer.price < 10)
      && (filters.tab !== 'big' || offer.discount >= 75)
      && (filters.tab !== 'free' || offer.price === 0))
      .sort((a, b) => a.price - b.price)}))
    .filter(game => game.offers.length > 0)
    .sort((a, b) => {
      if (filters.sort === 'price') return a.offers[0].price - b.offers[0].price;
      if (filters.sort === 'discount') return b.offers[0].discount - a.offers[0].discount;
      if (filters.sort === 'rating') return b.reviewPercent - a.reviewPercent || b.reviewCount - a.reviewCount;
      if (filters.sort === 'title') return a.title.localeCompare(b.title);
      return Math.max(...b.offers.map(o => o.dealRating)) - Math.max(...a.offers.map(o => o.dealRating)) || b.reviewCount - a.reviewCount;
    });
}
export function timeAgo(date: string | null) {
  if (!date) return 'Waiting for first refresh';
  const minutes = Math.max(0, Math.floor((Date.now() - new Date(date).getTime()) / 60000));
  if (minutes < 1) return 'Updated just now';
  if (minutes < 60) return `Updated ${minutes} min ago`;
  return `Updated ${Math.floor(minutes / 60)}h ago`;
}

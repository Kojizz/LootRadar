import type { Game } from './types';

export function gameImageSources(game: Game): string[] {
  const sources = [game.image, game.thumbnail].filter((src): src is string => !!src?.trim());
  if (/^[0-9]+$/.test(game.steamAppId ?? '')) {
    sources.push(`https://cdn.akamai.steamstatic.com/steam/apps/${game.steamAppId}/header.jpg`);
  }
  // Retry a transient failure once, bypassing a cached failed CDN response.
  const original = sources[0];
  if (original) {
    try {
      const retry = new URL(original);
      if (retry.protocol === 'https:' || retry.protocol === 'http:') {
        retry.searchParams.set('lootradar_retry', '1');
        sources.push(retry.toString());
      }
    } catch { /* Relative artwork can still use the other fallback sources. */ }
  }
  return [...new Set(sources)];
}

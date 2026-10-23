import { describe, expect, it } from 'vitest';
import { gameImageSources } from './gameImages';
import type { Game } from './types';

const game = {id:'1', title:'Test', image:'', thumbnail:'', steamAppId:''} as Game;
describe('game artwork recovery', () => {
  it('uses a thumbnail even when the primary image is absent', () => {
    expect(gameImageSources({...game, thumbnail:'/thumb.png'})).toEqual(['/thumb.png']);
  });
  it('tries a second Steam CDN when provider artwork fails', () => {
    expect(gameImageSources({...game, steamAppId:'534370', image:'https://example.com/broken.jpg', thumbnail:'https://example.com/broken.jpg'})).toEqual([
      'https://example.com/broken.jpg',
      'https://cdn.akamai.steamstatic.com/steam/apps/534370/header.jpg',
      'https://example.com/broken.jpg?lootradar_retry=1',
    ]);
  });
  it('does not invent artwork for missing or invalid Steam identifiers', () => {
    expect(gameImageSources({...game, steamAppId:'../../anything'})).toEqual([]);
  });
});

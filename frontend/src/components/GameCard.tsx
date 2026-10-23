import { useCurrency } from '../lib/currency';
import { Heart, ArrowUpRight, ThumbsUp, Gamepad2 } from 'lucide-react';
import type { Game } from '../lib/types';
import { gameImageSources } from '../lib/gameImages';

import { useState } from 'react';
const STORE_LOGOS: Record<string, string> = {
  '1': '/stores/steam.svg',
  '25': '/stores/epicgames.svg',
  '7': '/stores/gogdotcom.svg',
  'ms': '/stores/microsoft.svg',
};
export function StoreIcon({id, small = false}: {id: string; small?: boolean}) {
  return <span aria-hidden="true" className={`store-icon store-${id} ${small ? 'small' : ''}`}>
    {STORE_LOGOS[id] ? <img src={STORE_LOGOS[id]} alt="" width="24" height="24"/> : <Gamepad2 size={small ? 12 : 17}/>}
  </span>;
}
export function GameImage({game, className = ''}: {game: Game; className?: string}) {
  const sources = gameImageSources(game);
  // A different game or refreshed artwork must not inherit an earlier failure.
  return <GameArtwork key={JSON.stringify([game.id, sources])} title={game.title} sources={sources} className={className}/>;
}
function GameArtwork({title, sources, className}: {title: string; sources: string[]; className: string}) {
  const [index, setIndex] = useState(0);
  return index >= sources.length ? <div className={`image-fallback ${className}`}><Gamepad2 size={44}/><span>{title}</span></div>
    : <img key={sources[index]} className={className} src={sources[index]} alt={title} loading="lazy" decoding="async" referrerPolicy="no-referrer" onError={() => setIndex(value => value + 1)}/>;
}
export default function GameCard({game, saved, pending, onOpen, onSave, target}: {game: Game; saved: boolean; pending: boolean; onOpen: () => void; onSave: () => void; target?: number | null}) {
  const {money} = useCurrency();
  const best = game.offers[0];
  return <article className="game-card" data-testid="game-card">
    <div className="card-art"><button className="art-button" onClick={onOpen} aria-label={`Compare prices for ${game.title}`}><GameImage game={game}/></button>
      {best && best.discount > 0 && <span className={`discount-badge ${best.price === 0 ? 'free' : ''}`}>{best.price === 0 ? 'FREE' : `−${best.discount}%`}</span>}
      <button className={`save-button ${saved ? 'saved' : ''}`} onClick={onSave} disabled={pending} aria-label={`${saved ? 'Remove' : 'Save'} ${game.title}${saved ? ' from' : ' to'} wishlist`} aria-pressed={saved}><Heart size={17} fill={saved ? 'currentColor' : 'none'}/></button>
    </div>
    <div className="card-body"><button className="card-title" onClick={onOpen}>{game.title}</button>
      <div className="card-rating">{game.upcoming && <span>{game.releaseDate || 'Release date to be announced'}</span>}{game.reviewCount > 0 ? <><ThumbsUp size={12}/><span>{game.reviewPercent}% positive</span><span className="review-count">({new Intl.NumberFormat('en', {notation: 'compact'}).format(game.reviewCount)})</span></> : <span>Discover something new</span>}</div>
      <div className="card-bottom"><div className="store-label">{best ? <><StoreIcon id={best.storeId} small/><span>{best.storeName}</span>{game.offers.length > 1 && <span className="more-stores">+{game.offers.length - 1}</span>}</> : <span>{game.upcoming ? 'Coming soon' : 'No current offer'}</span>}</div>
        {best && <div className="card-price">{best.regularPrice > best.price && <s>{money(best.regularPrice)}</s>}<strong className={best.price === 0 ? 'lime' : ''}>{money(best.price)}</strong></div>}
      </div>
      {target != null && <div className={`target-note ${best && best.price <= target ? 'reached' : ''}`}>{best && best.price <= target ? '✓ Target reached' : `Target: ${money(target)}`}</div>}
      <button className="card-hover-action" onClick={onOpen}>Compare prices <ArrowUpRight size={15}/></button>
    </div>
  </article>;
}

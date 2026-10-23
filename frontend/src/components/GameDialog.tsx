import * as Dialog from '@radix-ui/react-dialog';
import { X, ExternalLink, Heart, TrendingDown, Clock3, LoaderCircle, AlertCircle, Check, ArrowUpRight } from 'lucide-react';
import { lazy, Suspense, useEffect, useState } from 'react';
import type { Detail, Game, Wish } from '../lib/types';
import { api } from '../lib/api';
import { money, timeAgo } from '../lib/deals';
import { GameImage, StoreIcon } from './GameCard';
const PriceChart = lazy(() => import('./PriceChart'));

export default function GameDialog({game, wish, onClose, onSave, onRemove}: {game: Game | null; wish?: Wish; onClose: () => void; onSave: (id: string, target: number | null) => Promise<void>; onRemove: (id: string) => Promise<void>}) {
  const [detail, setDetail] = useState<Detail | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [target, setTarget] = useState('');
  const [targetMessage, setTargetMessage] = useState('');
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    if (!game) return;
    let active = true; setDetail(null); setError(''); setTargetMessage('');
    api.detail(game.id).then(value => {if (active) setDetail(value);}).catch(e => {if (active) setError(e.message);});
    return () => {active = false;};
  }, [game?.id, retry]);
  useEffect(() => { setTarget(wish?.targetPrice?.toString() ?? ''); }, [game?.id, wish?.targetPrice]);
  const current = detail?.game ?? game;
  const best = current?.offers[0];
  async function saveTarget(event: React.FormEvent) {
    event.preventDefault(); if (!game) return;
    const amount = target.trim() === '' ? null : Number(target);
    if (amount !== null && (!Number.isFinite(amount) || amount < 0 || amount > 10000)) {setTargetMessage('Enter a price between $0 and $10,000.'); return;}
    setBusy(true); setTargetMessage('');
    try {await onSave(game.id, amount); setTargetMessage('Saved to your wishlist.');} catch {setTargetMessage('Could not save. Please try again.');} finally {setBusy(false);}
  }
  return <Dialog.Root open={!!game} onOpenChange={open => {if (!open) onClose();}}><Dialog.Portal><Dialog.Overlay className="dialog-overlay"/><Dialog.Content className="game-dialog">
    {current && <><div className="dialog-art"><GameImage game={current}/><div className="dialog-art-shade"/><span className="eyebrow"><TrendingDown size={14}/> THE RIGHT GAME. THE RIGHT PRICE.</span></div>
      <Dialog.Close className="dialog-close" aria-label="Close game details"><X size={20}/></Dialog.Close>
      <div className="dialog-content"><div className="dialog-heading"><div><Dialog.Title>{current.title}</Dialog.Title><Dialog.Description>Compare available offers across Steam, Epic Games, and GOG.</Dialog.Description></div><button className={`icon-button ${wish ? 'lime' : ''}`} aria-label={wish ? 'Remove from wishlist' : 'Save to wishlist'} disabled={busy} onClick={async () => {setBusy(true); try {if (wish) await onRemove(current.id); else await onSave(current.id, null);} catch {setTargetMessage('Could not update wishlist.');} finally {setBusy(false);}}}><Heart size={22} fill={wish ? 'currentColor' : 'none'}/></button></div>
      {!detail && !error && <div className="inline-loading"><LoaderCircle className="spin" size={16}/> Checking the latest store prices…</div>}
      {error && <div className="notice"><AlertCircle size={16}/>{error}<button onClick={() => setRetry(v => v + 1)}>Retry</button></div>}
      {detail && !detail.comparisonFresh && <div className="notice"><AlertCircle size={16}/>Live refresh unavailable. These are saved offers.</div>}
      <div className="offer-list">{current.offers.length === 0 ? <div className="notice">No current offers at the selected stores. Keep this game saved and check back.</div> : current.offers.map((offer, index) => <div className={`offer-row ${index === 0 ? 'best' : ''}`} key={offer.storeId}>
        <StoreIcon id={offer.storeId}/><div className="offer-store"><strong>{offer.storeName}</strong><small>{timeAgo(offer.checkedAt)}</small></div>{index === 0 && <span className="best-tag">BEST PRICE</span>}
        <div className="offer-price">{offer.regularPrice > offer.price && <s>{money(offer.regularPrice)}</s>}<strong>{money(offer.price)}</strong></div>
        <a className={index === 0 ? 'button primary small-button' : 'button secondary small-button'} href={offer.url} target="_blank" rel="noopener noreferrer">View deal <ArrowUpRight size={16}/></a>
      </div>)}</div>
      <p className="fine-print">{detail?.note ?? 'All prices in USD. Store links open through CheapShark.'}</p>
      <section className="history-panel"><div className="section-label"><h3><TrendingDown size={18}/> Price history</h3><span>Last 90 days</span></div>
        {detail ? <>{detail.history.length > 0 ? <Suspense fallback={<div className="chart-placeholder"/>}><PriceChart points={detail.history}/></Suspense> : <p className="muted">Price tracking will start with the next successful refresh.</p>}
        {detail.history.length === 1 && <p className="history-explanation"><Clock3 size={14}/> Tracking just started. The chart grows as we observe prices over time.</p>}
        {detail.providerHistoricalLow && <div className="historical-low"><span>Historical low across CheapShark’s tracked stores</span><strong>{money(detail.providerHistoricalLow.price)}</strong><small>{new Date(detail.providerHistoricalLow.date).toLocaleDateString(undefined, {month: 'short', year: 'numeric'})}</small></div>}</> : <div className="chart-placeholder"/>}
      </section>
      <form className="target-form" onSubmit={saveTarget}><div><h3>Make it a wishlist game</h3><p>Set a target and see when the price is right.</p></div><div className="target-controls"><label className="target-input"><span>$</span><input aria-label="Target price in USD" type="number" min="0" max="10000" step="0.01" placeholder={best ? (best.price * 0.8).toFixed(2) : '10.00'} value={target} onChange={e => setTarget(e.target.value)}/></label><button className="button primary" disabled={busy}>{busy ? <LoaderCircle className="spin" size={16}/> : <Check size={16}/>} Save target</button></div></form>
      {targetMessage && <p className="target-message" role="status">{targetMessage}</p>}
      <p className="fine-print"><Heart size={12}/> Your wishlist stays with this browser. Email alerts and account sync are coming later.</p>
      <a className="provider-link" href="https://www.cheapshark.com" target="_blank" rel="noopener noreferrer">Price data by CheapShark <ExternalLink size={12}/></a>
    </div></>}
  </Dialog.Content></Dialog.Portal></Dialog.Root>;
}

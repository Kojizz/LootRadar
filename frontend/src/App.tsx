import AccountDialog from './components/AccountDialog';
import { CurrencySelector } from './lib/currency';
import type { Session } from './lib/api';
import { useCurrency } from './lib/currency';
import { useEffect, useMemo, useRef, useState } from 'react';
import { Radar, Compass, Heart, ChartNoAxesCombined, Search, ArrowUpRight, ArrowRight, Sparkles, SlidersHorizontal, X, ChevronDown, CircleHelp, Check, Gift, LoaderCircle, RefreshCw, AlertCircle, Menu, CheckCheck, TrendingDown } from 'lucide-react';
import type { Catalog, Filters, Game, Wish } from './lib/types';
import { api } from './lib/api';
import { DEFAULT_FILTERS, filterGames, timeAgo } from './lib/deals';
import GameCard, { StoreIcon } from './components/GameCard';
import GameDialog from './components/GameDialog';

type Page = 'discover' | 'wishlist' | 'history';
export default function App() {
  const {money} = useCurrency();
  const [session, setSession] = useState<Session | null>(null);
  const [accountOpen, setAccountOpen] = useState(false);
  async function refreshAccount() {setSession(await api.session()); await loadWishlist();}
  useEffect(() => {void api.session().then(setSession).catch(() => {}); if (new URLSearchParams(location.search).get('login') === 'failed') setToast('Sign-in did not complete. Please try again.');}, []);
  const [catalog, setCatalog] = useState<Catalog | null>(null);
  const [wishes, setWishes] = useState<Wish[]>([]);
  const [error, setError] = useState('');
  const [wishlistError, setWishlistError] = useState('');
  const [filters, setFilters] = useState<Filters>(DEFAULT_FILTERS);
  const [page, setPage] = useState<Page>('discover');
  const [selected, setSelected] = useState<Game | null>(null);
  const [visible, setVisible] = useState(24);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [toast, setToast] = useState('');
  const [pending, setPending] = useState<string[]>([]);
  const [about, setAbout] = useState(false);
  const searchRef = useRef<HTMLInputElement>(null);

  async function load() {
    try {setCatalog(await api.catalog()); setError('');} catch {setError('The radar is having trouble connecting. Make sure the backend is running, then try again.');}
  }
  async function loadWishlist() {
    try {setWishes(await api.wishlist()); setWishlistError('');} catch {setWishlistError('Your wishlist could not be loaded.');}
  }
  useEffect(() => {void load(); void loadWishlist();}, []);
  useEffect(() => {const timer = window.setInterval(() => {void load(); void loadWishlist();}, catalog?.status.syncing || !catalog?.games.length ? 5000 : 60000); return () => clearInterval(timer);}, [catalog?.status.syncing, catalog?.games.length]);
  useEffect(() => {setVisible(24);}, [filters, page]);
  useEffect(() => {if (!toast) return; const timer = setTimeout(() => setToast(''), 3500); return () => clearTimeout(timer);}, [toast]);
  useEffect(() => {
    const handler = (event: KeyboardEvent) => {if (event.key === '/' && !(event.target instanceof HTMLInputElement) && !(event.target instanceof HTMLTextAreaElement)) {event.preventDefault(); searchRef.current?.focus();}};
    window.addEventListener('keydown', handler); return () => window.removeEventListener('keydown', handler);
  }, []);

  const games = catalog?.games ?? [];
  const savedIds = useMemo(() => new Set(wishes.map(w => w.game.id)), [wishes]);
  const sourceGames = page === 'wishlist' ? wishes.map(w => w.game) : games;
  const filtered = useMemo(() => filterGames(sourceGames, filters), [sourceGames, filters]);
  const unavailableWishes = page === 'wishlist' ? wishes.filter(w => !w.game.upcoming && filters.tab === 'all' && !w.game.offers.length && w.game.title.toLowerCase().includes(filters.query.toLowerCase())) : [];
  const featured = useMemo(() => [...games].filter(g => g.offers[0]?.discount >= 40 && g.reviewPercent >= 75 && g.steamAppId).sort((a, b) => b.offers[0].dealRating - a.offers[0].dealRating || b.reviewCount - a.reviewCount)[0] ?? games.find(g => g.offers.length > 0), [games]);
  const freeCount = games.filter(g => g.offers.some(o => o.price === 0)).length;
  const dealCount = games.reduce((sum, game) => sum + game.offers.filter(o => o.discount > 0).length, 0);
  const activeFilters = filters.query !== '' || filters.stores.length !== 4 || filters.maxPrice !== 100 || filters.tab !== 'all';
  const title = page === 'wishlist' ? 'Good things come to those who wishlist.' : page === 'history' ? 'Every price has a story.' : 'Good games. Better prices.';

  function navigate(next: Page) {setPage(next); setFilters(DEFAULT_FILTERS); setSidebarOpen(false); window.scrollTo({top: 0, behavior: 'smooth'});}
  function updateFilter<K extends keyof Filters>(key: K, value: Filters[K]) {setFilters(current => ({...current, [key]: value}));}
  function toggleStore(id: string) {updateFilter('stores', filters.stores.includes(id) ? filters.stores.filter(s => s !== id) : [...filters.stores, id]);}
  async function save(id: string, target: number | null = null) {
    setPending(p => [...p, id]);
    try {const wish = await api.save(id, target); setWishes(current => [wish, ...current.filter(w => w.game.id !== id)]); setToast(target == null ? 'Game added to your wishlist' : 'Target price saved');}
    catch (e) {setToast('Could not save this game. Please try again.'); throw e;}
    finally {setPending(p => p.filter(v => v !== id));}
  }
  async function remove(id: string) {
    setPending(p => [...p, id]);
    try {await api.remove(id); setWishes(current => current.filter(w => w.game.id !== id)); setToast('Game removed from your wishlist');}
    catch (e) {setToast('Could not update your wishlist. Please try again.'); throw e;}
    finally {setPending(p => p.filter(v => v !== id));}
  }
  function toggleWish(game: Game) {void (savedIds.has(game.id) ? remove(game.id) : save(game.id)).catch(() => {});}

  return <div className="app-shell">
    {sidebarOpen && <button className="sidebar-backdrop" aria-label="Close navigation" onClick={() => setSidebarOpen(false)}/>}
    <aside className={`sidebar ${sidebarOpen ? 'open' : ''}`}>
      <button className="brand" onClick={() => navigate('discover')} aria-label="LootRadar home"><img className="brand-logo" src="/branding/lootradar.png" alt="LootRadar" width="2172" height="724"/></button>
      <div className="sidebar-content"><div className="sidebar-eyebrow">YOUR NEXT GREAT FIND</div>
        <nav className="main-nav" aria-label="Main navigation">
          <button className={page === 'discover' ? 'active' : ''} onClick={() => navigate('discover')}><Compass size={19}/> Discover <span className="nav-dot"/></button>
          <button className={page === 'wishlist' ? 'active' : ''} onClick={() => navigate('wishlist')}><Heart size={19}/> Wishlist <span className="nav-count">{wishes.length}</span></button>
          <button className={page === 'history' ? 'active' : ''} onClick={() => navigate('history')}><ChartNoAxesCombined size={19}/> Price history</button>
        </nav>
        <div className="sidebar-divider"/><div className="filter-heading"><span>STORES</span><span>{filters.stores.length} selected</span></div>
        <div className="store-filters">{(catalog?.stores ?? [{id: '1', name: 'Steam'}, {id: '25', name: 'Epic Games'}, {id: '7', name: 'GOG'}, {id: 'ms', name: 'Microsoft / Xbox PC'}]).map(store => <label className="store-filter" key={store.id}><StoreIcon id={store.id}/><span>{store.name}</span><input type="checkbox" checked={filters.stores.includes(store.id)} onChange={() => toggleStore(store.id)}/><span className="custom-checkbox">{filters.stores.includes(store.id) && <Check size={11}/>}</span></label>)}</div>
        <div className="sidebar-divider"/><label className="filter-heading" htmlFor="price-range"><span>YOUR BUDGET</span><span>USD</span></label>
        <div className="budget-value">Up to <strong>${filters.maxPrice}</strong></div><input id="price-range" aria-label="Maximum price" className="price-range" type="range" min="0" max="100" value={filters.maxPrice} onChange={e => updateFilter('maxPrice', Number(e.target.value))} style={{'--range': `${filters.maxPrice}%`} as React.CSSProperties}/><div className="range-labels"><span>Free</span><span>$100</span></div>
        <button className="reset-filters" onClick={() => setFilters(DEFAULT_FILTERS)}><RefreshCw size={12}/> Reset filters</button>
        <div className="sidebar-tip"><span className="tip-icon"><Sparkles size={17}/></span><strong>A little patience pays off.</strong><p>Save the games you love.<br/>Find the price you’ll love, too.</p><button onClick={() => navigate('wishlist')}>Build your wishlist <ArrowRight size={14}/></button></div>
      </div>
      <div className="sidebar-footer"><button onClick={() => setAbout(v => !v)}><CircleHelp size={16}/> About LootRadar <ArrowUpRight size={13}/></button><span><i/> Made for the players.</span></div>
    </aside>

    <div className="main-shell"><header className="topbar"><button className="mobile-menu icon-button" aria-label="Open navigation" onClick={() => setSidebarOpen(true)}><Menu size={21}/></button><div className="breadcrumb">The good stuff <span>/</span> <strong>{page === 'discover' ? 'Discover' : page === 'wishlist' ? 'Wishlist' : 'Price history'}</strong></div>
      <label className="search-box"><Search size={17}/><input ref={searchRef} type="search" value={filters.query} onChange={e => updateFilter('query', e.target.value)} placeholder="Find your next game…" aria-label="Search games"/>{filters.query ? <button aria-label="Clear search" onClick={() => updateFilter('query', '')}><X size={14}/></button> : <kbd>/</kbd>}</label><CurrencySelector/><button className="account-button" onClick={() => setAccountOpen(true)} aria-label="Open account">{session?.authenticated ? session.name : 'Sign in'}</button>
    </header>

    <main><div className="page-intro"><div><div className="eyebrow intro-eyebrow"><span/> {page === 'discover' ? 'LESS SPENDING. MORE PLAYING.' : page === 'wishlist' ? 'YOUR PERSONAL COLLECTION' : 'A LITTLE PERSPECTIVE'}</div><h1>{title}</h1><p>{page === 'discover' ? 'Discover the best PC game deals, all in one place.' : page === 'wishlist' ? 'Your next adventures, saved for the right moment.' : 'Follow real price observations and find your moment to play.'}</p></div><div className={`sync-label ${catalog?.status.stale ? 'stale' : ''}`}><span className={catalog?.status.syncing ? 'pulse' : ''}/>{catalog?.status.syncing ? 'Refreshing prices…' : catalog?.status.stale ? 'Saved prices' : timeAgo(catalog?.status.lastSuccess ?? null)}</div></div>
      {error && <div className="notice error-notice"><AlertCircle size={18}/><span>{error}</span><button onClick={() => void load()}>Try again</button></div>}
      {catalog?.status.message && <div className="notice"><AlertCircle size={16}/><span>{catalog.status.message}</span></div>}
      {wishlistError && <div className="notice"><AlertCircle size={16}/><span>{wishlistError}</span><button onClick={() => void loadWishlist()}>Retry wishlist</button></div>}
      {about && <div className="about-panel"><button className="icon-button" onClick={() => setAbout(false)} aria-label="Close about"><X size={18}/></button><h2>A better way to find your next game.</h2><p>LootRadar compares Steam, Epic Games, GOG, and Microsoft / Xbox PC offers using CheapShark and Microsoft. The discovery feed features up to 60 top promotions per store and refreshes hourly while the server is running. Open a game for its full comparison at supported stores.</p><p>Prices are in USD for the US market. A free offer may be temporary; confirm the terms at the store. Guest wishlists stay in this browser. Sign in with Google or Steam to save a wishlist across devices. Currency conversions are estimates of US prices, not regional offers. Charts show observations collected since tracking began.</p><a href="https://www.cheapshark.com" target="_blank" rel="noopener noreferrer">Powered by CheapShark <ArrowUpRight size={14}/></a></div>}

      {page === 'discover' && !filters.query && <div className="hero-grid"><section className={`spotlight ${!featured ? 'skeleton' : ''}`}>
        {featured && <><img className="spotlight-image" src={featured.steamAppId ? `https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/${featured.steamAppId}/library_hero.jpg` : featured.image} alt="" onError={e => {if (e.currentTarget.src !== featured.image) e.currentTarget.src = featured.image;}}/><div className="spotlight-shade"/>
          <div className="spotlight-content"><div className="spotlight-kicker"><Sparkles size={13}/> ON OUR RADAR <span>PLAYER FAVORITE</span></div><div className="spotlight-bottom"><span className="spotlight-genre">A highly rated escape. An even better price.</span><h2>{featured.title}</h2><div className="spotlight-meta"><span className="hero-discount">−{featured.offers[0].discount}%</span><s>{money(featured.offers[0].regularPrice)}</s><strong>{money(featured.offers[0].price)}</strong><span className="hero-store"><StoreIcon id={featured.offers[0].storeId} small/>{featured.offers[0].storeName}</span></div><button className="button primary" onClick={() => setSelected(featured)}>Check out the deal <ArrowUpRight size={17}/></button></div></div><div className="spotlight-number">01 <span>/ THE SPOTLIGHT</span></div></>}
        {!featured && <div className="hero-loading"><Radar size={32}/><h2>Finding your next great game.</h2><p>{error ? 'Reconnect to start discovering.' : 'Checking the latest offers from your favorite stores…'}</p></div>}
      </section><section className="free-promo"><div className="promo-top"><span className="eyebrow">THE BEST PRICE IS FREE</span><Gift size={19}/></div><div className="promo-orbit" aria-hidden="true"><div className="orbit-ring outer"/><div className="orbit-ring inner"/><div className="gift-cube"><Gift size={43} strokeWidth={1.5}/></div><span className="orbit-dot one"/><span className="orbit-dot two"/><span className="orbit-plus">+</span></div><div className="promo-bottom"><h2>Zero dollars.<br/>All yours to discover.</h2><p>{freeCount ? `${freeCount} zero-price ${freeCount === 1 ? 'offer' : 'offers'} on the radar right now.` : 'Keep an eye out for the next free find.'}</p><button onClick={() => {updateFilter('tab', 'free'); document.getElementById('deals-section')?.scrollIntoView({behavior: 'smooth', block: 'start'});}}>Explore free games <ArrowRight size={16}/></button></div></section></div>}

      {page === 'discover' && !filters.query && <div className="trust-strip"><span><CheckCheck size={16}/><strong>{dealCount || '—'}</strong> offers on the radar</span><span><StoreIcon id="1" small/><strong>4</strong> stores. One destination.</span><span><RefreshCw size={14}/> Refreshed every hour</span><span className="trust-last"><Heart size={14}/> More game for your money.</span></div>}
      {page === 'history' && <div className="history-intro"><span className="history-intro-icon"><TrendingDown size={26}/></span><div><h2>Real prices, collected over time.</h2><p>Open a game below to see its observed price chart and CheapShark historical low. New charts start with a single point and grow with each hourly refresh.</p></div></div>}

      <section id="deals-section" className="deals-section"><div className="deals-heading"><div><h2>{filters.query ? `Results for “${filters.query}”` : page === 'wishlist' ? 'Your wishlist' : page === 'history' ? 'Explore price history' : 'Find your next obsession'}<span className="results-count">{filtered.length + unavailableWishes.length}</span></h2><p>{page === 'wishlist' ? 'Saved here, ready when you are. Set a target price inside any game.' : page === 'history' ? 'Choose a game to take a closer look.' : 'Great games. Small prices. A very happy backlog.'}</p></div><button className="filter-mobile button secondary" onClick={() => setSidebarOpen(true)}><SlidersHorizontal size={16}/> Filters</button></div>
        <div className="deals-toolbar"><div className="tabs" role="tablist" aria-label="Deal categories">{[{id: 'all', label: 'All deals'}, {id: 'sales', label: 'Top sales'}, {id: 'upcoming', label: 'Upcoming'}, {id: 'under10', label: 'Under $10'}, {id: 'big', label: '75% & up'}, {id: 'free', label: 'Free games'}].map(tab => <button key={tab.id} role="tab" aria-selected={filters.tab === tab.id} className={filters.tab === tab.id ? 'active' : ''} onClick={() => updateFilter('tab', tab.id)}>{tab.id === 'all' && <Sparkles size={13}/>} {tab.label}{tab.id === 'free' && freeCount > 0 && <span>{freeCount}</span>}</button>)}</div><label className="sort-select"><span>Sort by:</span><select aria-label="Sort games" value={filters.sort} onChange={e => updateFilter('sort', e.target.value)}><option value="best">Best deals</option><option value="price">Lowest price</option><option value="discount">Biggest discount</option><option value="rating">Top rated</option><option value="title">Name A–Z</option></select><ChevronDown size={13}/></label></div>
        {filters.tab === 'upcoming' && <p className="notice">Upcoming PC games from Steam. Release dates can change; prices and discounts appear only when available.</p>}
        {filters.tab === 'sales' && <p className="notice">Top sales: largest percentage discounts among these games.</p>}
        {activeFilters && <div className="active-filters"><span>{filtered.length} matching games</span>{filters.maxPrice !== 100 && <span className="filter-chip">Up to ${filters.maxPrice}<button aria-label="Clear budget filter" onClick={() => updateFilter('maxPrice', 100)}><X size={12}/></button></span>}<button onClick={() => setFilters(DEFAULT_FILTERS)}>Clear all filters <X size={12}/></button></div>}
        {!catalog && !error ? <div className="games-grid">{Array.from({length: 8}, (_, i) => <div className="skeleton skeleton-card" key={i}/>)}</div> : <>
          {filtered.length > 0 && <div className="games-grid">{filtered.slice(0, visible).map(game => <GameCard key={game.id} game={game} saved={savedIds.has(game.id)} pending={pending.includes(game.id)} onOpen={() => setSelected(game)} onSave={() => toggleWish(game)} target={page === 'wishlist' ? wishes.find(w => w.game.id === game.id)?.targetPrice : undefined}/>)}</div>}
          {unavailableWishes.length > 0 && <div className="unavailable-wishes"><h3>Waiting for another offer</h3>{unavailableWishes.map(w => <button key={w.game.id} onClick={() => setSelected(w.game)}>{w.game.title}<ArrowUpRight size={16}/></button>)}</div>}
          {filtered.length === 0 && unavailableWishes.length === 0 && <div className="empty-state">{page === 'wishlist' && wishes.length === 0 ? <Heart size={34}/> : catalog?.status.syncing ? <LoaderCircle size={34} className="spin"/> : <Search size={34}/>}<h3>{page === 'wishlist' && wishes.length === 0 ? 'Your next adventure starts with a heart.' : catalog?.status.syncing && !games.length ? 'Warming up the radar…' : 'No games on this frequency.'}</h3><p>{page === 'wishlist' && wishes.length === 0 ? 'Tap the heart on any game to save it here. Good deals are worth keeping.' : catalog?.status.syncing && !games.length ? 'The first live offers will appear in a moment.' : 'Try a different search, a bigger budget, or another store.'}</p>{page === 'wishlist' && wishes.length === 0 ? <button className="button primary" onClick={() => navigate('discover')}>Find something great <ArrowRight size={16}/></button> : activeFilters ? <button className="button secondary" onClick={() => setFilters(DEFAULT_FILTERS)}>Reset filters</button> : <button className="button secondary" onClick={() => void load()}>Check again</button>}</div>}
          {filtered.length > visible && <div className="load-more"><button className="button secondary" onClick={() => setVisible(v => v + 24)}>Discover more deals <ChevronDown size={15}/></button><span>Showing {Math.min(visible, filtered.length)} of {filtered.length} games</span></div>}
        </>}
      </section>
      <footer className="main-footer"><span><Radar size={15}/> A good deal is just the beginning.</span><div><a href="https://www.cheapshark.com" target="_blank" rel="noopener noreferrer">CheapShark & Microsoft <ArrowUpRight size={11}/></a><span>US market · Converted estimates</span><button onClick={() => {setAbout(true); window.scrollTo({top: 0, behavior: 'smooth'});}}>How it works</button></div></footer>
    </main></div>
    <AccountDialog open={accountOpen} onClose={() => setAccountOpen(false)} session={session} onRefresh={refreshAccount}/>
    <GameDialog game={selected} wish={wishes.find(w => w.game.id === selected?.id)} onClose={() => setSelected(null)} onSave={save} onRemove={remove}/>
    {toast && <div className="toast" role="status"><Check size={17}/>{toast}<button aria-label="Dismiss notification" onClick={() => setToast('')}><X size={14}/></button></div>}
  </div>;
}

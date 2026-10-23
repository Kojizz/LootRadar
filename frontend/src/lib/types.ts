export type Offer = {storeId: string; storeName: string; price: number; regularPrice: number; discount: number; dealRating: number; url: string; checkedAt: string};
export type Game = {id: string; title: string; steamAppId: string; image: string; thumbnail?: string; reviewPercent: number; reviewCount: number; reviewText: string; offers: Offer[]};
export type Store = {id: string; name: string};
export type Catalog = {games: Game[]; stores: Store[]; status: {lastSuccess: string | null; lastAttempt: string | null; syncing: boolean; stale: boolean; message: string | null}};
export type Wish = {game: Game; targetPrice: number | null; createdAt: string};
export type PricePoint = {observedAt: string; price: number};
export type Detail = {game: Game; history: PricePoint[]; providerHistoricalLow: {price: number; date: string} | null; comparisonFresh: boolean; note: string};
export type Filters = {query: string; stores: string[]; maxPrice: number; tab: string; sort: string};

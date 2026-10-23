import type { Catalog, Detail, Wish } from './types';
export type Session = {authenticated: boolean; name: string; googleEnabled: boolean; csrfToken: string; csrfHeader: string};
async function session(): Promise<Session> {
  const response = await fetch('/api/auth/session');
  if (!response.ok) throw new Error('Could not load sign-in status');
  return response.json();
}
const profileKey = 'lootradar-profile-v1';
function profileId() {
  let id = localStorage.getItem(profileKey);
  if (!id || !/^[0-9a-f-]{36}$/i.test(id)) { id = crypto.randomUUID(); localStorage.setItem(profileKey, id); }
  return id;
}
async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const csrf = options.method && !['GET','HEAD'].includes(options.method) ? await session() : null;
  const response = await fetch('/api' + path, {...options, headers: {'Content-Type': 'application/json', 'X-Profile-Id': profileId(), ...(csrf ? {[csrf.csrfHeader]: csrf.csrfToken} : {}), ...options.headers}});
  if (!response.ok) throw new Error(response.status === 404 ? 'This game is no longer available.' : 'Could not connect to LootRadar. Please try again.');
  if (response.status === 204) return undefined as T;
  return response.json();
}
export const api = {
  session,
  logout: () => request<void>('/auth/logout', {method: 'POST'}),
  importGuest: () => request<void>('/wishlist/import', {method: 'POST'}),
  catalog: () => request<Catalog>('/catalog'),
  detail: (id: string) => request<Detail>('/games/' + encodeURIComponent(id)),
  wishlist: () => request<Wish[]>('/wishlist'),
  save: (id: string, targetPrice: number | null = null) => request<Wish>('/wishlist/' + encodeURIComponent(id), {method: 'PUT', body: JSON.stringify({targetPrice})}),
  remove: (id: string) => request<void>('/wishlist/' + encodeURIComponent(id), {method: 'DELETE'}),
};

import type { Catalog, Detail, Wish } from './types';
const profileKey = 'lootradar-profile-v1';
function profileId() {
  let id = localStorage.getItem(profileKey);
  if (!id || !/^[0-9a-f-]{36}$/i.test(id)) { id = crypto.randomUUID(); localStorage.setItem(profileKey, id); }
  return id;
}
async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch('/api' + path, {...options, headers: {'Content-Type': 'application/json', 'X-Profile-Id': profileId(), ...options.headers}});
  if (!response.ok) throw new Error(response.status === 404 ? 'This game is no longer available.' : 'Could not connect to LootRadar. Please try again.');
  if (response.status === 204) return undefined as T;
  return response.json();
}
export const api = {
  catalog: () => request<Catalog>('/catalog'),
  detail: (id: string) => request<Detail>('/games/' + encodeURIComponent(id)),
  wishlist: () => request<Wish[]>('/wishlist'),
  save: (id: string, targetPrice: number | null = null) => request<Wish>('/wishlist/' + encodeURIComponent(id), {method: 'PUT', body: JSON.stringify({targetPrice})}),
  remove: (id: string) => request<void>('/wishlist/' + encodeURIComponent(id), {method: 'DELETE'}),
};

import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
type Currency = 'USD' | 'RUB' | 'KZT';
type Rates = {rates: Partial<Record<Currency, number>>; date: string; stale: boolean};
const Context = createContext({currency: 'USD' as Currency, setCurrency: (_: Currency) => {}, money: (v: number) => '$' + v, rates: {rates: {USD: 1}, date: '', stale: false} as Rates});
export function CurrencyProvider({children}: {children: ReactNode}) {
  const [currency, setCurrency] = useState<Currency>(() => (['USD','RUB','KZT'].includes(localStorage.getItem('lootradar-currency') ?? '') ? localStorage.getItem('lootradar-currency') : 'USD') as Currency);
  const [rates, setRates] = useState<Rates>({rates: {USD: 1}, date: '', stale: false});
  useEffect(() => {let active = true; fetch('/api/currencies').then(r => r.ok ? r.json() : Promise.reject()).then(data => {if (active) setRates(data);}).catch(() => {if (active) setRates({rates: {USD: 1}, date: '', stale: true});}); return () => {active = false;};}, []);
  const effective = rates.rates[currency] ? currency : 'USD';
  const money = (value: number) => value === 0 ? 'Free' : (effective !== 'USD' ? '≈ ' : '') + new Intl.NumberFormat('en-US', {style:'currency', currency: effective, maximumFractionDigits: 2}).format(value * (rates.rates[effective] ?? 1));
  return <Context.Provider value={{currency: effective, setCurrency: value => {localStorage.setItem('lootradar-currency', value); setCurrency(value);}, money, rates}}>{children}</Context.Provider>;
}
export const useCurrency = () => useContext(Context);
export function CurrencySelector() {
  const {currency, setCurrency, rates} = useCurrency();
  return <label className="currency-picker"><select aria-label="Display currency" value={currency} onChange={e => setCurrency(e.target.value as Currency)}>{(['USD','RUB','KZT'] as const).map(code => <option value={code} key={code} disabled={!rates.rates[code]}>{code === 'KZT' ? 'KZT · ₸' : code === 'RUB' ? 'RUB · ₽' : 'USD · $'}</option>)}</select><small title={rates.date ? `CBR exchange rates: ${rates.date}` : 'Exchange rates unavailable'}>{currency === 'USD' ? 'US prices' : `Estimate${rates.stale ? ' · cached' : ''}`}</small></label>;
}

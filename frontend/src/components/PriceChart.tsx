import { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip } from 'recharts';
import type { PricePoint } from '../lib/types';
import { money } from '../lib/deals';
export default function PriceChart({points}: {points: PricePoint[]}) {
  return <div className="price-chart" role="img" aria-label={'Observed lowest prices: ' + points.map(p => `${new Date(p.observedAt).toLocaleString()}: ${money(p.price)}`).join(', ')}>
    <ResponsiveContainer width="100%" height={190}>
      <AreaChart data={points.map(p => ({...p, time: new Date(p.observedAt).getTime()}))} margin={{top: 12, left: 0, right: 18, bottom: 0}}>
        <defs><linearGradient id="price-fill" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#c3f564" stopOpacity={0.25}/><stop offset="100%" stopColor="#c3f564" stopOpacity={0}/></linearGradient></defs>
        <CartesianGrid stroke="#2d302a" vertical={false} strokeDasharray="3 5"/>
        <XAxis dataKey="time" tickFormatter={v => new Date(v).toLocaleDateString(undefined, {month: 'short', day: 'numeric'})} tick={{fill: '#8f958b', fontSize: 11}} axisLine={false} tickLine={false} minTickGap={40}/>
        <YAxis tickFormatter={v => '$' + v} tick={{fill: '#8f958b', fontSize: 11}} axisLine={false} tickLine={false} width={45} domain={[0, 'auto']}/>
        <Tooltip contentStyle={{background: '#1c201a', border: '1px solid #3a4132', borderRadius: 10}} labelFormatter={v => new Date(Number(v)).toLocaleString()} formatter={v => [money(Number(v)), 'Lowest observed']}/>
        <Area type="stepAfter" dataKey="price" stroke="#c3f564" strokeWidth={2} fill="url(#price-fill)" dot={{r: 4, fill: '#c3f564', strokeWidth: 0}}/>
      </AreaChart>
    </ResponsiveContainer>
  </div>;
}

import type { AlertDetail } from '../types';
export function ChainView({ alert }: { alert: AlertDetail }) {
  const hops = alert.hops;
  const spanMin = hops.length > 1 ? Math.round((new Date(hops[hops.length-1].occurredAt).getTime() - new Date(hops[0].occurredAt).getTime()) / 60000) : 0;
  return (
    <section aria-label="chain view" data-testid="chain-view">
      <header>
        <h2>Alert #{alert.alertId} — {alert.status}</h2>
        <p>Total span: {spanMin} minutes · End-to-end retention: {(alert.endToEndRetention * 100).toFixed(1)}%</p>
        <p>{alert.startAt} → {alert.endAt}</p>
      </header>
      <div style={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 4 }} data-testid="chain-nodes">
        {hops.map((h, i) => (
          <span key={h.transferId} style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            {i === 0 && <span data-testid="account-node">{h.fromAccount}</span>}
            <span data-testid="chain-arrow" aria-label={`hop ${i+1}`}>
              → <small>{h.amount.toLocaleString('en-IN')} {i > 0 ? `· gap ${h.gapMinutes} min · ${(h.hopRetention! * 100).toFixed(1)}%` : ''}</small>
            </span>
            <span data-testid="account-node">{h.toAccount}</span>
          </span>
        ))}
      </div>
      <table><thead><tr><th>#</th><th>From</th><th>To</th><th>Amount</th><th>Occurred</th><th>Gap (min)</th><th>Hop retention</th></tr></thead>
      <tbody>{hops.map((h,i)=>(<tr key={h.transferId}><td>{i+1}</td><td>{h.fromAccount}</td><td>{h.toAccount}</td><td>{h.amount}</td><td>{h.occurredAt}</td><td>{h.gapMinutes ?? '—'}</td><td>{h.hopRetention != null ? (h.hopRetention*100).toFixed(1)+'%' : '—'}</td></tr>))}</tbody></table>
    </section>
  );
}

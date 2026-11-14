import type { AlertSummary } from '../types';
export function AlertList({ alerts, onSelect, selectedId }: { alerts: AlertSummary[]; onSelect: (id: number) => void; selectedId?: number }) {
  if (alerts.length === 0) return <p>No alerts for this status.</p>;
  return <ul data-testid="alert-list">{alerts.map(a => (
    <li key={a.alertId} onClick={() => onSelect(a.alertId)} style={{ cursor: 'pointer', fontWeight: a.alertId === selectedId ? 700 : 400 }}>
      <strong>#{a.alertId}</strong> {a.status} · {a.hops} hops · first ₹{a.firstAmount.toLocaleString('en-IN')} · e2e {(a.endToEndRetention*100).toFixed(1)}% · {a.maskedPath}
    </li>))}</ul>;
}

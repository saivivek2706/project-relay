import type { AlertDetail, AlertSummary, AuditRow } from './types';
const base = '';
export async function listAlerts(status: string): Promise<{ content: AlertSummary[]; totalElements: number }> {
  const r = await fetch(`${base}/api/alerts?status=${encodeURIComponent(status)}&page=0&size=100`);
  if (!r.ok) throw new Error('failed to load alerts');
  return r.json();
}
export async function getAlert(id: number): Promise<AlertDetail> { const r = await fetch(`${base}/api/alerts/${id}`); if (!r.ok) throw new Error('not found'); return r.json(); }
export async function getAudit(id: number): Promise<AuditRow[]> { const r = await fetch(`${base}/api/alerts/${id}/audit`); if (!r.ok) throw new Error('audit failed'); return r.json(); }
export async function patchStatus(id: number, status: string, note: string, actor: string) {
  const r = await fetch(`${base}/api/alerts/${id}/status`, { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ status, note, actor }) });
  if (r.status === 409) throw new Error('Invalid transition (409)');
  if (!r.ok) throw new Error('status change failed');
  return r.json();
}

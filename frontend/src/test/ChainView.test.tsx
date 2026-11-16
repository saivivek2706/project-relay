import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ChainView } from '../components/ChainView';
import type { AlertDetail } from '../types';
const alert: AlertDetail = { alertId: 1, status: 'OPEN', endToEndRetention: 0.94, startAt: '2026-01-01T09:00:00Z', endAt: '2026-01-01T09:15:00Z', hops: [
  { transferId: 'T1', fromAccount: 'XXXXXX0001', toAccount: 'XXXXXX0002', amount: 200000, occurredAt: '2026-01-01T09:00:00Z' },
  { transferId: 'T2', fromAccount: 'XXXXXX0002', toAccount: 'XXXXXX0003', amount: 194000, occurredAt: '2026-01-01T09:05:00Z', gapMinutes: 5, hopRetention: 0.97 },
  { transferId: 'T3', fromAccount: 'XXXXXX0003', toAccount: 'XXXXXX0004', amount: 188000, occurredAt: '2026-01-01T09:10:00Z', gapMinutes: 5, hopRetention: 0.9691 },
  { transferId: 'T4', fromAccount: 'XXXXXX0004', toAccount: 'XXXXXX0005', amount: 182000, occurredAt: '2026-01-01T09:15:00Z', gapMinutes: 5, hopRetention: 0.9681 },
]};
describe('ChainView', () => {
  it('shows 4 hops and only masked account numbers', () => {
    const { container } = render(<ChainView alert={alert} />);
    expect(screen.getAllByTestId('chain-arrow')).toHaveLength(4);
    expect(container.textContent).toContain('XXXXXX0001');
    const accounts = Array.from(container.querySelectorAll('[data-testid="account-node"]')).map(node => node.textContent ?? '');
    expect(accounts).toHaveLength(5);
    expect(accounts.every(account => /^XXXXXX\d{4}$/.test(account))).toBe(true);
    expect(accounts.join(' ')).not.toMatch(/\d{10,16}/);
    expect(container.textContent).toContain('Total span: 15 minutes');
    expect(container.textContent).toContain('94.0%');
  });
});

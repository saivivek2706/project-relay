export type AlertSummary = { alertId: number; hops: number; firstAmount: number; lastAmount: number; endToEndRetention: number; startAt: string; endAt: string; status: string; maskedPath: string; };
export type Hop = { transferId: string; fromAccount: string; toAccount: string; amount: number; occurredAt: string; gapMinutes?: number; hopRetention?: number; };
export type AlertDetail = { alertId: number; hops: Hop[]; status: string; endToEndRetention: number; startAt: string; endAt: string; };
export type AuditRow = { auditId: number; alertId: number; fromStatus: string; toStatus: string; actor: string; note: string; at: string; };

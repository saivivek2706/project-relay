CREATE TABLE transfers (
 transfer_id VARCHAR(100) PRIMARY KEY,
 from_account VARCHAR(16) NOT NULL,
 to_account VARCHAR(16) NOT NULL,
 amount NUMERIC(19,2) NOT NULL,
 occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_transfers_to_occurred ON transfers(to_account, occurred_at);
CREATE INDEX idx_transfers_from_occurred ON transfers(from_account, occurred_at);
CREATE TABLE alerts (
 alert_id BIGSERIAL PRIMARY KEY,
 hops INT NOT NULL,
 first_amount NUMERIC(19,2) NOT NULL,
 last_amount NUMERIC(19,2) NOT NULL,
 end_to_end_retention NUMERIC(10,4) NOT NULL,
 start_at TIMESTAMPTZ NOT NULL,
 end_at TIMESTAMPTZ NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 chain_transfer_ids TEXT NOT NULL
);
CREATE INDEX idx_alerts_status_updated ON alerts(status, updated_at DESC);
CREATE TABLE alert_audit (
 audit_id BIGSERIAL PRIMARY KEY,
 alert_id BIGINT NOT NULL REFERENCES alerts(alert_id),
 from_status VARCHAR(20), to_status VARCHAR(20) NOT NULL,
 actor VARCHAR(100), note TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Persist the original ingest decision so an idempotent replay is stable even after later chain extensions.
ALTER TABLE transfers ADD COLUMN decision VARCHAR(10) NOT NULL DEFAULT 'CLEAR';
ALTER TABLE transfers ADD COLUMN alert_id BIGINT REFERENCES alerts(alert_id);

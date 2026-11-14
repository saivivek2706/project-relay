CREATE TABLE transfers (
    transfer_id VARCHAR(100) PRIMARY KEY,
    from_account VARCHAR(16) NOT NULL,
    to_account VARCHAR(16) NOT NULL,
    amount NUMERIC(10,2) NOT NULL CHECK (amount > 0),
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_transfer_accounts_differ CHECK (from_account <> to_account)
);

-- Equality on to_account is the leading lookup predicate; occurred_at then narrows
-- the matching inbound edges to the requested time range and supports newest-first traversal.
CREATE INDEX idx_transfers_to_account_occurred_at
    ON transfers (to_account, occurred_at DESC);

CREATE TABLE alerts (
    alert_id UUID PRIMARY KEY,
    hops INTEGER NOT NULL,
    first_amount NUMERIC(10,2) NOT NULL,
    last_amount NUMERIC(10,2) NOT NULL,
    end_to_end_retention NUMERIC(5,4) NOT NULL,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE alert_transfers (
    alert_id UUID NOT NULL REFERENCES alerts(alert_id),
    transfer_id VARCHAR(100) NOT NULL REFERENCES transfers(transfer_id),
    hop_order INTEGER NOT NULL,
    PRIMARY KEY (alert_id, hop_order),
    UNIQUE (alert_id, transfer_id)
);

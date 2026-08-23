ALTER TABLE landing_lead ADD COLUMN IF NOT EXISTS name VARCHAR(255);
ALTER TABLE landing_lead ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'NEW';
ALTER TABLE landing_lead ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(1000);
ALTER TABLE landing_lead ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW();

UPDATE landing_lead
SET name = NULLIF(TRIM(CONCAT_WS(' ', first_name, last_name)), '')
WHERE name IS NULL;

ALTER TABLE landing_lead ALTER COLUMN first_name DROP NOT NULL;
ALTER TABLE landing_lead ALTER COLUMN last_name DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_landing_lead_status ON landing_lead (status);

CREATE TABLE IF NOT EXISTS telegram_subscriber (
    chat_id       BIGINT PRIMARY KEY,
    username      VARCHAR(255),
    full_name     VARCHAR(255),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    subscribed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_telegram_subscriber_active
    ON telegram_subscriber (active) WHERE active = TRUE;

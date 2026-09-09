CREATE TABLE IF NOT EXISTS integration_settings (
    id                          SMALLINT PRIMARY KEY CHECK (id = 1),
    telegram_bot_token          TEXT,
    telegram_bot_username       VARCHAR(64),
    telegram_polling_enabled    BOOLEAN,
    eskiz_email                 VARCHAR(255),
    eskiz_password              TEXT,
    eskiz_from                  VARCHAR(64),
    updated_at                  TIMESTAMP,
    updated_by                  BIGINT
);

INSERT INTO integration_settings (id)
VALUES (1)
ON CONFLICT (id) DO NOTHING;

COMMENT ON TABLE integration_settings IS
    'Singleton integration overrides. NULL values use application/environment fallbacks.';

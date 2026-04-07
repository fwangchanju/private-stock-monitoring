ALTER TABLE market_overview
    ADD COLUMN upper_limit_count INT NOT NULL DEFAULT 0,
    ADD COLUMN lower_limit_count INT NOT NULL DEFAULT 0;

ALTER TABLE market_overview_snapshot
    ADD COLUMN upper_limit_count INT NOT NULL DEFAULT 0,
    ADD COLUMN lower_limit_count INT NOT NULL DEFAULT 0;

ALTER TABLE program_trading_ranking_snapshot
    ADD COLUMN market_type VARCHAR(20) NOT NULL DEFAULT 'KOSPI',
    ADD COLUMN amt_qty_type VARCHAR(20) NOT NULL DEFAULT 'AMOUNT';

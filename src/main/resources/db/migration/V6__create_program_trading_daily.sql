CREATE TABLE program_trading_daily (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_code VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,
    program_buy_amount DECIMAL(19,2) NOT NULL,
    program_sell_amount DECIMAL(19,2) NOT NULL,
    program_net_buy_amount DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_program_trading_daily PRIMARY KEY (id),
    CONSTRAINT uk_program_trading_daily UNIQUE (stock_code, trade_date),
    CONSTRAINT fk_program_trading_daily_stock FOREIGN KEY (stock_code) REFERENCES stock_master (stock_code)
);

CREATE INDEX idx_program_trading_daily_stock_date ON program_trading_daily (stock_code, trade_date DESC);

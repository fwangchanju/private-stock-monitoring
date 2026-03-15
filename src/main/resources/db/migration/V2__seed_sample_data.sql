INSERT INTO app_user (id, user_key, display_name, telegram_chat_id, timezone, created_at, updated_at)
VALUES (1, 'default', 'Default User', NULL, 'Asia/Seoul', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO user_notification_setting (id, user_id, reminder_enabled, reminder_time, timezone, created_at, updated_at)
VALUES (1, 1, TRUE, TIME '10:00:00', 'Asia/Seoul', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO stock_master (stock_code, stock_name, market_type, active, created_at, updated_at) VALUES
('005930', 'Samsung Electronics', 'KOSPI', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('000660', 'SK Hynix', 'KOSPI', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('035420', 'NAVER', 'KOSPI', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('068270', 'Celltrion', 'KOSPI', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('251270', 'Netmarble', 'KOSPI', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO watch_stock (user_id, stock_code, display_order, created_at) VALUES
(1, '005930', 1, CURRENT_TIMESTAMP),
(1, '000660', 2, CURRENT_TIMESTAMP),
(1, '035420', 3, CURRENT_TIMESTAMP);

INSERT INTO market_overview (market_type, snapshot_time, last_collected_at, market_status, index_value, change_value, change_rate, trading_value, advancers, decliners, unchanged_count, created_at, updated_at) VALUES
('KOSPI', TIMESTAMP '2026-03-13 10:00:00', TIMESTAMP '2026-03-13 10:00:25', 'STRONG', 2688.2300, 18.1200, 0.6800, 532000000000.00, 531, 298, 69, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('KOSDAQ', TIMESTAMP '2026-03-13 10:00:00', TIMESTAMP '2026-03-13 10:00:30', 'MIXED', 874.5100, -1.4300, -0.1600, 318000000000.00, 617, 781, 104, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO market_overview_snapshot (market_type, snapshot_time, last_collected_at, market_status, index_value, change_value, change_rate, trading_value, advancers, decliners, unchanged_count, created_at)
SELECT market_type, snapshot_time, last_collected_at, market_status, index_value, change_value, change_rate, trading_value, advancers, decliners, unchanged_count, CURRENT_TIMESTAMP
FROM market_overview;

INSERT INTO investor_trading_summary (market_type, investor_type, snapshot_time, last_collected_at, buy_amount, sell_amount, net_buy_amount, created_at, updated_at) VALUES
('KOSPI', 'PERSONAL', TIMESTAMP '2026-03-13 10:00:00', TIMESTAMP '2026-03-13 10:00:20', 182000000000.00, 205000000000.00, -23000000000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('KOSPI', 'FOREIGNER', TIMESTAMP '2026-03-13 10:00:00', TIMESTAMP '2026-03-13 10:00:20', 240000000000.00, 208000000000.00, 32000000000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('KOSPI', 'INSTITUTION', TIMESTAMP '2026-03-13 10:00:00', TIMESTAMP '2026-03-13 10:00:20', 154000000000.00, 161000000000.00, -7000000000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO investor_trading_summary_snapshot (market_type, investor_type, snapshot_time, last_collected_at, buy_amount, sell_amount, net_buy_amount, created_at)
SELECT market_type, investor_type, snapshot_time, last_collected_at, buy_amount, sell_amount, net_buy_amount, CURRENT_TIMESTAMP
FROM investor_trading_summary;

INSERT INTO intraday_investor_ranking_snapshot (market_type, investor_type, ranking_type, rank_no, stock_code, stock_name, net_buy_amount, traded_volume, snapshot_time, created_at) VALUES
('KOSPI', 'FOREIGNER', 'NET_BUY', 1, '005930', 'Samsung Electronics', 12800000000.00, 1820000, TIMESTAMP '2026-03-13 10:00:00', CURRENT_TIMESTAMP),
('KOSPI', 'FOREIGNER', 'NET_BUY', 2, '000660', 'SK Hynix', 11600000000.00, 930000, TIMESTAMP '2026-03-13 10:00:00', CURRENT_TIMESTAMP),
('KOSPI', 'FOREIGNER', 'NET_BUY', 3, '035420', 'NAVER', 4200000000.00, 315000, TIMESTAMP '2026-03-13 10:00:00', CURRENT_TIMESTAMP);

INSERT INTO program_trading_ranking_snapshot (ranking_type, rank_no, stock_code, stock_name, program_buy_amount, program_sell_amount, program_net_buy_amount, snapshot_time, created_at) VALUES
( 'NET_BUY', 1, '005930', 'Samsung Electronics', 44500000000.00, 30700000000.00, 13800000000.00, TIMESTAMP '2026-03-13 10:00:00', CURRENT_TIMESTAMP),
( 'NET_BUY', 2, '000660', 'SK Hynix', 21200000000.00, 13400000000.00, 7800000000.00, TIMESTAMP '2026-03-13 10:00:00', CURRENT_TIMESTAMP),
( 'NET_BUY', 3, '035420', 'NAVER', 9700000000.00, 6200000000.00, 3500000000.00, TIMESTAMP '2026-03-13 10:00:00', CURRENT_TIMESTAMP);

INSERT INTO index_contribution_ranking_snapshot (market_type, rank_no, stock_code, stock_name, contribution_score, price_change_rate, snapshot_time, created_at) VALUES
('KOSPI', 1, '005930', 'Samsung Electronics', 4.8200, 1.3200, TIMESTAMP '2026-03-13 10:00:00', CURRENT_TIMESTAMP),
('KOSPI', 2, '000660', 'SK Hynix', 3.9100, 2.1800, TIMESTAMP '2026-03-13 10:00:00', CURRENT_TIMESTAMP),
('KOSPI', 3, '068270', 'Celltrion', 1.7700, 1.0500, TIMESTAMP '2026-03-13 10:00:00', CURRENT_TIMESTAMP);

INSERT INTO program_trading_history (stock_code, snapshot_time, program_buy_amount, program_sell_amount, program_net_buy_amount, created_at) VALUES
('005930', TIMESTAMP '2026-03-13 09:50:00', 32100000000.00, 29200000000.00, 2900000000.00, CURRENT_TIMESTAMP),
('005930', TIMESTAMP '2026-03-13 10:00:00', 44500000000.00, 30700000000.00, 13800000000.00, CURRENT_TIMESTAMP),
('000660', TIMESTAMP '2026-03-13 09:50:00', 14100000000.00, 9800000000.00, 4300000000.00, CURRENT_TIMESTAMP),
('000660', TIMESTAMP '2026-03-13 10:00:00', 21200000000.00, 13400000000.00, 7800000000.00, CURRENT_TIMESTAMP),
('035420', TIMESTAMP '2026-03-13 09:50:00', 6100000000.00, 5000000000.00, 1100000000.00, CURRENT_TIMESTAMP),
('035420', TIMESTAMP '2026-03-13 10:00:00', 9700000000.00, 6200000000.00, 3500000000.00, CURRENT_TIMESTAMP);

INSERT INTO short_selling_history (stock_code, trade_date, short_volume, short_amount, short_ratio, created_at) VALUES
('005930', DATE '2026-03-11', 482000, 35100000000.00, 2.1800, CURRENT_TIMESTAMP),
('005930', DATE '2026-03-12', 501000, 37200000000.00, 2.2600, CURRENT_TIMESTAMP),
('005930', DATE '2026-03-13', 445000, 33300000000.00, 2.0100, CURRENT_TIMESTAMP),
('000660', DATE '2026-03-11', 190000, 28200000000.00, 1.8900, CURRENT_TIMESTAMP),
('000660', DATE '2026-03-12', 204000, 30400000000.00, 1.9700, CURRENT_TIMESTAMP),
('000660', DATE '2026-03-13', 178000, 26500000000.00, 1.7400, CURRENT_TIMESTAMP);

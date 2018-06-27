-- Insert NSE into exchanges table.
INSERT INTO exchanges (title, code) VALUES ('National Stock Exchange of India', 'NSE');

-- Insert indexes nifty50, next50, midcap50
INSERT INTO stock_indexes (index_name, exchange_id) VALUES ('NIFTY 50', 1);
INSERT INTO stock_indexes (index_name, exchange_id) VALUES ('NIFTY NEXT 50', 1);
INSERT INTO stock_indexes (index_name, exchange_id) VALUES ('NIFTY MIDCAP 50', 1);

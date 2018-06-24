-- Insert NSE into exchanges table.
INSERT INTO exchanges (title, code) VALUES ('National Stock Exchange of India', 'NSE');

-- Insert indexes nifty50, next50, midcap50
INSERT INTO stock_indexes (index_name, exchange_id) VALUES ('nifty50', 1);
INSERT INTO stock_indexes (index_name, exchange_id) VALUES ('next50', 1);
INSERT INTO stock_indexes (index_name, exchange_id) VALUES ('midcap50', 1);

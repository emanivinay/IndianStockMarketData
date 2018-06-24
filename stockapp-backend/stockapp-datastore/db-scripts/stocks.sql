-- Create the exchange table.
CREATE TABLE exchanges (exchange_id serial primary key,
					    title varchar(256) not null,
					    code varchar(256) unique not null);

-- Create the stocks table.
CREATE TABLE stocks (stock_id serial primary key,
					 exchange_id integer references exchanges(exchange_id),
					 symbol varchar(32) not null,
					 open real default -1.0,
                     volume integer default 0,
					 ltp real default -1.0,
					 high real default -1.0,
					 low real default -1.0,
					 prev_close real default -1.0);

-- Create the stock index table, This is to be maintained manually.
CREATE TABLE stock_indexes (stock_index_id serial primary key,
                          exchange_id integer references exchanges(exchange_id),
                          index_name varchar(128) not null);

-- Create the index-stock listing table which stores the list of constituent
-- stocks for each index.
CREATE TABLE index_listings (index_listing_id serial primary key,
                            index_id integer references stock_indexes(stock_index_id),
                            stock_id integer references stocks(stock_id));

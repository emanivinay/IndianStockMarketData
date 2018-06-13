-- Create the user table. This also creates 2 indices on this table, 
-- one by user_id cause it's the primary key and another by username
-- cause of the unique constraint.
CREATE TABLE users (user_id serial primary key,
                    password_hash varchar(256) not null,
                    password_salt varchar(256) not null,
                    username varchar(64) unique not null,
                    date_created timestamp without time zone not null);

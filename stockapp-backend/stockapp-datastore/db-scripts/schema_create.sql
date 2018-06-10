-- Create the user table
CREATE TABLE users (user_id serial primary key,
                    password_hash varchar(256) not null,
                    password_salt varchar(256) not null,
                    username varchar(64) not null,
                    date_created timestamp without time zone not null);

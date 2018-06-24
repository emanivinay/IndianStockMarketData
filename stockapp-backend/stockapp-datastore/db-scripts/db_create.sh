# This script needs to be run as `postgres` user. All tables and schemas
# created will be owned by `postgres`.

[[ -z $DBNAME ]] && echo "DBNAME variable not set, exiting" && exit;

# First create the role and database.
psql -U postgres -v dbname=$DBNAME  -f db_create.sql

# Now create user data tables
psql -U postgres -d $DBNAME -f users.sql; 

# Now create stock data tables
psql -U postgres -d $DBNAME -f stocks.sql;

# Populate initial data in tables.
psql -U postgres -d $DBNAME -f data.sql;

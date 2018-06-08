# This script needs to be run as `postgres` user. All tables and schemas
# created will be owned by `postgres`.

# First create the role and database.
psql -U postgres -v dbname=$DBNAME  -f db_create.sql

# Now create database tables
psql -U postgres -d $DBNAME -f schema_create.sql; 

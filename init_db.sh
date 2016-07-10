#!/bin/bash
#
# Example usage:
#   ./init_db.sh sqlite /tmp/cube.db
#   export PGDATABASE=cube; ./init_db.sh postgres

RDBMS="$1"

if [ "$RDBMS" == "sqlite" ] ; then
    SQLITE_FILENAME="$2"
    if [ -f $SQLITE_FILENAME ] ; then
        rm $SQLITE_FILENAME
    fi
    sed 's/${auto_increment_type}/INTEGER/' cube.sql | sqlite3 $DB_NAME
elif [ "$RDBMS" == "postgres" ] ; then
    DB_NAME=${PGDATABASE}
    export PGDATABASE=
    psql -c "DROP DATABASE $DB_NAME"
    psql -c "CREATE DATABASE $DB_NAME"
    export PGDATABASE="$DB_NAME"
    sed 's/${auto_increment_type}/SERIAL/' cube.sql | psql
else
    echo Unknown RDBMS $RDBMS
    exit 1
fi

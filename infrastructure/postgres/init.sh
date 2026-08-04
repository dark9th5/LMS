#!/usr/bin/env bash
set -euo pipefail
: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${POSTGRES_SERVICE_PASSWORD:?POSTGRES_SERVICE_PASSWORD is required}"

schemas=(identity organization course enrollment learning assessment grading reporting file_storage license audit notification certificate ai configuration integration operations competency)
for schema in "${schemas[@]}"; do
  role="${schema}_user"
  psql -v ON_ERROR_STOP=1 \
    --username "$POSTGRES_USER" \
    --dbname "$POSTGRES_DB" \
    --set=role_name="$role" \
    --set=schema_name="$schema" \
    --set=role_password="$POSTGRES_SERVICE_PASSWORD" <<'EOSQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'role_name', :'role_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'role_name') \gexec
SELECT format('ALTER ROLE %I PASSWORD %L', :'role_name', :'role_password') \gexec
SELECT format('CREATE SCHEMA IF NOT EXISTS %I AUTHORIZATION %I', :'schema_name', :'role_name') \gexec
SELECT format('ALTER SCHEMA %I OWNER TO %I', :'schema_name', :'role_name') \gexec
SELECT format('ALTER ROLE %I SET search_path TO %I', :'role_name', :'schema_name') \gexec
EOSQL
done

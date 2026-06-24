-- ============================================================
-- VitalLoop — PostgreSQL initialization script
-- Runs once when the postgres container is first created.
-- ============================================================

-- Operational schema: used by application services for
-- non-clinical relational data (users, alerts, audit logs, etc.).
-- FHIR clinical data lives in the HAPI FHIR server / GCP FHIR store.
CREATE SCHEMA IF NOT EXISTS operational;

-- Grant the application user full access to the operational schema.
-- ${POSTGRES_USER} is resolved to the value supplied via docker-compose env.
DO $$
BEGIN
  EXECUTE format(
    'GRANT ALL PRIVILEGES ON SCHEMA operational TO %I',
    current_user
  );
END
$$;

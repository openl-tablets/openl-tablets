-- The texts of the audit log are searched by what they contain, in any case: lower(field) LIKE '%text%'. Only a
-- trigram index of the pg_trgm extension serves such a search. The extension is created where the database user may
-- create it; otherwise the search reads the table, as it does on the other databases, and the migration goes on.
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS pg_trgm;
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'The audit log is searched without trigram indexes: %', SQLERRM;
END
$$;

-- The operator class is named by the schema the extension is installed in, which need not be on the search path.
DO $$
DECLARE
    trgm_schema TEXT;
    field       TEXT;
BEGIN
    SELECT n.nspname
    INTO trgm_schema
    FROM pg_extension e
             JOIN pg_namespace n ON n.oid = e.extnamespace
    WHERE e.extname = 'pg_trgm';
    IF trgm_schema IS NOT NULL THEN
        FOREACH field IN ARRAY ARRAY ['actor', 'target', 'subject', 'clientAddress']
            LOOP
                EXECUTE format('CREATE INDEX ix_OpenL_Audit_Log_%s_search ON OpenL_Audit_Log'
                                   || ' USING gin (lower(%s) %I.gin_trgm_ops)', field, field, trgm_schema);
            END LOOP;
    END IF;
END
$$;

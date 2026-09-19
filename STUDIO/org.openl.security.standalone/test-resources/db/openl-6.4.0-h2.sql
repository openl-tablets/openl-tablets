-- What OpenL Tablets 6.4.0 changed in an H2 database of 6.0.0. Layered on openl-6.0.0-h2.sql, it
-- reproduces a database handed over by 6.4.0, the last release that migrated with the previous tool.

ALTER TABLE OpenL_Users ADD lastLogin TIMESTAMP;

INSERT INTO "openl_security_flyway" ("installed_rank", "version", "description", "type", "script", "checksum", "installed_by", "execution_time", "success")
VALUES (22, '16', 'V16__User_last_login.sql', 'SQL', 'V16__User_last_login.sql', 0, 'openl', 0, TRUE);

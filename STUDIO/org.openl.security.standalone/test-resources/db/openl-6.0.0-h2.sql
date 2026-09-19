-- What OpenL Tablets 6.0.0 changed in an H2 database of 5.27.15. Layered on openl-5.27.15-h2.sql, it
-- reproduces a database handed over by 6.0.0: the statements are the ones the migration tool of that
-- release ran, and the rows it appended to its history table.

-- Permission masks of the previous model, where each privilege had a mask of its own:
-- 16777217 view, 67108884 edit, 33554448 create, 50331664 add, 83886088 delete, 100663328 erase,
-- 184549376 deploy, 201326592 run, 218103808 benchmark.

-- Erasing became deleting, and adding became creating.
DELETE FROM acl_entry
WHERE mask = 100663328
  AND EXISTS (SELECT 1 FROM acl_entry AS other
              WHERE other.acl_object_identity = acl_entry.acl_object_identity
                AND other.sid = acl_entry.sid AND other.mask = 83886088);
UPDATE acl_entry SET mask = 83886088 WHERE mask = 100663328;

DELETE FROM acl_entry
WHERE mask = 50331664
  AND EXISTS (SELECT 1 FROM acl_entry AS other
              WHERE other.acl_object_identity = acl_entry.acl_object_identity
                AND other.sid = acl_entry.sid AND other.mask = 33554448);
UPDATE acl_entry SET mask = 33554448 WHERE mask = 50331664;

-- Deploying, running and benchmarking stopped being permissions on a repository.
DELETE FROM acl_entry WHERE mask IN (184549376, 201326592, 218103808);

-- The remaining masks became the four of Spring Security: 1 view, 2 edit, 4 create, 8 delete.
UPDATE acl_entry
SET mask = CASE
    WHEN mask = 16777217 THEN 1
    WHEN mask = 67108884 THEN 2
    WHEN mask = 33554448 THEN 4
    WHEN mask = 83886088 THEN 8
    ELSE mask
END
WHERE mask IN (16777217, 67108884, 33554448, 83886088);

-- Unlocking stopped being an authority of its own.
DELETE FROM OpenL_Group_Authorities WHERE authority IN ('UNLOCK_PROJECTS', 'UNLOCK_DEPLOYMENT');

-- One entry per security identity, carrying the role its permissions add up to.
CREATE TABLE Temp_BitwiseOrResults (
    acl_object_identity BIGINT NOT NULL,
    sid BIGINT NOT NULL,
    cumulative_mask INTEGER NOT NULL,
    granting BOOLEAN NOT NULL
);
INSERT INTO Temp_BitwiseOrResults (acl_object_identity, sid, cumulative_mask, granting)
SELECT acl_object_identity, sid, SUM(DISTINCT mask), granting
FROM acl_entry
GROUP BY acl_object_identity, sid, granting;
DELETE FROM acl_entry;
INSERT INTO acl_entry (acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
SELECT acl_object_identity,
       ROW_NUMBER() OVER (PARTITION BY acl_object_identity ORDER BY sid),
       sid,
       CASE
           WHEN cumulative_mask > 1 AND cumulative_mask < 15 THEN 15
           WHEN cumulative_mask > 15 AND cumulative_mask < 31 THEN 31
           ELSE cumulative_mask
       END,
       granting, FALSE, FALSE
FROM Temp_BitwiseOrResults;
DROP TABLE Temp_BitwiseOrResults;

CREATE INDEX idx_OpenL_External_Groups_groupName ON OpenL_External_Groups (groupName);

CREATE TABLE OpenL_PAT_Tokens (
    publicId VARCHAR(16) NOT NULL,
    secretHash VARCHAR(255) NOT NULL,
    createdAt TIMESTAMP NOT NULL,
    expiresAt TIMESTAMP,
    loginName VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (publicId),
    CONSTRAINT fk_OpenL_PAT_Tokens_user FOREIGN KEY (loginName) REFERENCES OpenL_Users (loginName) ON DELETE CASCADE,
    CONSTRAINT uq_OpenL_PAT_Tokens_user_name UNIQUE (loginName, name),
    CONSTRAINT ck_OpenL_PAT_Tokens_expires CHECK (expiresAt IS NULL OR expiresAt > createdAt)
);
CREATE INDEX ix_OpenL_PAT_Tokens_loginName ON OpenL_PAT_Tokens (loginName);

INSERT INTO "openl_security_flyway" ("installed_rank", "version", "description", "type", "script", "checksum", "installed_by", "execution_time", "success")
SELECT rank, version, description, 'SQL', description, 0, 'openl', 0, TRUE
FROM (
    SELECT 16 AS rank, '13.1' AS version, 'V13.1__Update_ACL_permissions.sql' AS description
    UNION ALL SELECT 17, '13.2', 'V13.2__Cutom_permissions_to_ACL_default.sql'
    UNION ALL SELECT 18, '13.3', 'V13.3__Drop_unlock_privileges_default.sql'
    UNION ALL SELECT 19, '13.4', 'V13.4__Merge_privileges_into_roles.sql'
    UNION ALL SELECT 20, '14', 'v14__Create_Index_ExternalGroups.sql'
    UNION ALL SELECT 21, '15', 'V15__Create_PAT_Tokens.sql'
) applied;

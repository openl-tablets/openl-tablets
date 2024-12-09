-- Permission mask values:
-- +------------+-------------+
-- |    Mask    | Description |
-- +------------+-------------+
-- | 83886088   | DELETE      |
-- | 100663328  | ERASE       |
-- | 33554448   | CREATE      |
-- | 50331664   | ADD         |
-- | 184549376  | DEPLOY      |
-- | 201326592  | RUN         |
-- | 218103808  | BENCHMARK   |
-- +------------+-------------+

-- Delete rows with `ERASE` permission if both `DELETE` and `ERASE` permissions exist
DELETE FROM acl_entry
WHERE mask = 100663328
  AND EXISTS (
    SELECT 1
    FROM acl_entry AS sub_entry
    WHERE sub_entry.acl_object_identity = acl_entry.acl_object_identity
      AND sub_entry.sid = acl_entry.sid
      AND sub_entry.mask = 83886088
);

-- Update remaining rows with `ERASE` permission to `DELETE` permission
UPDATE acl_entry
SET mask = 83886088
WHERE mask = 100663328
  AND NOT EXISTS (
    SELECT 1
    FROM acl_entry AS sub_entry
    WHERE sub_entry.acl_object_identity = acl_entry.acl_object_identity
      AND sub_entry.sid = acl_entry.sid
      AND sub_entry.mask = 83886088
);

-- Delete rows with `ADD` permission if both `CREATE` and `ADD` permissions exist
DELETE FROM acl_entry
WHERE mask = 50331664
  AND EXISTS (
    SELECT 1
    FROM acl_entry AS sub_entry
    WHERE sub_entry.acl_object_identity = acl_entry.acl_object_identity
      AND sub_entry.sid = acl_entry.sid
      AND sub_entry.mask = 33554448
);

-- Update remaining rows with `ADD` permission to `CREATE` permission
UPDATE acl_entry
SET mask = 33554448
WHERE mask = 50331664
  AND NOT EXISTS (
    SELECT 1
    FROM acl_entry AS sub_entry
    WHERE sub_entry.acl_object_identity = acl_entry.acl_object_identity
      AND sub_entry.sid = acl_entry.sid
      AND sub_entry.mask = 33554448
);

-- Delete rows with `DEPLOY`, `RUN`, `BENCHMARK` permission
DELETE FROM acl_entry
WHERE acl_entry.mask IN (184549376, 201326592, 218103808);

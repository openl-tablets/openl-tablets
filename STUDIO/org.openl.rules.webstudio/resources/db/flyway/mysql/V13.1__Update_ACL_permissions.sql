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
DELETE acl_entry
FROM acl_entry
JOIN acl_entry AS sub_entry
ON sub_entry.acl_object_identity = acl_entry.acl_object_identity
   AND sub_entry.sid = acl_entry.sid
   AND sub_entry.mask = 83886088
WHERE acl_entry.mask = 100663328;

-- Update remaining rows with `ERASE` permission to `DELETE` permission
UPDATE acl_entry AS main_entry
    LEFT JOIN acl_entry AS sub_entry
ON sub_entry.acl_object_identity = main_entry.acl_object_identity
    AND sub_entry.sid = main_entry.sid
    AND sub_entry.mask = 83886088
    SET main_entry.mask = 83886088
WHERE main_entry.mask = 100663328
  AND sub_entry.id IS NULL;

-- Delete rows with `ADD` permission if both `CREATE` and `ADD` permissions exist
DELETE acl_entry
FROM acl_entry
JOIN acl_entry AS sub_entry
ON sub_entry.acl_object_identity = acl_entry.acl_object_identity
   AND sub_entry.sid = acl_entry.sid
   AND sub_entry.mask = 33554448
WHERE acl_entry.mask = 50331664;

-- Update remaining rows with `ADD` permission to `CREATE` permission
UPDATE acl_entry AS main_entry
    LEFT JOIN acl_entry AS sub_entry
ON sub_entry.acl_object_identity = main_entry.acl_object_identity
    AND sub_entry.sid = main_entry.sid
    AND sub_entry.mask = 33554448
    SET main_entry.mask = 33554448
WHERE main_entry.mask = 50331664
  AND sub_entry.id IS NULL;

-- Delete rows with `DEPLOY`, `RUN`, `BENCHMARK` permission
DELETE FROM acl_entry
WHERE acl_entry.mask IN (184549376, 201326592, 218103808);

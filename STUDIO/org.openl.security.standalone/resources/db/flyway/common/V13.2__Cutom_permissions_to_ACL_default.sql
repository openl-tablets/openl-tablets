-- Permission mask values:
-- +------------+----------+-------------+
-- | OpenL Mask | ACL Mask | Description |
-- +------------+----------+-------------+
-- | 16777217   | 1        | VIEW        |
-- | 67108884   | 2        | EDIT        |
-- | 33554448   | 4        | CREATE      |
-- | 83886088   | 8        | DELETE      |
-- +------------+----------+-------------+
UPDATE acl_entry
SET mask = CASE
       WHEN mask = 16777217 THEN 1
       WHEN mask = 67108884 THEN 2
       WHEN mask = 33554448 THEN 4
       WHEN mask = 83886088 THEN 8
       ELSE mask
    END
WHERE mask IN (16777217, 67108884, 33554448, 83886088);


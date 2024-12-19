CREATE TABLE Temp_BitwiseOrResults
(
    acl_object_identity ${bigint}  NOT NULL,
    sid                 ${bigint}  NOT NULL,
    cumulative_mask     ${int}     NOT NULL,
    granting            ${boolean} NOT NULL,
    audit_success       ${boolean} NOT NULL,
    audit_failure       ${boolean} NOT NULL
);

-- Step 2: Populate the temporary table with aggregated results
INSERT INTO Temp_BitwiseOrResults (acl_object_identity, sid, cumulative_mask, granting, audit_success, audit_failure)
SELECT acl_object_identity,
       sid,
       SUM(DISTINCT mask) as cumulative_mask,
       granting,
       ${false}     as audit_success,
       ${false}     as audit_failure
FROM acl_entry
GROUP BY acl_object_identity, sid, granting;

-- Step 3: Delete old rows that have been merged
DELETE
FROM acl_entry
WHERE EXISTS (SELECT 1
              FROM Temp_BitwiseOrResults m
              WHERE acl_entry.acl_object_identity = m.acl_object_identity
                AND acl_entry.sid = m.sid);

-- Step 4: Insert merged rows back into acl_entry
INSERT INTO acl_entry (acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
SELECT acl_object_identity,
       ROW_NUMBER() OVER (PARTITION BY acl_object_identity ORDER BY sid) AS ace_order,
        sid,
       CASE
           WHEN cumulative_mask > 1 AND cumulative_mask < 15 THEN 15
           WHEN cumulative_mask > 15 AND cumulative_mask < 31 THEN 31
           ELSE cumulative_mask
           END AS mask,
       granting,
       audit_success,
       audit_failure
FROM Temp_BitwiseOrResults;

-- Step 5: Drop the temporary table
DROP TABLE Temp_BitwiseOrResults;

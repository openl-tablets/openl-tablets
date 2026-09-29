-- Define missed Authorities
INSERT INTO OpenL_Group_Authorities(groupID, authority)
SELECT DISTINCT g2g.groupID, ga.authority
FROM OpenL_Group2Group g2g
         JOIN OpenL_Group_Authorities ga ON ga.groupID = g2g.includedGroupID
         LEFT JOIN OpenL_Group_Authorities ga_check
                   ON ga_check.groupID = g2g.groupID AND ga_check.authority = ga.authority
WHERE ga_check.authority IS NULL;


-- Define missed SID
INSERT INTO acl_sid (principal, sid)
SELECT ${false}, g.groupName
FROM OpenL_Groups g
WHERE g.id IN (SELECT DISTINCT groupID FROM OpenL_Group2Group)
  AND g.groupName NOT IN (SELECT DISTINCT sid FROM acl_sid WHERE principal = ${false});


-- Define missed ACL entries
INSERT INTO acl_entry (acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
SELECT acl_object_identity,
       (SELECT max(ace_order) FROM acl_entry d WHERE en.acl_object_identity = d.acl_object_identity) +
       ROW_NUMBER() OVER (PARTITION BY en.acl_object_identity ORDER BY missedId) AS ace_order, missedId,
       mask,
       granting,
       audit_success,
       audit_failure
FROM acl_entry en
         JOIN(SELECT a1.id AS missedId,
                     a2.id AS includedGroup
              FROM OpenL_Group2Group g2g
                       JOIN OpenL_Groups g1 ON g2g.groupID = g1.id
                       JOIN OpenL_Groups g2 ON g2g.includedGroupID = g2.id
                       JOIN acl_sid a1 ON a1.sid = g1.groupName
                       JOIN acl_sid a2 ON a2.sid = g2.groupName) gr2gr ON en.sid = gr2gr.includedGroup
order by acl_object_identity, ace_order;


-- Delete nested groups
DROP TABLE OpenL_Group2Group;

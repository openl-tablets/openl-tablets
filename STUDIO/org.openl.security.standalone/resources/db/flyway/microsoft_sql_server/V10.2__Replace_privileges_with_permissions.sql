INSERT INTO acl_entry (acl_object_identity, sid, mask, granting, audit_success, audit_failure, ace_order)
    SELECT e.bid, e.aid, e.mask, 1, 0, 0, (SELECT count(*) FROM acl_entry d WHERE e.bid = d.acl_object_identity) + ROW_NUMBER() OVER (ORDER BY e.aid, e.bid)
    FROM (
        SELECT DISTINCT b.id bid, a.id aid, c.mask mask
        FROM acl_sid a,
             acl_object_identity b,
             acl_permission_mapping c,
             acl_class d,
             OpenL_Group_Authorities t1 INNER JOIN OpenL_Groups t2 ON t1.groupID = t2.id
        WHERE b.object_id_identity = c.object_id_identity
          AND b.object_id_class  = d.id
          AND d.class = c.class
          AND a.principal = 0
          AND c.authority = t1.authority
          AND a.sid = t2.groupName
        ) e;

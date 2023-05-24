DELETE FROM OpenL_Group_Authorities WHERE authority IN (SELECT apm.authority FROM acl_permission_mapping apm WHERE apm.authority <> 'VIEW_PROJECTS');

DELETE FROM OpenL_Group_Authorities WHERE authority IN ('CREATE_TABLES', 'EDIT_TABLES', 'REMOVE_TABLES');

DROP TABLE acl_repo_root;
DROP TABLE acl_permission_mapping;
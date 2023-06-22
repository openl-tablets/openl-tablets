DELETE FROM OpenL_Group_Authorities WHERE authority NOT IN ('ADMIN', 'UNLOCK_PROJECTS', 'UNLOCK_DEPLOYMENT');

DROP TABLE acl_repo_root;
DROP TABLE acl_permission_mapping;

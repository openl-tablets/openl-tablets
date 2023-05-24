CREATE TABLE acl_permission_mapping (
    mask ${bigint} not null,
    authority ${varchar}(50) not null,
    class ${varchar}(255) not null,
    object_id_identity ${varchar}(255) not null,
    PRIMARY KEY (class, object_id_identity, authority, mask)
);

CREATE TABLE acl_repo_root (
    object_id_identity ${varchar}(255) not null,
    PRIMARY KEY (object_id_identity)
);

INSERT INTO acl_repo_root (object_id_identity) VALUES ('1');
INSERT INTO acl_repo_root (object_id_identity) VALUES ('2');
INSERT INTO acl_repo_root (object_id_identity) VALUES ('3');

INSERT INTO acl_permission_mapping (class, object_id_identity, mask, authority) VALUES ('org.openl.security.acl.repository.Root', '1', 16777217, 'VIEW_PROJECTS');
INSERT INTO acl_permission_mapping (class, object_id_identity, mask, authority) VALUES ('org.openl.security.acl.repository.Root', '1', 33554448, 'CREATE_PROJECTS');
INSERT INTO acl_permission_mapping (class, object_id_identity, mask, authority) VALUES ('org.openl.security.acl.repository.Root', '1', 50331664, 'EDIT_PROJECTS');
INSERT INTO acl_permission_mapping (class, object_id_identity, mask, authority) VALUES ('org.openl.security.acl.repository.Root', '1', 67108884, 'EDIT_PROJECTS');
INSERT INTO acl_permission_mapping (class, object_id_identity, mask, authority) VALUES ('org.openl.security.acl.repository.Root', '1', 83886088, 'DELETE_PROJECTS');
INSERT INTO acl_permission_mapping (class, object_id_identity, mask, authority) VALUES ('org.openl.security.acl.repository.Root', '1', 100663328, 'ERASE_PROJECTS');
INSERT INTO acl_permission_mapping (class, object_id_identity, mask, authority) VALUES ('org.openl.security.acl.repository.Root', '1', 201326592, 'RUN');
INSERT INTO acl_permission_mapping (class, object_id_identity, mask, authority) VALUES ('org.openl.security.acl.repository.Root', '1', 201326592, 'TRACE');
INSERT INTO acl_permission_mapping (class, object_id_identity, mask, authority) VALUES ('org.openl.security.acl.repository.Root', '1', 218103808, 'BENCHMARK');

INSERT INTO acl_permission_mapping (class, object_id_identity, mask, authority) VALUES ('org.openl.security.acl.repository.Root', '2', 16777217, 'VIEW_PROJECTS');
INSERT INTO acl_permission_mapping (class, object_id_identity, mask, authority) VALUES ('org.openl.security.acl.repository.Root', '2', 67108884, 'EDIT_DEPLOYMENT');
INSERT INTO acl_permission_mapping (class, object_id_identity, mask, authority) VALUES ('org.openl.security.acl.repository.Root', '2', 50331664, 'EDIT_DEPLOYMENT');
INSERT INTO acl_permission_mapping (class, object_id_identity, mask, authority) VALUES ('org.openl.security.acl.repository.Root', '2', 33554448, 'CREATE_DEPLOYMENT');
INSERT INTO acl_permission_mapping (class, object_id_identity, mask, authority) VALUES ('org.openl.security.acl.repository.Root', '2', 83886088, 'DELETE_DEPLOYMENT');
INSERT INTO acl_permission_mapping (class, object_id_identity, mask, authority) VALUES ('org.openl.security.acl.repository.Root', '2', 100663328, 'ERASE_DEPLOYMENT');
INSERT INTO acl_permission_mapping (class, object_id_identity, mask, authority) VALUES ('org.openl.security.acl.repository.Root', '2', 184549376, 'DEPLOY_PROJECTS');

INSERT INTO acl_permission_mapping (class, object_id_identity, mask, authority) VALUES ('org.openl.security.acl.repository.Root', '3', 16777217, 'VIEW_PROJECTS');
INSERT INTO acl_permission_mapping (class, object_id_identity, mask, authority) VALUES ('org.openl.security.acl.repository.Root', '3', 16777217, 'DEPLOY_PROJECTS');
INSERT INTO acl_permission_mapping (class, object_id_identity, mask, authority) VALUES ('org.openl.security.acl.repository.Root', '3', 67108884, 'DEPLOY_PROJECTS');
INSERT INTO acl_permission_mapping (class, object_id_identity, mask, authority) VALUES ('org.openl.security.acl.repository.Root', '3', 83886088, 'DEPLOY_PROJECTS');

INSERT INTO acl_sid (principal, sid) VALUES (${false}, 'ADMIN');
INSERT INTO acl_sid (principal, sid)
    SELECT DISTINCT ${false}, t2.groupName
    FROM OpenL_Group_Authorities t1 INNER JOIN OpenL_Groups t2 ON t1.groupID = t2.id
    WHERE authority in (SELECT authority FROM acl_permission_mapping);

INSERT INTO acl_class (class, class_id_type) VALUES ('org.openl.security.acl.repository.Root', 'java.lang.String');
INSERT INTO acl_class (class, class_id_type) VALUES ('org.openl.security.acl.repository.ProjectArtifact', 'java.lang.String');
INSERT INTO acl_class (class, class_id_type) VALUES ('org.openl.security.acl.repository.DeploymentProjectArtifact', 'java.lang.String');
INSERT INTO acl_class (class, class_id_type) VALUES ('org.openl.security.acl.repository.RepositoryObjectIdentity', 'java.lang.String');

INSERT INTO acl_object_identity (object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting)
SELECT a.id, d.object_id_identity, null, b.id, ${true}
FROM acl_class a,
     acl_sid b,
     acl_repo_root d
WHERE a.class = 'org.openl.security.acl.repository.Root'
  AND b.principal = ${false}
  AND b.sid = 'ADMIN';
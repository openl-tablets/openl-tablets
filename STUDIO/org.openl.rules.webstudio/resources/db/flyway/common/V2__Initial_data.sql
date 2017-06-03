INSERT INTO OpenLUsers (loginName, password) VALUES('admin', '$2a$10$VSm7Xm79x6Nbzk7dxnzzzeITSB8IzwubuJxG/XYaK0Taa9Cb/4V06');

INSERT INTO OpenLGroups (id, groupName, description, userPrivileges) VALUES (0, 'Administrators', NULL, 'PRIVILEGE_ALL');
INSERT INTO OpenLGroups (id, groupName, description, userPrivileges) VALUES (1, 'Viewers', NULL, 'PRIVILEGE_VIEW_PROJECTS');
INSERT INTO OpenLGroups (id, groupName, description, userPrivileges) VALUES (2, 'Developers', NULL, 'PRIVILEGE_CREATE_PROJECTS,PRIVILEGE_EDIT_PROJECTS,PRIVILEGE_ERASE_PROJECTS,PRIVILEGE_DELETE_PROJECTS,PRIVILEGE_CREATE_TABLES,PRIVILEGE_EDIT_TABLES,PRIVILEGE_REMOVE_TABLES');
INSERT INTO OpenLGroups (id, groupName, description, userPrivileges) VALUES (3, 'Testers', NULL, 'PRIVILEGE_RUN,PRIVILEGE_TRACE,PRIVILEGE_BENCHMARK');
INSERT INTO OpenLGroups (id, groupName, description, userPrivileges) VALUES (4, 'Deployers', NULL, 'PRIVILEGE_DEPLOY_PROJECTS,PRIVILEGE_EDIT_DEPLOYMENT,PRIVILEGE_CREATE_DEPLOYMENT,PRIVILEGE_DELETE_DEPLOYMENT,PRIVILEGE_ERASE_DEPLOYMENT');
INSERT INTO OpenLGroups (id, groupName, description, userPrivileges) VALUES (5, 'Analysts', NULL, '');


INSERT INTO OpenLGroup2Group (groupID, includedGroupID) VALUES (2, 1);
INSERT INTO OpenLGroup2Group (groupID, includedGroupID) VALUES (3, 1);
INSERT INTO OpenLGroup2Group (groupID, includedGroupID) VALUES (4, 1);
INSERT INTO OpenLGroup2Group (groupID, includedGroupID) VALUES (5, 2);
INSERT INTO OpenLGroup2Group (groupID, includedGroupID) VALUES (5, 3);

INSERT INTO OpenLUser2Group (loginName, groupID) VALUES ('admin', 0);

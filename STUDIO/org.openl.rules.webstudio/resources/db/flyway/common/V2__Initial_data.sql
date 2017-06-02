INSERT INTO OpenLUsers (loginName, password) VALUES('admin', '$2a$10$VSm7Xm79x6Nbzk7dxnzzzeITSB8IzwubuJxG/XYaK0Taa9Cb/4V06');

INSERT INTO OpenLGroups (groupName, userPrivileges) VALUES ('Viewers', 'PRIVILEGE_VIEW_PROJECTS');
INSERT INTO OpenLGroups (groupName, userPrivileges) VALUES ('Developers', 'PRIVILEGE_CREATE_PROJECTS,PRIVILEGE_EDIT_PROJECTS,PRIVILEGE_ERASE_PROJECTS,PRIVILEGE_DELETE_PROJECTS,PRIVILEGE_CREATE_TABLES,PRIVILEGE_EDIT_TABLES,PRIVILEGE_REMOVE_TABLES');
INSERT INTO OpenLGroups (groupName, userPrivileges) VALUES ('Deployers', 'PRIVILEGE_DEPLOY_PROJECTS,PRIVILEGE_EDIT_DEPLOYMENT,PRIVILEGE_CREATE_DEPLOYMENT,PRIVILEGE_DELETE_DEPLOYMENT,PRIVILEGE_ERASE_DEPLOYMENT');
INSERT INTO OpenLGroups (groupName, userPrivileges) VALUES ('Testers', 'PRIVILEGE_RUN,PRIVILEGE_TRACE,PRIVILEGE_BENCHMARK');
INSERT INTO OpenLGroups (groupName, userPrivileges) VALUES ('Analysts', '');
INSERT INTO OpenLGroups (groupName, userPrivileges) VALUES ('Administrators', 'PRIVILEGE_ALL');

INSERT INTO OpenLGroup2Group (groupName, includedGroupName) VALUES ('Developers', 'Viewers');
INSERT INTO OpenLGroup2Group (groupName, includedGroupName) VALUES ('Deployers', 'Viewers');
INSERT INTO OpenLGroup2Group (groupName, includedGroupName) VALUES ('Testers', 'Viewers');
INSERT INTO OpenLGroup2Group (groupName, includedGroupName) VALUES ('Analysts', 'Developers');
INSERT INTO OpenLGroup2Group (groupName, includedGroupName) VALUES ('Analysts', 'Testers');

INSERT INTO OpenLUser2Group (loginName, groupName) VALUES ('admin', 'Administrators');

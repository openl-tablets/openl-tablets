INSERT INTO OpenL_Users (loginName, password) VALUES('admin', '$2a$10$VSm7Xm79x6Nbzk7dxnzzzeITSB8IzwubuJxG/XYaK0Taa9Cb/4V06');

INSERT INTO OpenL_Groups (id, groupName) VALUES (0, 'Administrators');
INSERT INTO OpenL_Groups (id, groupName) VALUES (1, 'Viewers');
INSERT INTO OpenL_Groups (id, groupName) VALUES (2, 'Developers');
INSERT INTO OpenL_Groups (id, groupName) VALUES (3, 'Testers');
INSERT INTO OpenL_Groups (id, groupName) VALUES (4, 'Deployers');
INSERT INTO OpenL_Groups (id, groupName) VALUES (5, 'Analysts');

INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (0,'PRIVILEGE_ALL');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (1,'PRIVILEGE_VIEW_PROJECTS');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (2,'PRIVILEGE_CREATE_PROJECTS');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (2,'PRIVILEGE_EDIT_PROJECTS');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (2,'PRIVILEGE_ERASE_PROJECTS');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (2,'PRIVILEGE_DELETE_PROJECTS');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (2,'PRIVILEGE_CREATE_TABLES');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (2,'PRIVILEGE_EDIT_TABLES');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (2,'PRIVILEGE_REMOVE_TABLES');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (3,'PRIVILEGE_RUN');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (3,'PRIVILEGE_TRACE');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (3,'PRIVILEGE_BENCHMARK');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (4,'PRIVILEGE_DEPLOY_PROJECTS');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (4,'PRIVILEGE_EDIT_DEPLOYMENT');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (4,'PRIVILEGE_CREATE_DEPLOYMENT');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (4,'PRIVILEGE_DELETE_DEPLOYMENT');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (4,'PRIVILEGE_ERASE_DEPLOYMENT');

INSERT INTO OpenL_Group2Group (groupID, includedGroupID) VALUES (2, 1);
INSERT INTO OpenL_Group2Group (groupID, includedGroupID) VALUES (3, 1);
INSERT INTO OpenL_Group2Group (groupID, includedGroupID) VALUES (4, 1);
INSERT INTO OpenL_Group2Group (groupID, includedGroupID) VALUES (5, 2);
INSERT INTO OpenL_Group2Group (groupID, includedGroupID) VALUES (5, 3);

INSERT INTO OpenL_User2Group (loginName, groupID) VALUES ('admin', 0);

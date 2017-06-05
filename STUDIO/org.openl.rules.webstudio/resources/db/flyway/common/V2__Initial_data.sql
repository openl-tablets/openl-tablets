INSERT INTO OpenL_Users (loginName, password) VALUES('admin', '$2a$10$VSm7Xm79x6Nbzk7dxnzzzeITSB8IzwubuJxG/XYaK0Taa9Cb/4V06');

INSERT INTO OpenL_Groups (id, groupName) VALUES (0, 'Administrators');
INSERT INTO OpenL_Groups (id, groupName) VALUES (1, 'Viewers');
INSERT INTO OpenL_Groups (id, groupName) VALUES (2, 'Developers');
INSERT INTO OpenL_Groups (id, groupName) VALUES (3, 'Testers');
INSERT INTO OpenL_Groups (id, groupName) VALUES (4, 'Deployers');
INSERT INTO OpenL_Groups (id, groupName) VALUES (5, 'Analysts');

INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (0,'ADMIN');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (1,'VIEW_PROJECTS');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (2,'CREATE_PROJECTS');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (2,'EDIT_PROJECTS');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (2,'ERASE_PROJECTS');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (2,'DELETE_PROJECTS');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (2,'CREATE_TABLES');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (2,'EDIT_TABLES');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (2,'REMOVE_TABLES');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (3,'RUN');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (3,'TRACE');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (3,'BENCHMARK');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (4,'DEPLOY_PROJECTS');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (4,'EDIT_DEPLOYMENT');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (4,'CREATE_DEPLOYMENT');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (4,'DELETE_DEPLOYMENT');
INSERT INTO OpenL_Group_Authorities (groupId, authority) VALUES (4,'ERASE_DEPLOYMENT');

INSERT INTO OpenL_Group2Group (groupID, includedGroupID) VALUES (2, 1);
INSERT INTO OpenL_Group2Group (groupID, includedGroupID) VALUES (3, 1);
INSERT INTO OpenL_Group2Group (groupID, includedGroupID) VALUES (4, 1);
INSERT INTO OpenL_Group2Group (groupID, includedGroupID) VALUES (5, 2);
INSERT INTO OpenL_Group2Group (groupID, includedGroupID) VALUES (5, 3);

INSERT INTO OpenL_User2Group (loginName, groupID) VALUES ('admin', 0);

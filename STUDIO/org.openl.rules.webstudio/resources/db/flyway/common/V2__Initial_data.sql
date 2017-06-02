INSERT INTO OpenLUsers (loginName, password) VALUES('admin', '$2a$10$VSm7Xm79x6Nbzk7dxnzzzeITSB8IzwubuJxG/XYaK0Taa9Cb/4V06');

INSERT INTO OpenLGroups (groupName, description, userPrivileges) VALUES ('Viewers', NULL, 'PRIVILEGE_VIEW_PROJECTS');
INSERT INTO OpenLGroups (groupName, description, userPrivileges) VALUES ('Developers', NULL, 'PRIVILEGE_CREATE_PROJECTS,PRIVILEGE_EDIT_PROJECTS,PRIVILEGE_ERASE_PROJECTS,PRIVILEGE_DELETE_PROJECTS,PRIVILEGE_CREATE_TABLES,PRIVILEGE_EDIT_TABLES,PRIVILEGE_REMOVE_TABLES');
INSERT INTO OpenLGroups (groupName, description, userPrivileges) VALUES ('Deployers', NULL, 'PRIVILEGE_DEPLOY_PROJECTS,PRIVILEGE_EDIT_DEPLOYMENT,PRIVILEGE_CREATE_DEPLOYMENT,PRIVILEGE_DELETE_DEPLOYMENT,PRIVILEGE_ERASE_DEPLOYMENT');
INSERT INTO OpenLGroups (groupName, description, userPrivileges) VALUES ('Testers', NULL, 'PRIVILEGE_RUN,PRIVILEGE_TRACE,PRIVILEGE_BENCHMARK');
INSERT INTO OpenLGroups (groupName, description, userPrivileges) VALUES ('Analysts', NULL, '');
INSERT INTO OpenLGroups (groupName, description, userPrivileges) VALUES ('Administrators', NULL, 'PRIVILEGE_ALL');

INSERT INTO OpenLGroup2Group (groupID, includedGroupID) (
	SELECT g1.id, g2.id
	FROM OpenLGroups g1, OpenLGroups g2
	WHERE g1.groupName = 'Developers' AND g2.groupName = 'Viewers'
		OR g1.groupName = 'Deployers' AND g2.groupName = 'Viewers'
		OR g1.groupName = 'Testers' AND g2.groupName = 'Viewers'
		OR g1.groupName = 'Analysts' AND g2.groupName = 'Developers'
		OR g1.groupName = 'Analysts' AND g2.groupName = 'Testers'
);

INSERT INTO OpenLUser2Group (loginName, groupID) (
	SELECT 'admin', g.id
	FROM OpenLGroups g
	WHERE g.groupName = 'Administrators'
);

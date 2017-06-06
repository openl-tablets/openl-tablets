INSERT INTO OpenL_Groups (groupName) VALUES ('Administrators');
INSERT INTO OpenL_Groups (groupName) VALUES ('Viewers');
INSERT INTO OpenL_Groups (groupName) VALUES ('Developers');
INSERT INTO OpenL_Groups (groupName) VALUES ('Testers');
INSERT INTO OpenL_Groups (groupName) VALUES ('Deployers');
INSERT INTO OpenL_Groups (groupName) VALUES ('Analysts');

INSERT INTO OpenL_Group_Authorities (groupID, authority) SELECT id, 'ADMIN' FROM OpenL_Groups WHERE groupName = 'Administrators';
INSERT INTO OpenL_Group_Authorities (groupID, authority) SELECT id, 'VIEW_PROJECTS' FROM OpenL_Groups WHERE groupName = 'Viewers';
INSERT INTO OpenL_Group_Authorities (groupID, authority) SELECT id, 'CREATE_PROJECTS' FROM OpenL_Groups WHERE groupName = 'Developers';
INSERT INTO OpenL_Group_Authorities (groupID, authority) SELECT id, 'EDIT_PROJECTS' FROM OpenL_Groups WHERE groupName = 'Developers';
INSERT INTO OpenL_Group_Authorities (groupID, authority) SELECT id, 'ERASE_PROJECTS' FROM OpenL_Groups WHERE groupName = 'Developers';
INSERT INTO OpenL_Group_Authorities (groupID, authority) SELECT id, 'DELETE_PROJECTS' FROM OpenL_Groups WHERE groupName = 'Developers';
INSERT INTO OpenL_Group_Authorities (groupID, authority) SELECT id, 'CREATE_TABLES' FROM OpenL_Groups WHERE groupName = 'Developers';
INSERT INTO OpenL_Group_Authorities (groupID, authority) SELECT id, 'EDIT_TABLES' FROM OpenL_Groups WHERE groupName = 'Developers';
INSERT INTO OpenL_Group_Authorities (groupID, authority) SELECT id, 'REMOVE_TABLES' FROM OpenL_Groups WHERE groupName = 'Developers';
INSERT INTO OpenL_Group_Authorities (groupID, authority) SELECT id, 'RUN' FROM OpenL_Groups WHERE groupName = 'Testers';
INSERT INTO OpenL_Group_Authorities (groupID, authority) SELECT id, 'TRACE' FROM OpenL_Groups WHERE groupName = 'Testers';
INSERT INTO OpenL_Group_Authorities (groupID, authority) SELECT id, 'BENCHMARK' FROM OpenL_Groups WHERE groupName = 'Testers';
INSERT INTO OpenL_Group_Authorities (groupID, authority) SELECT id, 'DEPLOY_PROJECTS' FROM OpenL_Groups WHERE groupName = 'Deployers';
INSERT INTO OpenL_Group_Authorities (groupID, authority) SELECT id, 'EDIT_DEPLOYMENT' FROM OpenL_Groups WHERE groupName = 'Deployers';
INSERT INTO OpenL_Group_Authorities (groupID, authority) SELECT id, 'CREATE_DEPLOYMENT' FROM OpenL_Groups WHERE groupName = 'Deployers';
INSERT INTO OpenL_Group_Authorities (groupID, authority) SELECT id, 'DELETE_DEPLOYMENT' FROM OpenL_Groups WHERE groupName = 'Deployers';
INSERT INTO OpenL_Group_Authorities (groupID, authority) SELECT id, 'ERASE_DEPLOYMENT' FROM OpenL_Groups WHERE groupName = 'Deployers';

INSERT INTO OpenL_Group2Group (groupID, includedGroupID) (
	SELECT g1.id, g2.id
	FROM OpenL_Groups g1, OpenL_Groups g2
	WHERE g1.groupName = 'Developers' AND g2.groupName = 'Viewers'
		OR g1.groupName = 'Deployers' AND g2.groupName = 'Viewers'
		OR g1.groupName = 'Testers' AND g2.groupName = 'Viewers'
		OR g1.groupName = 'Analysts' AND g2.groupName = 'Developers'
		OR g1.groupName = 'Analysts' AND g2.groupName = 'Testers'
);

INSERT INTO OpenL_Users (loginName, password) VALUES('admin', '$2a$10$VSm7Xm79x6Nbzk7dxnzzzeITSB8IzwubuJxG/XYaK0Taa9Cb/4V06');
INSERT INTO OpenL_User2Group (loginName, groupID)  SELECT 'admin', id FROM OpenL_Groups WHERE groupName = 'Administrators';

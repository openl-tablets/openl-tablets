INSERT INTO ${schemaPrefix}OpenLUser (LoginName, Password) VALUES('user', 'ee11cbb19052e40b07aac0ca060c23ee');
INSERT INTO ${schemaPrefix}OpenLUser (LoginName, Password) VALUES('u0', '3e334e859879af256d3827d651b7804a');
INSERT INTO ${schemaPrefix}OpenLUser (LoginName, Password) VALUES('u1', 'e4774cdda0793f86414e8b9140bb6db4');
INSERT INTO ${schemaPrefix}OpenLUser (LoginName, Password) VALUES('u2', '270c1b084f3f146eb5787075158d9c53');
INSERT INTO ${schemaPrefix}OpenLUser (LoginName, Password) VALUES('u3', '532a7b8e0328a8d05a8e6258b28b9a36');
INSERT INTO ${schemaPrefix}OpenLUser (LoginName, Password) VALUES('u4', '7b8d62fd2f0f5b2e3ba5437e5b983128');
INSERT INTO ${schemaPrefix}OpenLUser (LoginName, Password) VALUES('a1', '8a8bb7cd343aa2ad99b7d762030857a2');

INSERT INTO ${schemaPrefix}UserGroup (GroupName, Description, UserPrivileges) VALUES ('Viewers', NULL, 'PRIVILEGE_VIEW_PROJECTS');
INSERT INTO ${schemaPrefix}UserGroup (GroupName, Description, UserPrivileges) VALUES ('Developers', NULL, 'PRIVILEGE_CREATE_PROJECTS,PRIVILEGE_EDIT_PROJECTS,PRIVILEGE_ERASE_PROJECTS,PRIVILEGE_DELETE_PROJECTS,PRIVILEGE_CREATE_TABLES,PRIVILEGE_EDIT_TABLES,PRIVILEGE_REMOVE_TABLES');
INSERT INTO ${schemaPrefix}UserGroup (GroupName, Description, UserPrivileges) VALUES ('Deployers', NULL, 'PRIVILEGE_DEPLOY_PROJECTS,PRIVILEGE_EDIT_DEPLOYMENT,PRIVILEGE_CREATE_DEPLOYMENT,PRIVILEGE_DELETE_DEPLOYMENT,PRIVILEGE_ERASE_DEPLOYMENT');
INSERT INTO ${schemaPrefix}UserGroup (GroupName, Description, UserPrivileges) VALUES ('Testers', NULL, 'PRIVILEGE_RUN,PRIVILEGE_TRACE,PRIVILEGE_BENCHMARK');
INSERT INTO ${schemaPrefix}UserGroup (GroupName, Description, UserPrivileges) VALUES ('Analysts', NULL, '');
INSERT INTO ${schemaPrefix}UserGroup (GroupName, Description, UserPrivileges) VALUES ('Administrators', NULL, 'PRIVILEGE_ALL');

INSERT INTO ${schemaPrefix}Group2Group (GroupID, IncludedGroupID) (
	SELECT g1.GroupID, g2.GroupID
	FROM UserGroup g1, UserGroup g2
	WHERE g1.GroupName = 'Developers' AND g2.GroupName = 'Viewers'
		OR g1.GroupName = 'Deployers' AND g2.GroupName = 'Viewers'
		OR g1.GroupName = 'Testers' AND g2.GroupName = 'Viewers'
		OR g1.GroupName = 'Analysts' AND g2.GroupName = 'Developers'
		OR g1.GroupName = 'Analysts' AND g2.GroupName = 'Testers'
);

INSERT INTO ${schemaPrefix}User2Group (UserID, GroupID) (
	SELECT u.UserID, g.GroupID
	FROM OpenLUser u, UserGroup g
	WHERE u.LoginName = 'user' AND g.GroupName = 'Viewers'
		OR u.LoginName = 'u0' AND g.GroupName = 'Testers'
		OR u.LoginName = 'u1' AND g.GroupName = 'Developers'
		OR u.LoginName = 'u1' AND g.GroupName = 'Analysts'
		OR u.LoginName = 'u2' AND g.GroupName = 'Viewers'
		OR u.LoginName = 'u3' AND g.GroupName = 'Viewers'
		OR u.LoginName = 'u4' AND g.GroupName = 'Deployers'
		OR u.LoginName = 'a1' AND g.GroupName = 'Administrators'
);

INSERT INTO ${schemaPrefix}AccessControlEntry (permission, object, GroupID) (
	SELECT 'r+w+', '/repo1/project1', g.GroupID
	FROM UserGroup g
	WHERE g.GroupName = 'Viewers'
);


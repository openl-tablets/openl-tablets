/* ${schema} */
CREATE TABLE ${schema}.OpenlUser (
    UserID int NOT NULL ${increment_type},
    FirstName   varchar(50),
    Surname varchar(50),
    LoginName varchar(50),
    Password varchar(50),
    UserPrivileges varchar(200),
    PRIMARY KEY (UserId)
);

CREATE TABLE ${schema}.UserGroup (
    GroupID int NOT NULL ${increment_type},
    Description varchar(200),
    GroupName   varchar(40),
    UserPrivileges  varchar(200),
    PRIMARY KEY (GroupID)
);

CREATE TABLE ${schema}.User2Group (
    UserID  int,
    GroupID int,
    PRIMARY KEY (UserID, GroupID)
);

CREATE TABLE ${schema}.Group2Group (
    IncludedGroupID int,
    GroupID int,
    PRIMARY KEY (IncludedGroupID, GroupID)
);

CREATE TABLE ${schema}.AccessControlEntry (
    ACEID   int NOT NULL ${increment_type},
    object   varchar(200), 
    permission   varchar(200),
    GroupID int,
    UserID  int,
    PRIMARY KEY (ACEID)
);

INSERT INTO ${schema}.OpenlUser (UserID, LoginName, Password) VALUES(1, 'user', 'ee11cbb19052e40b07aac0ca060c23ee');
INSERT INTO ${schema}.OpenlUser (UserID, LoginName, Password) VALUES(2, 'u0', 'ee11cbb19052e40b07aac0ca060c23ee');
INSERT INTO ${schema}.OpenlUser (UserID, LoginName, Password) VALUES(3, 'u1', 'ee11cbb19052e40b07aac0ca060c23ee');
INSERT INTO ${schema}.OpenlUser (UserID, LoginName, Password) VALUES(4, 'u2', 'ee11cbb19052e40b07aac0ca060c23ee');
INSERT INTO ${schema}.OpenlUser (UserID, LoginName, Password) VALUES(5, 'u3', 'ee11cbb19052e40b07aac0ca060c23ee');
INSERT INTO ${schema}.OpenlUser (UserID, LoginName, Password) VALUES(6, 'u4', 'ee11cbb19052e40b07aac0ca060c23ee');
INSERT INTO ${schema}.OpenlUser (UserID, LoginName, Password) VALUES(7, 'a1', 'ee11cbb19052e40b07aac0ca060c23ee');

INSERT INTO ${schema}.UserGroup (GroupId, GroupName, Description, UserPrivileges) VALUES (1, 'Viewers', '[null]', 'PRIVILEGE_VIEW_PROJECTS');
INSERT INTO ${schema}.UserGroup (GroupId, GroupName, Description, UserPrivileges) VALUES (2, 'Developers', '[null]', 'PRIVILEGE_CREATE_PROJECTS,PRIVILEGE_EDIT_PROJECTS,PRIVILEGE_ERASE_PROJECTS,PRIVILEGE_DELETE_PROJECTS,PRIVILEGE_CREATE_TABLES,PRIVILEGE_EDIT_TABLES,PRIVILEGE_REMOVE_TABLES');
INSERT INTO ${schema}.UserGroup (GroupId, GroupName, Description, UserPrivileges) VALUES (3, 'Deployers', '[null]', 'PRIVILEGE_DEPLOY_PROJECTS,PRIVILEGE_EDIT_DEPLOYMENT,PRIVILEGE_CREATE_DEPLOYMENT,PRIVILEGE_DELETE_DEPLOYMENT,PRIVILEGE_ERASE_DEPLOYMENT');
INSERT INTO ${schema}.UserGroup (GroupId, GroupName, Description, UserPrivileges) VALUES (4, 'Testers', '[null]', 'PRIVILEGE_RUN,PRIVILEGE_TRACE,PRIVILEGE_BENCHMARK');
INSERT INTO ${schema}.UserGroup (GroupId, GroupName, Description, UserPrivileges) VALUES (5, 'Analysts', '[null]', '');
INSERT INTO ${schema}.UserGroup (GroupId, GroupName, Description, UserPrivileges) VALUES (6, 'Administrators', '[null]', 'PRIVILEGE_ALL');

INSERT INTO ${schema}.Group2Group (GroupId, IncludedGroupId) VALUES (2, 1);
INSERT INTO ${schema}.Group2Group (GroupId, IncludedGroupId) VALUES (3, 1);
INSERT INTO ${schema}.Group2Group (GroupId, IncludedGroupId) VALUES (4, 1);
INSERT INTO ${schema}.Group2Group (GroupId, IncludedGroupId) VALUES (5, 2);
INSERT INTO ${schema}.Group2Group (GroupId, IncludedGroupId) VALUES (5, 4);

INSERT INTO ${schema}.User2Group (UserId, GroupId) VALUES (1, 1);
INSERT INTO ${schema}.User2Group (UserId, GroupId) VALUES (2, 4);
INSERT INTO ${schema}.User2Group (UserId, GroupId) VALUES (3, 2);
INSERT INTO ${schema}.User2Group (UserId, GroupId) VALUES (3, 5);
INSERT INTO ${schema}.User2Group (UserId, GroupId) VALUES (4, 1);
INSERT INTO ${schema}.User2Group (UserId, GroupId) VALUES (5, 1);
INSERT INTO ${schema}.User2Group (UserId, GroupId) VALUES (6, 3);
INSERT INTO ${schema}.User2Group (UserId, GroupId) VALUES (7, 6);

INSERT INTO ${schema}.AccessControlEntry (ACEId, Permission, Object, GroupId) VALUES (1, 'r+w+', '/repo1/project1', 1);


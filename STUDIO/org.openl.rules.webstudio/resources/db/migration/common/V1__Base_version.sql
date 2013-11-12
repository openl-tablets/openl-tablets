${create_hibernate_sequence}

CREATE TABLE ${schemaPrefix}OpenLUser (
    UserID ${identity_column},
    FirstName   varchar(50) default null,
    Surname varchar(50) default null,
    LoginName varchar(50) not null unique,
    Password varchar(50)not null,
    UserPrivileges ${longtext},
    PRIMARY KEY (UserID)
);

CREATE TABLE ${schemaPrefix}UserGroup (
    GroupID ${identity_column},
    Description varchar(200) default null,
    GroupName   varchar(40) not null unique,
    UserPrivileges  ${longtext},
    PRIMARY KEY (GroupID)
);

CREATE TABLE ${schemaPrefix}User2Group (
    UserID  ${bigint} not null,
    GroupID ${bigint} not null,
    PRIMARY KEY (UserID, GroupID),
    CONSTRAINT fk_User2Group1 FOREIGN KEY (UserID) REFERENCES OpenLUser(UserID),
    CONSTRAINT fk_User2Group2 FOREIGN KEY (GroupID) REFERENCES UserGroup(GroupID)
);

CREATE TABLE ${schemaPrefix}Group2Group (
    IncludedGroupID ${bigint} not null,
    GroupID ${bigint} not null,
    PRIMARY KEY (IncludedGroupID, GroupID),
    CONSTRAINT fk_Group2Group1 FOREIGN KEY (IncludedGroupID) REFERENCES UserGroup(GroupID),
    CONSTRAINT fk_Group2Group2 FOREIGN KEY (GroupID) REFERENCES UserGroup(GroupID)
);

CREATE TABLE ${schemaPrefix}AccessControlEntry (
    ACEID   ${identity_column},
    object   ${longtext} not null, 
    permission   ${longtext} not null,
    GroupID ${bigint} default null,
    UserID  ${bigint} default null,
    PRIMARY KEY (ACEID),
    CONSTRAINT fk_AccessControlEntry1 FOREIGN KEY (UserID) REFERENCES OpenLUser(UserID),
    CONSTRAINT fk_AccessControlEntry2 FOREIGN KEY (GroupID) REFERENCES UserGroup(GroupID)
);


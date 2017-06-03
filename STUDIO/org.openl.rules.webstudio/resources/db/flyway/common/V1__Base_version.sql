CREATE TABLE OpenL_Users (
    loginName varchar(50) not null,
    password varchar(128),
    lastLogin TIMESTAMP,
    origin varchar(50),
    firstName varchar(50),
    surname varchar(50),
    PRIMARY KEY (loginName)
);

CREATE TABLE OpenL_Groups (
    id ${identity},
    groupName varchar(40) not null unique,
    description varchar(200),
    userPrivileges  varchar(1000),
    PRIMARY KEY (id)
);

CREATE TABLE OpenL_User2Group (
    loginName varchar(50) not null,
    groupID ${bigint} not null,
    PRIMARY KEY (loginName, groupID),
    CONSTRAINT fk_OpenLUser2Group1 FOREIGN KEY (loginName) REFERENCES OpenLUsers(loginName),
    CONSTRAINT fk_OpenLUser2Group2 FOREIGN KEY (groupID) REFERENCES OpenLGroups(id)
);

CREATE TABLE OpenL_Group2Group (
    IncludedGroupID ${bigint} not null,
    groupID ${bigint} not null,
    PRIMARY KEY (IncludedGroupID, groupID),
    CONSTRAINT fk_OpenLGroup2Group1 FOREIGN KEY (IncludedGroupID) REFERENCES OpenLGroups(id),
    CONSTRAINT fk_OpenLGroup2Group2 FOREIGN KEY (groupID) REFERENCES OpenLGroups(id)
);

CREATE TABLE OpenLUsers (
    loginName varchar(50) not null,
    password varchar(128) not null,
    lastLogin TIMESTAMP,
    origin varchar(50),
    firstName varchar(50),
    surname varchar(50),
    PRIMARY KEY (loginName)
);

CREATE TABLE OpenLGroups (
    groupName varchar(50) not null,
    description varchar(200),
    userPrivileges varchar(1000),
    PRIMARY KEY (groupName)
);

CREATE TABLE OpenLUser2Group (
    loginName varchar(50) not null,
    groupName varchar(50) not null,
    PRIMARY KEY (loginName, groupName),
    CONSTRAINT fk_OpenLUser2Group1 FOREIGN KEY (loginName) REFERENCES OpenLUsers(loginName),
    CONSTRAINT fk_OpenLUser2Group2 FOREIGN KEY (groupName) REFERENCES OpenLGroups(groupName)
);

CREATE TABLE OpenLGroup2Group (
    includedGroupName varchar(50) not null,
    groupName varchar(50) not null,
    PRIMARY KEY (includedGroupName, groupName),
    CONSTRAINT fk_OpenLGroup2Group1 FOREIGN KEY (includedGroupName) REFERENCES OpenLGroups(groupName),
    CONSTRAINT fk_OpenLGroup2Group2 FOREIGN KEY (groupName) REFERENCES OpenLGroups(groupName)
);

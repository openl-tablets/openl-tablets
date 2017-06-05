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
    PRIMARY KEY (id)
);

CREATE TABLE OpenL_Group_Authorities (
    groupID ${bigint} not null,
    authority varchar(40) not null,
    PRIMARY KEY (groupID, authority),
    CONSTRAINT fk_OpenL_Group_Authorities1 FOREIGN KEY (groupID) REFERENCES OpenL_Groups(id) ON DELETE CASCADE
);

CREATE TABLE OpenL_User2Group (
    loginName varchar(50) not null,
    groupID ${bigint} not null,
    PRIMARY KEY (loginName, groupID),
    CONSTRAINT fk_OpenL_User2Group1 FOREIGN KEY (loginName) REFERENCES OpenL_Users(loginName) ON DELETE CASCADE,
    CONSTRAINT fk_OpenL_User2Group2 FOREIGN KEY (groupID) REFERENCES OpenL_Groups(id) ON DELETE CASCADE
);

CREATE TABLE OpenL_Group2Group (
    IncludedGroupID ${bigint} not null,
    groupID ${bigint} not null,
    PRIMARY KEY (IncludedGroupID, groupID),
    CONSTRAINT fk_OpenL_Group2Group1 FOREIGN KEY (IncludedGroupID) REFERENCES OpenL_Groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_OpenL_Group2Group2 FOREIGN KEY (groupID) REFERENCES OpenL_Groups(id) ON DELETE CASCADE
);

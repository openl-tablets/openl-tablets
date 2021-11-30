CREATE TABLE OpenL_External_Groups (
    loginName ${varchar}(50) not null,
    groupName ${varchar}(50) not null,
    PRIMARY KEY (loginName, groupName),
    CONSTRAINT fk_OpenL_External_Groups1 FOREIGN KEY (loginName) REFERENCES OpenL_Users(loginName) ON DELETE CASCADE
);

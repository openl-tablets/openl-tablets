CREATE TABLE OpenL_Tag_Types (
    id ${identity},
    name ${varchar}(255) not null,
    extensible ${boolean} not null,
    nullable ${boolean} not null,
    PRIMARY KEY (id),
    CONSTRAINT uni_OpenL_Tag_Types1 UNIQUE (name)
);

CREATE TABLE OpenL_Tags (
    id ${identity},
    tag_type_id ${bigint} not null,
    name ${varchar}(255) not null,
    PRIMARY KEY (id),
    CONSTRAINT fk_OpenL_Tags1 FOREIGN KEY (tag_type_id) REFERENCES OpenL_Tag_Types(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT uni_OpenL_Tags1 UNIQUE (tag_type_id, name)
);

CREATE TABLE OpenL_Projects (
    id ${identity},
    repository_id ${varchar}(255) not null,
    project_path ${varchar}(1000) not null,
    PRIMARY KEY (id)
);

CREATE TABLE OpenL_Project_Tags (
    project_id ${bigint} not null,
    tag_id ${bigint} not null,
    PRIMARY KEY (project_id, tag_id),
    CONSTRAINT fk_OpenL_Project_Tags1 FOREIGN KEY (project_id) REFERENCES OpenL_Projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_OpenL_Project_Tags2 FOREIGN KEY (tag_id) REFERENCES OpenL_Tags(id) ON DELETE CASCADE
);

CREATE TABLE OpenL_Project_Grouping (
    loginName ${varchar} not null,
    group1 ${varchar}(255),
    group2 ${varchar}(255),
    group3 ${varchar}(255),
    PRIMARY KEY (loginName),
    CONSTRAINT fk_OpenL_Project_Grouping1 FOREIGN KEY (loginName) REFERENCES OpenL_Users(loginName) ON DELETE CASCADE
);

CREATE TABLE OpenL_Tag_Templates (
    template ${varchar} not null,
    priority int,
    PRIMARY KEY (template),
    CONSTRAINT uni_OpenL_Tag_Templates1 UNIQUE (priority)
)
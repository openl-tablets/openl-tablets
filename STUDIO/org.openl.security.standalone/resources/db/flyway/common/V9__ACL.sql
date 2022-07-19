CREATE TABLE acl_sid
(
    id        ${identity} not null,
    principal ${boolean}  not null,
    sid       ${varchar}(255) not null,
    PRIMARY KEY (id),
    constraint unique_OpenL_acl_sid1 UNIQUE (sid, principal)
);

CREATE TABLE acl_class
(
    id            ${identity} not null primary key,
    class         ${varchar}(255) not null,
    class_id_type ${varchar}(255),
    PRIMARY KEY (id),
    CONSTRAINT unique_OpenL_acl_class1 UNIQUE (class)
);

CREATE TABLE acl_object_identity
(
    id                 ${identity},
    object_id_class    ${bigint}  not null,
    object_id_identity ${varchar}(255) not null,
    parent_object      ${bigint},
    owner_sid          ${bigint},
    entries_inheriting ${boolean} not null,
    PRIMARY KEY (id),
    CONSTRAINT unique_OpenL_acl_object_identity1 UNIQUE (object_id_class, object_id_identity),
    CONSTRAINT fk_OpenL_acl_object_identity1 FOREIGN KEY (parent_object) REFERENCES acl_object_identity (id),
    CONSTRAINT fk_OpenL_acl_object_identity2 FOREIGN KEY (object_id_class) REFERENCES acl_class (id),
    CONSTRAINT fk_OpenL_acl_object_identity3 FOREIGN KEY (owner_sid) REFERENCES acl_sid (id)
);

CREATE TABLE acl_entry
(
    id                  ${identity},
    acl_object_identity ${bigint}  not null,
    ace_order           int        not null,
    sid                 ${bigint}  not null,
    mask                int        not null,
    granting            ${boolean} not null,
    audit_success       ${boolean} not null,
    audit_failure       ${boolean} not null,
    PRIMARY KEY (id),
    CONSTRAINT unique_OpenL_acl_entry1 UNIQUE (acl_object_identity, ace_order),
    CONSTRAINT fk_OpenL_acl_entry1 FOREIGN KEY (acl_object_identity) REFERENCES acl_object_identity (id),
    CONSTRAINT fk_OpenL_acl_entry2 FOREIGN KEY (sid) REFERENCES acl_sid (id)
);
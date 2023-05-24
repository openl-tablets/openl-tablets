CREATE TABLE acl_sid (
	id NUMBER(38) NOT NULL PRIMARY KEY,
	principal NUMBER(1) NOT NULL CHECK (principal in (0, 1)),
	sid NVARCHAR2(255) NOT NULL,
	CONSTRAINT unique_acl_sid UNIQUE (sid, principal)
);

CREATE TABLE acl_class (
	id NUMBER(38) NOT NULL PRIMARY KEY,
	class NVARCHAR2(2000) NOT NULL,
	class_id_type NVARCHAR2(2000),
	CONSTRAINT uk_acl_class UNIQUE (class)
);

CREATE TABLE acl_object_identity (
	id NUMBER(38) NOT NULL PRIMARY KEY,
	object_id_class NUMBER(38) NOT NULL,
	object_id_identity NVARCHAR2(2000) NOT NULL,
	parent_object NUMBER(38),
	owner_sid NUMBER(38),
	entries_inheriting NUMBER(1) NOT NULL CHECK (entries_inheriting in (0, 1)),
	CONSTRAINT uk_acl_object_identity UNIQUE (object_id_class, object_id_identity),
	CONSTRAINT fk_acl_object_identity_parent FOREIGN KEY (parent_object) REFERENCES acl_object_identity (id),
	CONSTRAINT fk_acl_object_identity_class FOREIGN KEY (object_id_class) REFERENCES acl_class (id),
	CONSTRAINT fk_acl_object_identity_owner FOREIGN KEY (owner_sid) REFERENCES acl_sid (id)
);

CREATE TABLE acl_entry (
	id NUMBER(38) NOT NULL PRIMARY KEY,
	acl_object_identity NUMBER(38) NOT NULL,
	ace_order INTEGER NOT NULL,
	sid NUMBER(38) NOT NULL,
	mask INTEGER NOT NULL,
	granting NUMBER(1) NOT NULL CHECK (granting in (0, 1)),
	audit_success NUMBER(1) NOT NULL CHECK (audit_success in (0, 1)),
	audit_failure NUMBER(1) NOT NULL CHECK (audit_failure in (0, 1)),
	CONSTRAINT unique_acl_entry UNIQUE (acl_object_identity, ace_order),
	CONSTRAINT fk_acl_entry_object FOREIGN KEY (acl_object_identity) REFERENCES acl_object_identity (id),
	CONSTRAINT fk_acl_entry_acl FOREIGN KEY (sid) REFERENCES acl_sid (id)
);

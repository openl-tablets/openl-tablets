CREATE SEQUENCE acl_sid_sequence START WITH 1 INCREMENT BY 1 NOMAXVALUE;
CREATE OR REPLACE TRIGGER acl_sid_id_trigger
	BEFORE INSERT ON acl_sid
	FOR EACH ROW
BEGIN
	SELECT acl_sid_sequence.nextval INTO :new.id FROM dual;
END;

CREATE SEQUENCE acl_class_sequence START WITH 1 INCREMENT BY 1 NOMAXVALUE;
CREATE OR REPLACE TRIGGER acl_class_id_trigger
	BEFORE INSERT ON acl_class
	FOR EACH ROW
BEGIN
	SELECT acl_class_sequence.nextval INTO :new.id FROM dual;
END;

CREATE SEQUENCE acl_object_identity_sequence START WITH 1 INCREMENT BY 1 NOMAXVALUE;
CREATE OR REPLACE TRIGGER acl_object_identity_id_trigger
	BEFORE INSERT ON acl_object_identity
	FOR EACH ROW
BEGIN
	SELECT acl_object_identity_sequence.nextval INTO :new.id FROM dual;
END;

CREATE SEQUENCE acl_entry_sequence START WITH 1 INCREMENT BY 1 NOMAXVALUE;
CREATE OR REPLACE TRIGGER acl_entry_id_trigger
	BEFORE INSERT ON acl_entry
	FOR EACH ROW
BEGIN
	SELECT acl_entry_sequence.nextval INTO :new.id FROM dual;
END;
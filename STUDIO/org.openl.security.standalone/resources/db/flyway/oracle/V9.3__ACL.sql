CREATE SEQUENCE acl_object_identity_sequence START WITH 1 INCREMENT BY 1 NOMAXVALUE;
CREATE OR REPLACE TRIGGER acl_object_identity_id_trigger
	BEFORE INSERT ON acl_object_identity
	FOR EACH ROW
BEGIN
	SELECT acl_object_identity_sequence.nextval INTO :new.id FROM dual;
END;

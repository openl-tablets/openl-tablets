CREATE SEQUENCE acl_class_sequence START WITH 1 INCREMENT BY 1 NOMAXVALUE;
CREATE OR REPLACE TRIGGER acl_class_id_trigger
	BEFORE INSERT ON acl_class
	FOR EACH ROW
BEGIN
	SELECT acl_class_sequence.nextval INTO :new.id FROM dual;
END;

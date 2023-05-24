CREATE SEQUENCE acl_entry_sequence START WITH 1 INCREMENT BY 1 NOMAXVALUE;
CREATE OR REPLACE TRIGGER acl_entry_id_trigger
	BEFORE INSERT ON acl_entry
	FOR EACH ROW
BEGIN
	SELECT acl_entry_sequence.nextval INTO :new.id FROM dual;
END;

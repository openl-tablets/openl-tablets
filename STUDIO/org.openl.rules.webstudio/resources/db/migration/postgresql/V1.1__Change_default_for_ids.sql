CREATE SEQUENCE hibernate_sequence;

ALTER TABLE openluser ALTER COLUMN userid SET DEFAULT nextval('hibernate_sequence');
ALTER TABLE usergroup ALTER COLUMN groupid SET DEFAULT nextval('hibernate_sequence');
ALTER TABLE accesscontrolentry ALTER COLUMN aceid SET DEFAULT nextval('hibernate_sequence');

DROP SEQUENCE openluser_userid_seq;
DROP SEQUENCE usergroup_groupid_seq;
DROP SEQUENCE accesscontrolentry_aceid_seq;
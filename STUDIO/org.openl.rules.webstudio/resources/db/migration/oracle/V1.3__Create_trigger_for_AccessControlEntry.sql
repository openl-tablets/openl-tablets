create or replace trigger AccessControlEntry_ACEID_trg
before insert on AccessControlEntry
for each row
begin
  if :new.ACEID is null then
    select hibernate_sequence.nextval into :new.ACEID from dual;
  end if;
end;
create or replace trigger UserGroup_GroupID_trg
before insert on UserGroup
for each row
begin
  if :new.GroupID is null then
    select hibernate_sequence.nextval into :new.GroupID from dual;
  end if;
end;

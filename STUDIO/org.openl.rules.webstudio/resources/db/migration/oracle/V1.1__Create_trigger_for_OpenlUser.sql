create or replace trigger OpenlUser_UserID_trg
before insert on OpenlUser
for each row
begin
  if :new.UserID is null then
    select hibernate_sequence.nextval into :new.UserID from dual;
  end if;
end;

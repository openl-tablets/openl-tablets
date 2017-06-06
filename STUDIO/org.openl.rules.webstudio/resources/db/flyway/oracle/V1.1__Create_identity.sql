create SEQUENCE OpenL_Groups_ID_SEQ
create or replace trigger OpenL_Groups_ID_TRG
before insert on OpenL_Groups
for each row
begin
  if :new.id is null then
    select OpenL_Groups_ID_SEQ.nextval into :new.id from dual;
  end if;
end;

create SEQUENCE OpenL_Audit_Log_ID_SEQ;
create or replace trigger OpenL_Audit_Log_ID_TRG
before insert on OpenL_Audit_Log
for each row
begin
  if :new.id is null then
    select OpenL_Audit_Log_ID_SEQ.nextval into :new.id from dual;
  end if;
end;
/

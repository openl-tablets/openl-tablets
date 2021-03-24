create SEQUENCE OpenL_Tags_ID_SEQ;
create or replace trigger OpenL_Tags_ID_TRG
before insert on OpenL_Tags
for each row
begin
  if :new.id is null then
    select OpenL_Tags_ID_SEQ.nextval into :new.id from dual;
  end if;
end;

create SEQUENCE OpenL_Projects_ID_SEQ;
create or replace trigger OpenL_Projects_ID_TRG
before insert on OpenL_Projects
for each row
begin
  if :new.id is null then
    select OpenL_Projects_ID_SEQ.nextval into :new.id from dual;
  end if;
end;

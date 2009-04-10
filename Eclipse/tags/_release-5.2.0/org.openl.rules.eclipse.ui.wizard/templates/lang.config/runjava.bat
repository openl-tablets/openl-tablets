rem @echo off
call %~dp0/setenv
rem cd %~dp0
echo. 
@echo on
%JAVA_CONSOLE% %VM_PARAMS% -cp %_USER_CP%%_LIB% %1 %2 %3 %4 %5 %6 %7 %8
echo. 
pause

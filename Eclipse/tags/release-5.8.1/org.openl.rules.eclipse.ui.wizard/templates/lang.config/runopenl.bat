rem @echo off
call %~dp0/setenv
rem cd %~dp0
echo. 
rem @echo on
%JAVA_CONSOLE% %VM_PARAMS% -cp classes;%_LIB% org.openl.main.OpenlMain  %1 %2 %3 %4 %5 %6 %7 %8

echo. 
pause

@echo off
echo %M2_HOME%
set path=%M2_HOME%\bin;%path% 
del /Q target\dependency
call mvn dependency:copy-dependencies
call mvn -Dmaven.test.skip=true install 
pause

rem @echo off
rem set JAVA_HOME=E:\Test\Java\jdk1.5.0_12\bin\
rem set JDK_ROOT=E:\Test\Java\jdk1.5.0_12\bin\
rem set PATH=%JAVA_HOME%\bin;%PATH%
rem set path
rem call -nowait SplashKiller.exe
pause
setlocal
rem set NAB=-Dlog4j.log4j.rootCategory=INFO
set NAB=-Dlog4j.debug=false
set mainClass=com.exigen.scan.demo.ScannerApplication
for %%N in (target\dependency\*.*) do call append.bat %%N
set CMD=java -cp %NAB% %AGENT% %mainClass%
echo %CMD%
rem @echo %XXX%
echo Launching %mainClass%
rem path
%CMD% 
endlocal 
pause


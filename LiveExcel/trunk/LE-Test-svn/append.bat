rem @echo off
if "%NAB%" == ""  goto FIRST_TIME
set NAB=%NAB%;%1
goto END
:FIRST_TIME
set NAB=%1
:END


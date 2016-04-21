@rem set JRE_HOME=C:\Program Files\Java\jre1.8.0_92

@setlocal
@set delay=20
@echo ### Checking Java environment ...
@echo.
@call :start & if not errorlevel 1 goto :end
@set delay=120

@rem Try to detect installed Java
@where /Q java.exe
@if errorlevel 1 (
  @echo       Probably, you have not installed Java...
  @goto :noJava
)

@for /f %%i in ('@where java.exe') do @set VAR=%%i
@echo       Executable java.exe is located at:
@echo.
@echo           %VAR%
@echo.

@rem Try to resolve JRE_HOME from the found executable java.
@call :startJRE "%VAR%" & if not errorlevel 1 goto :end

@rem Try to resolve JRE_HOME from the symlink on the found executable java.
@for /f "tokens=2 delims=[]" %%i in ('@dir %VAR% ^| findstr /l \bin\java.exe') do @set VAR2=%%i
@call :startJRE "%VAR2%" & if not errorlevel 1 goto :end

:noJava

@echo       Check JRE_HOME and JAVA_HOME environment variables.
@echo       JRE_HOME should point to the directory where Java was installed.
@echo.

@if "%JRE_HOME%" == "" @if "%JAVA_HOME%" == "" (
  @echo       Neither the JAVA_HOME nor the JRE_HOME environment variable is defined
  @echo       At least one of these environment variable is needed to run this program
  @echo.
  @echo       !!!    JRE_HOME variable is not set 
  @echo.
  @echo       You can define this variable in this file "start.cmd"
  @echo       The first line should be like this:
  @echo.
  @echo           set JRE_HOME=C:\Program Files\Java\jre1.8.0_92
  @echo.
  @echo       where  "C:\Program Files\Java\jre1.8.0_92"
  @echo       is the directory where Java was installed.
  @goto :end
)

@echo       Current values of Java environment variables:
@if "%JAVA_HOME%" neq "" @echo           set JAVA_HOME=%JAVA_HOME%
@if "%JRE_HOME%" neq ""  @echo           set JRE_HOME=%JRE_HOME%

goto :end

rem SUBROUTINES

:startJRE
@setlocal
@set _ARG=%~1
@if "%_ARG:~-13%" neq "\bin\java.exe" exit /b 2 & endlocal
@set JRE_HOME=%_ARG:~0,-13%
@echo ### Extracting JRE_HOME from %_ARG% ...
@echo ### Trying to use JRE_HOME=%JRE_HOME% ...
@echo.

:start
@setlocal & call bin\setclasspath.bat > NUL & endlocal & if errorlevel 1 exit /b 1 & endlocal
@setlocal
@echo ### Starting OpenL Tablets DEMO ...
@echo.
@if "%JAVA_OPTS%" neq ""     @echo Using JAVA_OPTS:       "%JAVA_OPTS%"
@if "%CATALINA_OPTS%" neq "" @echo Using CATALINA_OPTS:   "%CATALINA_OPTS%"
@set CATALINA_OPTS=%CATALINA_OPTS% -DDEMO=DEMO
@pushd bin
@call startup.bat
@popd
@exit /b 0 & endlocal

:end
@echo.
@echo.
@echo       To get more information about OpenL Tablets DEMO,
@echo       please refer to "Demo Package Guide" on our site:
@echo.
@echo       http://openl-tablets.org/documentation/user-guides
@echo.
@echo %delay% seconds delay before closing this window.
@choice /C C /T %delay% /D C /N /M "Press [C] key to Close this windows immediatly." & endlocal

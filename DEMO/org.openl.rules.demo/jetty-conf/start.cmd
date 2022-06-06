@rem set JRE_HOME=C:\Program Files\Java\jre1.8.0_92
@rem set _JAVA_MEMORY=-Xms512m -Xmx2g

@setlocal
@set errorcode=0
@echo ### Checking Java environment ...
@echo.

@rem Try to all known locations of java
@call :tryJava "%JRE_HOME%" & if not errorlevel 1 goto :end
@call :tryJava "%JAVA_HOME%" & if not errorlevel 1 goto :end
@call :startJava "java" & if not errorlevel 1 goto :end
@call :findJava "%ProgramW6432%\Java" & if not errorlevel 1 goto :end
@call :findJava "%ProgramFiles%\Java" & if not errorlevel 1 goto :end
@call :findJava "%ProgramFiles(x86)%\Java" & if not errorlevel 1 goto :end

@set errorcode=1
@echo       Check JRE_HOME and JAVA_HOME environment variables.
@echo       JRE_HOME or JAVA_HOME should point to the directory where Java was installed.
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

@goto :end

rem SUBROUTINES
@rem errorlevel=0 java.exe has been found and executed successfully
@rem errorlevel=2 java.exe has not been found
@rem errorlevel=3 java.exe is not suitable
@rem errorlevel=4 Not suitable Java version


:findJava
@rem Find java installations in the folder. E.g. C:\Program Files\Java\[jre1.8.0_92]
@if not exist "%~1" exit /b 2
@for /f "tokens=*" %%i in ('dir "%~1" /O-D /AD /B') do @call :tryJava "%~1\%%~i" & echo. & if not errorlevel 1 exit /b 0
@exit /b 2

:tryJava
@rem Check if folder exists and it contains executable java.exe file
@if not exist "%~1" exit /b 2
@if not exist "%~1\bin\java.exe" exit /b 2
@call :startJava "%~1\bin\java.exe" & if not errorlevel 1 exit /b 0
@exit /b 3

:startJava
@"%~1" -version >nul 2>&1
@IF ERRORLEVEL 1 exit /b 3
@set _JAVA="%~1"
@echo ### Found executable Java at: %_JAVA%


@rem Determine Java version
@FOR /f "tokens=3" %%G IN ('call %_JAVA% -version 2^>^&1 ^| find "version"') DO @set _JAVA_VERSION=%%~G
@if "%_JAVA_VERSION%" == "" set _JAVA_VERSION=UNKNOWN

@rem Determine memory size
@for /f %%G in ('wmic ComputerSystem get TotalPhysicalMemory ^| findstr [0123456789]') do @set _MEMORY=%%G
@if "%_MEMORY%" == "" set _MEMORY=0
@set _MEMORY=%_MEMORY:~0,-9%

@if not defined _JAVA_MEMORY (
@rem default memory settings
@set _JAVA_MEMORY=-Xms256m -Xmx512m

@rem 2GiB
@if %_MEMORY% GEQ 2 set _JAVA_MEMORY=-Xms512m -Xmx1024m

@rem 3GiB
@if %_MEMORY% GEQ 3 set _JAVA_MEMORY=-Xms768m -Xmx1536m

@rem 4GiB
@if %_MEMORY% GEQ 4 set _JAVA_MEMORY=-Xms1g -Xmx2g

@rem 6GiB
@if %_MEMORY% GEQ 6 set _JAVA_MEMORY=-Xms2g -Xmx4g

@rem 8GiB
@if %_MEMORY% GEQ 8 set _JAVA_MEMORY=-Xms4g -Xmx6g

@rem 12GiB
@if %_MEMORY% GEQ 12 set _JAVA_MEMORY=-Xms4g -Xmx10g

@rem 16GiB
@if %_MEMORY% GEQ 16 set _JAVA_MEMORY=-Xms4g -Xmx12g

@rem 24GiB
@if %_MEMORY% GEQ 24 set _JAVA_MEMORY=-Xms4g -Xmx20g

@rem 32GiB
@if %_MEMORY% GEQ 32 set _JAVA_MEMORY=-Xms4g -Xmx28g

@rem 48GiB
@if %_MEMORY% GEQ 48 set _JAVA_MEMORY=-Xms4g -Xmx42g

@rem reset to safe settings for 32bit
@if %_MEMORY% GEQ 4 %_JAVA% -version 2>&1 | find "64-Bit" >nul || (
set _JAVA_MEMORY=-Xms512m -Xmx1024m
echo.
echo.
echo      ************************************************
echo      *                                              *
echo      *  Old 32-bit Java version has been detected!  *
echo      *                                              *
echo      *   A limited amount of memory will be used.   *
echo      *                                              *
echo      ************************************************
echo.
echo.
)
)

@rem Show Java version
@%_JAVA% %_JAVA_MEMORY% -version
@echo.
@echo -------------------------------

@setlocal
@pushd %~dp0

@rem Apply security policy for demo
@if exist demo-java.policy set JETTY_OPT=-Djava.security.manager -Djava.security.policy=demo-java.policy -Djava.extensions=%SystemRoot%\Sun\Java\lib\ext

@set JAVA_OPTS=%JETTY_OPT%  %_JAVA_MEMORY% %JAVA_OPTS%
@if not defined OPENL_HOME set OPENL_HOME=./openl-demo

@echo ### Starting OpenL Tablets DEMO ...
@echo Memory size:           "%_MEMORY%GBytes"
@echo Java version:          "%_JAVA_VERSION%"
@echo Java found in:         "%_JAVA%"
@echo Using JAVA_OPTS:       "%JAVA_OPTS%"
@echo Using OPENL_HOME:      "%OPENL_HOME%"

@set TEMP=.\tmp
@if not exist %TEMP% mkdir -p %TEMP%

%_JAVA% -DDEMO=DEMO -Dh2.bindAddress=localhost -Dopenl.home="%OPENL_HOME%" %JAVA_OPTS% -Djetty.home="%CD%" -Djetty.base="%CD%" -Djava.io.tmpdir="%TEMP%" -jar start.jar jetty.state=jetty.state jetty-started.xml

@popd
@exit /b 0 & endlocal

:end
@echo.
@echo.
@echo       To get more information about OpenL Tablets DEMO,
@echo       please refer to "Demo Package Guide" on our site:
@echo.
@echo       https://openl-tablets.org/documentation/user-guides
@echo.
@exit /b %errorcode% & endlocal

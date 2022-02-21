@echo STOP demo
@setlocal
@pushd bin

@IF "%JAVA_HOME%"=="" (
@    echo JAVA_HOME is not set, cannot run Jetty
@    exit /B 3
)

@rem try to kill java process with -DDEMO=DEMO property
@FOR /F %%A IN ('%JAVA_HOME%\bin\jps -v ^| find "-DDEMO=DEMO"') DO (
@echo Killing PID:%%A process...
@taskkill /F /T /PID %%A
)

@popd
@endlocal

@echo STOP demo
@setlocal
@pushd bin
@call shutdown.bat

@echo waiting 30 seconds...
@ping 127.0.0.1 -n 30 > NUL

@rem try to kill java process with -DDEMO=DEMO property
@FOR /F %%A IN ('jps -v ^| find "-DDEMO=DEMO"') DO (
@echo Killing PID:%%A process...
@taskkill /F /T /PID %%A
)

@popd
@endlocal

@echo START demo
@setlocal
@set CATALINA_OPTS=%CATALINA_OPTS% -DDEMO=DEMO
@pushd bin
@call startup.bat
@popd
@endlocal

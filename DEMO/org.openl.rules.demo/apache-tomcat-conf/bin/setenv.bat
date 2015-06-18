if "%CUSTOM_PORT%"=="" SET CUSTOM_PORT=8080
set JAVA_OPTS=%JAVA_OPTS% -Xms256m -Xmx1024m -XX:+UseParallelOldGC -XX:PermSize=128m -XX:MaxPermSize=384m
set CATALINA_OPTS=%CATALINA_OPTS% -Dderby.stream.error.file=..\\logs\\derby.log -Dws.port=%CUSTOM_PORT%

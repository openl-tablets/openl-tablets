set JAVA_OPTS=%JAVA_OPTS% -Xms512m -Xmx2g -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:PermSize=128m -XX:MaxPermSize=512m
set DEMO_OPTS=-Druleservice.datasource.type=jcr -Druleservice.rmiPort=8099 -Druleservice.openl.home=openl-demo
set DEMO_OPTS=%DEMO_OPTS% -Druleservice.datasource.deploy.classpath.jars=true
set DEMO_OPTS=%DEMO_OPTS% -Druleservice.datasource.repositoryPropertiesFile=openl-demo\system-settings\rules-production.properties
set DEMO_OPTS=%DEMO_OPTS% -Dwebstudio.home=openl-demo -Dwebstudio.configured=true -Dws.port=8080
set CATALINA_OPTS=%DEMO_OPTS% %CATALINA_OPTS% -Dderby.stream.error.file=..\\logs\\derby.log

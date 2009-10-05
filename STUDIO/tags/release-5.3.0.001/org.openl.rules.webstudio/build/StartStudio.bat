cd ..


set CP=../org.openl.lib.apache.tomcat/apache-tomcat-5.5.17/bin/bootstrap.jar
set CP=%CP%;../org.openl.core/bin
set CP=%CP%;../org.openl.commons/bin
set CP=%CP%;../org.openl.j/bin
set CP=%CP%;../org.openl.rules/bin
set CP=%CP%;../org.openl.lib.apache.commons/commons-lang-1.0.1/commons-lang-1.0.1.jar
set CP=%CP%;../org.openl.lib.apache.commons/commons-logging-1.0.3/commons-logging.jar
set CP=%CP%;../org.openl.lib.apache.commons/commons-logging-1.0.3/commons-logging-api.jar
set CP=%CP%;../org.openl.rules.webstudio/bin
set CP=%CP%;../org.openl.rules.webtools/bin

java -Xms256M -Xmx1024M -cp %CP% org.openl.rules.webtools.StartTomcat


pause



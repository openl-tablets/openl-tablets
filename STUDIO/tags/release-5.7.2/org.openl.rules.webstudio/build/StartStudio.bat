cd ..


set CP=../org.openl.rules.tomcat.lib/apache-tomcat-6.0.20/bin/bootstrap.jar
set CP=%CP%;../org.openl.core/bin
set CP=%CP%;../org.openl.commons/bin
set CP=%CP%;../org.openl.j/bin
set CP=%CP%;../org.openl.rules/bin
set CP=%CP%;../org.openl.lib.apache.commons/commons-lang-1.0.1/commons-lang-1.0.1.jar
set CP=%CP%;../org.openl.lib.apache.commons/commons-logging-1.0.3/commons-logging.jar
set CP=%CP%;../org.openl.lib.apache.commons/commons-logging-1.0.3/commons-logging-api.jar
set CP=%CP%;../org.openl.rules.webstudio/bin
set CP=%CP%;../org.openl.commons.web/bin

java -Xms256M -Xmx1024M -cp %CP% org.openl.rules.webstudio.util.StartTomcat


pause



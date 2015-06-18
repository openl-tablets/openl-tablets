export JAVA_OPTS="$JAVA_OPTS -Xms256m -Xmx1024m -XX:+UseParallelOldGC -XX:PermSize=128m -XX:MaxPermSize=384m"
export CATALINA_OPTS="$CATALINA_OPTS -Dderby.stream.error.file=..\\logs\\derby.log -Dws.port=8080"

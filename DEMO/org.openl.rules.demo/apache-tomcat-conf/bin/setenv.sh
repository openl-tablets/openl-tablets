export JAVA_OPTS="$JAVA_OPTS -Xms512m -Xmx2g -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:PermSize=128m -XX:MaxPermSize=512m"
export CATALINA_OPTS="$CATALINA_OPTS -Dderby.stream.error.file=..\\logs\\derby.log -Dws.port=8080"

#!/bin/sh

export JAVA_OPTS="$JAVA_OPTS -Xms512m -Xmx2g -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:PermSize=128m -XX:MaxPermSize=512m"
export CATALINA_OPTS="-Dwebstudio.home=openl-demo -Dwebstudio.configured=true -Dws.port=8080 $CATALINA_OPTS"
./bin/startup.sh

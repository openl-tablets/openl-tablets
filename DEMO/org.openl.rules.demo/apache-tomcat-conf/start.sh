#!/bin/bash

#export JAVA_HOME=/opt/java/jdk-9.0.4

# Java extensions can be outside of JAVA_HOME. Needed for security manager
JAVA_EXTENSIONS_DIR=/usr/java/packages/lib/ext

used_java="unknown"

if [[ -n "$JRE_HOME" ]] && [[ -x "$JRE_HOME/bin/java" ]];  then
    _java="$JRE_HOME/bin/java"
    used_java=$_java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    _java="$JAVA_HOME/bin/java"
    used_java=$_java
elif type -p java; then
    _java=java
    used_java="PATH"
else
    echo "--- Probably, you have not installed Java"
    exit 1
fi

if [[ "$_java" ]]; then
    version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    if [[ "$version" = "9" ]] || [[ "${version:0:2}" == "9." ]]; then
        JAVA_OPTS="$JAVA_OPTS --add-modules java.se.ee --patch-module java.xml.ws.annotation=lib/annotations-api.jar --add-exports=java.xml.ws.annotation/javax.annotation.security=ALL-UNNAMED"
    elif [[ "${version:0:3}" == "1.8" ]]; then
        JAVA_OPTS="$JAVA_OPTS -XX:+UseParNewGC -XX:+UseConcMarkSweepGC"
    elif [[ "${version:0:3}" == "1.7" ]]; then
        JAVA_OPTS="$JAVA_OPTS -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:PermSize=128m -XX:MaxPermSize=512mC"
    fi
fi

memory=$(free -g | awk '/Mem:/{print $2}')
if [[ "$CONTAINER" != "true" ]]; then
  if [[ ${memory} -ge 12 ]]; then
    _JAVA_MEMORY="-Xms8g -Xmx10g"
  elif [[ ${memory} -ge 8 ]]; then
    _JAVA_MEMORY="-Xms4g -Xmx7g"
  elif [[ _memory -ge 6 ]]; then
    _JAVA_MEMORY="-Xms3g -Xmx5g"
  elif [[ ${memory} -ge 4 ]]; then
    _JAVA_MEMORY="-Xms2g -Xmx3g"
  else
    _JAVA_MEMORY="-Xms512m -Xmx2g"
  fi
fi



[[ -f demo-java.policy ]] && CATALINA_OPTS="$CATALINA_OPTS -Djava.security.manager -Djava.security.policy=demo-java.policy -Djava.extensions=$JAVA_EXTENSIONS_DIR"

export JAVA_OPTS="$JAVA_OPTS $_JAVA_MEMORY"
export CATALINA_OPTS="-DDEMO=DEMO -Dopenl.home=openl-demo -Dws.port=8080 $CATALINA_OPTS"

echo "### Starting OpenL Tablets DEMO ..."
echo "Memory size (gigabytes):    $memory"
echo "Java version:               $version"
echo "Java found in:              $used_java"
echo "Using JAVA_OPTS:            $JAVA_OPTS"
echo "Using CATALINA_OPTS:        $CATALINA_OPTS"

chmod +x bin/*.sh
./bin/startup.sh

# syntax=docker/dockerfile:1

ARG JDK=21-jre-noble

FROM eclipse-temurin:${JDK} as jdk

FROM alpine as otel

ENV OTEL_VER 2.15.0

RUN <<EOT
set -euxv

# Install tools
apk add --no-cache wget gnupg

# Download OpenTelemetry java agent
wget  --progress=dot:giga -O opentelemetry-javaagent.jar "https://repo1.maven.org/maven2/io/opentelemetry/javaagent/opentelemetry-javaagent/$OTEL_VER/opentelemetry-javaagent-$OTEL_VER.jar"
wget  --progress=dot:giga -O opentelemetry-javaagent.jar.asc "https://repo1.maven.org/maven2/io/opentelemetry/javaagent/opentelemetry-javaagent/$OTEL_VER/opentelemetry-javaagent-$OTEL_VER.jar.asc"

# GPG verification
gpg --batch --keyserver keyserver.ubuntu.com --recv-keys 17A27CE7A60FF5F0
gpg --batch --verify opentelemetry-javaagent.jar.asc opentelemetry-javaagent.jar

# Remove tools
apk del wget gnupg
EOT

FROM ubuntu:focal

LABEL org.opencontainers.image.url="https://openl-tablets.org/"
LABEL org.opencontainers.image.vendor="OpenL Tablets"

ENV LC_ALL C.UTF-8

ARG APP=STUDIO/org.openl.rules.webstudio/target/webapp

# Copy Java
ENV JAVA_HOME /opt/java/openjdk
COPY --from=jdk $JAVA_HOME $JAVA_HOME

ENV OPENL_DIR /opt/openl
ENV OPENL_HOME $OPENL_DIR/local
ENV OPENL_HOME_SHARED $OPENL_DIR/shared
ENV OPENL_APP $OPENL_DIR/app
ENV OPENL_LIB $OPENL_DIR/lib

# OpenTelemetry
ARG OTEL_APP=Util/openl-rules-opentelemetry/target/openl-rules-opentelemetry.jar
ENV OTEL_DIR /opt/opentelemetry
ENV OTEL_SERVICE_NAME OpenL
RUN mkdir -p $OTEL_DIR
COPY --from=otel opentelemetry-javaagent.jar $OTEL_DIR

COPY --from=jetty:12-jre21 /usr/local/jetty $OPENL_APP

# Create start file for Jetty with configuration options
RUN <<'EOT' cat > $OPENL_DIR/start.sh && chmod +x $OPENL_DIR/start.sh
#!/usr/bin/env bash

if [ -r "$OPENL_DIR/setenv.sh" ]; then
    . "$OPENL_DIR/setenv.sh"
fi

echo "--------------------------------------------------------------------------------------------------"
echo "|    To define OpenTelemetry endpoint:    OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger-host:4318    |"
echo "|    To disable OpenL rules tracing:      OTEL_INSTRUMENTATION_OPENL_RULES_ENABLED=false         |"
echo "|    To disable OpenTelemetry:            OTEL_JAVAAGENT_ENABLED=false                           |"
echo "--------------------------------------------------------------------------------------------------"

if [ -z "$OTEL_JAVAAGENT_ENABLED" ] && [ -z "$(env | grep -E '^OTEL_EXPORTER_')" ]; then
    echo "INFO: OpenTelemetry has been disabled because of no OTEL_EXPORTER_*** are defined"
    export OTEL_JAVAAGENT_ENABLED=false
fi

if [ -z "$OTEL_JAVAAGENT_EXTENSIONS" ] && [ "${OTEL_INSTRUMENTATION_OPENL_RULES_ENABLED,,}" != "false"  ]; then
    echo "INFO: OpenL rules agent extension for the OpenTelemetry has been defined"
    export OTEL_JAVAAGENT_EXTENSIONS="$OTEL_DIR/openl-rules-opentelemetry.jar"
fi

JAVA_OPTS="$(eval echo \"$JAVA_OPTS\")"

exec java $JAVA_OPTS -Djetty.home="$OPENL_APP" -Djetty.base="$OPENL_APP" -Djava.io.tmpdir="${TMPDIR:-/tmp}" \
-javaagent:"$OTEL_DIR/opentelemetry-javaagent.jar" \
-jar "$OPENL_APP/start.jar" --module=http,ee10-jsp,ext,ee10-deploy --lib="$OPENL_LIB/*.jar" "$@"
EOT

# Create setenv.sh file for configuration customization purpose
RUN <<'EOT' cat > $OPENL_DIR/setenv.sh && chmod +x $OPENL_DIR/setenv.sh
export JAVA_OPTS="$JAVA_OPTS \
-Dorg.eclipse.jetty.server.Request.maxFormContentSize=-1 \
-Dorg.eclipse.jetty.server.Request.maxFormKeys=-1 \
-Djetty.httpConfig.requestHeaderSize=32768 \
-Djetty.httpConfig.responseHeaderSize=32768 \
"
EOT

RUN <<EOT
set -euxv

# Install fonts required for Apache POI (export into Excel with autowidth of columns)
apt-get update
apt-get install -y --no-install-recommends fontconfig
rm -rf /var/lib/apt/lists/*

# Permission for rootless mode (for running as non-root)
mkdir $OPENL_DIR/logs
chmod o=u $OPENL_DIR/logs

mkdir $OPENL_LIB
chmod o=u -R $OPENL_LIB
EOT

# Define executables
ENV PATH .:$JAVA_HOME/bin:$PATH

ENV JAVA_OPTS="-Xms32m -XX:MaxRAMPercentage=90.0"

# Create a system 'openl' user with home directory. Home directory is required for Java Prefs persistence to prevent
# WARN spamming in the log.
# UID=1000 as a de-facto standard in k8s examples.
RUN useradd -r -m -U -s /usr/sbin/nologin openl -u 1000

# Writable folder for 'openl' user where application files are stored.
# It should be mounted on an external volume to persist application data between redeploying if it is required.
# Do not confuse this with home directory of 'openl' user.
RUN mkdir -p "$OPENL_HOME" && chown openl:openl "$OPENL_HOME"
RUN mkdir -p "$OPENL_HOME_SHARED" && chown openl:openl "$OPENL_HOME_SHARED"
# Running a container under a non-root user
USER openl

EXPOSE 8080

COPY $OTEL_APP $OTEL_DIR
COPY $APP $OPENL_APP/webapps/ROOT

WORKDIR $OPENL_DIR

CMD ["/opt/openl/start.sh"]

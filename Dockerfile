# syntax=docker/dockerfile:1

FROM alpine AS otel

ENV OTEL_VER 2.28.1

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

FROM alpine AS log4j

ENV LOG4J_VER 2.26.0

RUN <<EOT
set -euxv

# Install tools
apk add --no-cache wget gnupg

# Download the log4j2 JSON Template Layout (provides the Elastic Common Schema layout). It is added to the
# webapp by the Docker image so the .war itself carries no dependency on it.
# LOG4J_VER must match log4j.version in the root pom.xml.
ARTIFACT=https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-layout-template-json/$LOG4J_VER/log4j-layout-template-json-$LOG4J_VER.jar
wget  --progress=dot:giga -O log4j-layout-template-json.jar "$ARTIFACT"
wget  --progress=dot:giga -O log4j-layout-template-json.jar.asc "$ARTIFACT.asc"

# GPG verification (ASF Logging Services release manager key)
gpg --batch --keyserver keyserver.ubuntu.com --recv-keys 56E73BA9A0B592D0
gpg --batch --verify log4j-layout-template-json.jar.asc log4j-layout-template-json.jar

# Remove tools
apk del wget gnupg
EOT

FROM eclipse-temurin:25-jre-alpine-3.23 AS openl

LABEL org.opencontainers.image.url="https://openl-tablets.org/"
LABEL org.opencontainers.image.vendor="OpenL Tablets"

ENV LC_ALL C.UTF-8

RUN apk upgrade --no-cache

ARG APP=STUDIO/org.openl.rules.webstudio/target/webapp

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

COPY --from=jetty:12.1-jdk25 /usr/local/jetty $OPENL_APP

# Create start file for Jetty with configuration options
RUN <<'EOT' cat > $OPENL_DIR/start.sh && chmod +x $OPENL_DIR/start.sh
#!/usr/bin/env sh

if [ -r "$OPENL_DIR/setenv.sh" ]; then
    . "$OPENL_DIR/setenv.sh"
fi

echo "--------------------------------------------------------------------------------------------------"
echo "|    To define OpenTelemetry endpoint:    OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger-host:4318    |"
echo "|    To disable OpenL rules tracing:      OTEL_INSTRUMENTATION_OPENL_RULES_ENABLED=false         |"
echo "|    To disable OpenTelemetry:            OTEL_JAVAAGENT_ENABLED=false                           |"
echo "|    To set the root log level:           LOGGING_LEVEL_ROOT=DEBUG  (default INFO)               |"
echo "--------------------------------------------------------------------------------------------------"

if [ -z "$OTEL_JAVAAGENT_ENABLED" ] && [ -z "$(env | grep -E '^OTEL_EXPORTER_')" ]; then
    echo "INFO: OpenTelemetry has been disabled because of no OTEL_EXPORTER_*** are defined"
    export OTEL_JAVAAGENT_ENABLED=false
fi

if [ -z "$OTEL_JAVAAGENT_EXTENSIONS" ] && [ "$(echo "$OTEL_INSTRUMENTATION_OPENL_RULES_ENABLED" | tr '[:upper:]' '[:lower:]')" != "false" ]; then
    echo "INFO: OpenL rules agent extension for the OpenTelemetry has been defined"
    export OTEL_JAVAAGENT_EXTENSIONS="$OTEL_DIR/openl-rules-opentelemetry.jar"
fi

JAVA_OPTS="$(eval echo \"$JAVA_OPTS\")"

exec java $JAVA_OPTS -Djetty.home="$OPENL_APP" -Djetty.base="$OPENL_APP" -Djava.io.tmpdir="${TMPDIR:-/tmp}" \
-javaagent:"$OTEL_DIR/opentelemetry-javaagent.jar" \
-jar "$OPENL_APP/start.jar" --module=http,ee10-jsp,ext,ee10-deploy,ee10-websocket-jakarta,ee10-cdi  --lib="$OPENL_LIB/*.jar" "$@"
EOT

# Create setenv.sh file for configuration customization purpose
RUN <<'EOT' cat > $OPENL_DIR/setenv.sh && chmod +x $OPENL_DIR/setenv.sh
export JAVA_OPTS="$JAVA_OPTS \
-Dlog4j2.configurationFile=$OPENL_DIR/log4j2.properties \
-Dorg.eclipse.jetty.server.Request.maxFormContentSize=-1 \
-Dorg.eclipse.jetty.server.Request.maxFormKeys=-1 \
-Djetty.httpConfig.requestHeaderSize=32768 \
-Djetty.httpConfig.responseHeaderSize=32768 \
"
EOT

# Create log4j2 configuration for cloud-friendly logging in Elastic Common Schema (ECS) JSON format on stdout.
# It overrides the plain-text configuration bundled in the .war and is applied only inside the container.
RUN <<'EOT' cat > $OPENL_DIR/log4j2.properties
status = warn

appender.console.type = Console
appender.console.name = STDOUT
appender.console.layout.type = JsonTemplateLayout
appender.console.layout.eventTemplateUri = classpath:EcsLayout.json
# Identify each log line's origin so it can be told apart from other services/instances.
# service.name reuses the OpenTelemetry service name; host.name is the container/pod hostname.
# EventTemplateAdditionalField rejects blank values, so every value keeps a non-blank default.
appender.console.layout.svc.type = EventTemplateAdditionalField
appender.console.layout.svc.key = service.name
appender.console.layout.svc.value = ${env:OTEL_SERVICE_NAME:-OpenL}
appender.console.layout.host.type = EventTemplateAdditionalField
appender.console.layout.host.key = host.name
appender.console.layout.host.value = ${env:HOSTNAME:-unknown}
appender.console.layout.ver.type = EventTemplateAdditionalField
appender.console.layout.ver.key = service.version
appender.console.layout.ver.value = ${env:SERVICE_VERSION:-0}
appender.console.layout.env.type = EventTemplateAdditionalField
appender.console.layout.env.key = service.environment
appender.console.layout.env.value = ${env:ENVIRONMENT:-production}

rootLogger.level = ${env:LOGGING_LEVEL_ROOT:-INFO}
rootLogger.appenderRef.console.ref = STDOUT
EOT

RUN <<EOT
set -euxv

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
RUN addgroup -g 1000 -S openl && adduser -S -D -s /usr/sbin/nologin -u 1000 openl -G openl

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
# Add the ECS JSON layout to the webapp. It is intentionally not bundled in the .war (which stays
# logging-agnostic); the image supplies it so container logging can use the ECS layout.
COPY --from=log4j /log4j-layout-template-json.jar $OPENL_APP/webapps/ROOT/WEB-INF/lib/

WORKDIR $OPENL_DIR

CMD ["/opt/openl/start.sh"]

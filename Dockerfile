# syntax=docker/dockerfile:1

# LOG4J_VER must match log4j.version in the root pom.xml.
ARG LOG4J_VER=2.26.0

FROM alpine AS otel

ENV OTEL_VER 2.29.0

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

ARG LOG4J_VER

RUN <<EOT
set -euxv

# Install tools
apk add --no-cache wget gnupg

# Download log4j2 for the single logging system of the Docker image (see the wiring in the 'openl' stage):
# - log4j-api, log4j-core, log4j-slf4j2-impl power the Jetty 'logging-log4j2' module
# - log4j-jul bridges java.util.logging into log4j2
# - log4j-layout-template-json provides the Elastic Common Schema (ECS) JSON layout

# Import the GPG key for verification (ASF Logging Services release manager key)
gpg --batch --keyserver keyserver.ubuntu.com --recv-keys 56E73BA9A0B592D0

for ARTIFACT in log4j-api log4j-core log4j-slf4j2-impl log4j-jul log4j-layout-template-json; do
    JAR=$ARTIFACT-$LOG4J_VER.jar
    URL=https://repo1.maven.org/maven2/org/apache/logging/log4j/$ARTIFACT/$LOG4J_VER/$JAR
    wget  --progress=dot:giga -O "$JAR" "$URL"
    wget  --progress=dot:giga -O "$JAR.asc" "$URL.asc"
    gpg --batch --verify "$JAR.asc" "$JAR"
done

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

# Switch Jetty server logging to log4j2 via the standard 'logging-log4j2' Jetty module, so the server and the
# webapp use the same logging system configured by the same file. The module jars are pre-placed into
# lib/logging, so Jetty downloads nothing. The ECS JSON layout goes to lib/ext, which the enabled 'ext' module
# adds to the server classpath. log4j2.version is pinned in the ini to keep the module wired to the jars
# supplied by this image even when Jetty's default log4j2 version differs.
ARG LOG4J_VER
COPY --from=log4j /log4j-api-*.jar /log4j-core-*.jar /log4j-slf4j2-impl-*.jar $OPENL_APP/lib/logging/
COPY --from=log4j /log4j-layout-template-json-*.jar /log4j-jul-*.jar $OPENL_APP/lib/ext/

# Bridge java.util.logging (Jakarta Faces and other JUL-based libraries) into log4j2, replacing the default
# JUL console handler, so this output flows through the single logging system as well.
RUN <<'EOT' cat > $OPENL_APP/etc/jul-bridge.xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Configure PUBLIC "-//Jetty//Configure//EN" "https://jetty.org/configure_10_0.dtd">
<Configure>
    <Call class="org.apache.logging.log4j.jul.Log4jBridgeHandler" name="install">
        <Arg type="boolean">true</Arg>
        <Arg/>
        <Arg type="boolean">true</Arg>
    </Call>
</Configure>
EOT

RUN <<EOT
set -euxv

cd $OPENL_APP
mkdir start.d
java -jar start.jar --add-modules=logging-log4j2 --approve-all-licenses log4j2.version=$LOG4J_VER
{
    echo "log4j2.version=$LOG4J_VER"
    # Hide the server-side lib/ext from webapps: they log through their own log4j2 from WEB-INF/lib
    echo 'jetty.webapp.addHiddenClasses+=,${jetty.home.uri}/lib/ext/'
} >> start.d/logging-log4j2.ini
printf '# Bridge java.util.logging into the single log4j2 logging system.\netc/jul-bridge.xml\n' > start.d/jul-bridge.ini
EOT

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
echo "|    To set the log output format:        LOGGING_FORMAT=ecs|otel|plain|none  (default ecs)      |"
echo "--------------------------------------------------------------------------------------------------"

# Select the log4j2 configuration for the chosen log output format.
LOGGING_FORMAT="$(echo "${LOGGING_FORMAT:-ecs}" | tr '[:upper:]' '[:lower:]')"
case "$LOGGING_FORMAT" in
    ecs|plain|none) ;;
    otel)
        # Disable log4j2 console output and let the OpenTelemetry agent capture log events and write them
        # to stdout in the OTLP JSON format
        LOGGING_FORMAT=none
        export OTEL_JAVAAGENT_ENABLED="${OTEL_JAVAAGENT_ENABLED:-true}"
        export OTEL_LOGS_EXPORTER="${OTEL_LOGS_EXPORTER:-experimental-otlp/stdout}"
        if [ -z "$(env | grep -E '^OTEL_EXPORTER_')" ]; then
            # No OTLP endpoint is defined: keep console logs only and mute the failing OTLP exporters
            export OTEL_TRACES_EXPORTER="${OTEL_TRACES_EXPORTER:-none}"
            export OTEL_METRICS_EXPORTER="${OTEL_METRICS_EXPORTER:-none}"
        fi
        ;;
    *)
        echo "WARN: Unsupported LOGGING_FORMAT='$LOGGING_FORMAT'. Supported values: ecs, otel, plain, none. Using 'ecs'"
        LOGGING_FORMAT=ecs
        ;;
esac
JAVA_OPTS="$JAVA_OPTS -Dlog4j2.configurationFile=$OPENL_DIR/log4j2-$LOGGING_FORMAT.properties"

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
-Dorg.eclipse.jetty.server.Request.maxFormContentSize=-1 \
-Dorg.eclipse.jetty.server.Request.maxFormKeys=-1 \
-Djetty.httpConfig.requestHeaderSize=32768 \
-Djetty.httpConfig.responseHeaderSize=32768 \
"
EOT

# Create log4j2 configurations for every LOGGING_FORMAT value. The Jetty server and the webapp read the same
# file via -Dlog4j2.configurationFile (see start.sh), so both log in the same output format. They override
# the plain-text configuration bundled in the .war and are applied only inside the container.

# ecs (default): cloud-friendly logging in Elastic Common Schema (ECS) JSON format on stdout.
RUN <<'EOT' cat > $OPENL_DIR/log4j2-ecs.properties
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

# plain: human-readable logging on stdout, like in the .war bundled configuration.
RUN <<'EOT' cat > $OPENL_DIR/log4j2-plain.properties
status = warn

appender.console.type = Console
appender.console.name = STDOUT
appender.console.layout.type = PatternLayout
appender.console.layout.pattern = [%-5level] %d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %c{20}%notEmpty{ id=%X{requestId}} - %msg%n

rootLogger.level = ${env:LOGGING_LEVEL_ROOT:-INFO}
rootLogger.appenderRef.console.ref = STDOUT
EOT

# none: discard all log events, nothing is written to the console. Log levels still apply, so the
# OpenTelemetry agent can capture log events when it is enabled (used by LOGGING_FORMAT=otel).
RUN <<'EOT' cat > $OPENL_DIR/log4j2-none.properties
status = warn

appender.null.type = Null
appender.null.name = NONE

rootLogger.level = ${env:LOGGING_LEVEL_ROOT:-INFO}
rootLogger.appenderRef.null.ref = NONE
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
# Add the ECS JSON layout to the webapp as well: Jetty hides the server-side log4j2 from webapps, so the
# webapp logs through its own log4j2 bundled in WEB-INF/lib. The layout is intentionally not bundled in the
# .war (which stays cloud-agnostic); the image supplies it so the webapp can use the shared configuration.
COPY --from=log4j /log4j-layout-template-json-*.jar $OPENL_APP/webapps/ROOT/WEB-INF/lib/

WORKDIR $OPENL_DIR

CMD ["/opt/openl/start.sh"]

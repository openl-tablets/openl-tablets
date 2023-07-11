FROM busybox as files

ARG APP=target/openl-tablets-demo.zip

COPY $APP demo.zip

# Unpack demo files
RUN set -eux ; \
    unzip -q demo.zip ; \
    rm demo.zip ; \
    \
    demoDir=$(ls | grep jetty-) ; \
    mv "$demoDir" /demo ; \
    \
    cd demo/webapps ; \
    \
# Permission for rootless mode (for running as non-root)
    cd .. ; \
    mkdir logs; \
    chmod o=u logs; \
    mkdir openl-demo; \
    chmod o=u openl-demo

FROM ubuntu:focal

LABEL org.opencontainers.image.title="OpenL Tablets DEMO"
LABEL org.opencontainers.image.description="Demo with preconfigured OpenL WebStudio, OpenL RuleServices and simple web client"
LABEL org.opencontainers.image.url="https://openl-tablets.org/"
LABEL org.opencontainers.image.vendor="OpenL Tablets"

ENV LC_ALL C.UTF-8

# Copy Java
ENV JAVA_HOME /opt/java/openjdk
ENV JAVA $JAVA_HOME/bin/java
COPY --from=eclipse-temurin:11-jre-focal $JAVA_HOME $JAVA_HOME

# Install fonts required for Apache POI (export into Excel with autowidth of columns)
RUN set -eux ; \
    \
    apt-get update ; \
    apt-get install -y --no-install-recommends \
    fontconfig ; \
    rm -rf /var/lib/apt/lists/*

RUN useradd -r -m -U -s /usr/sbin/nologin openl
USER openl

ENV JAVA_OPTS "-Xms64m -XX:MaxRAMPercentage=90.0"

EXPOSE 8080

COPY --from=files /demo /demo

WORKDIR /demo

#Start Jetty
CMD ["/demo/start", "batchMode"]

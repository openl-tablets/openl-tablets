FROM eclipse-temurin:24-jre-alpine AS layers
WORKDIR /layer
COPY target/*-fat.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:24-jre-alpine AS openl
# Set default JAVA_OPTS, can be overridden at runtime
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENV OPENL_DIR /opt/openl
ENV OPENL_HOME $OPENL_DIR/local
ENV OPENL_HOME_SHARED $OPENL_DIR/shared
ENV OPENL_APP $OPENL_DIR/app
ENV OPENL_LIB $OPENL_DIR/lib

WORKDIR $OPENL_DIR

COPY --from=layers /layer/dependencies/ ./
COPY --from=layers /layer/spring-boot-loader/ ./
COPY --from=layers /layer/snapshot-dependencies/ ./
COPY --from=layers /layer/application/ ./
COPY src/main/resources/application.properties application.properties

COPY entrypoint /usr/local/bin/
RUN chmod +x /usr/local/bin/entrypoint

ENTRYPOINT ["entrypoint"]

FROM eclipse-temurin:24-jre-alpine as openl
ENV OPENL_DIR /opt/openl
ENV OPENL_HOME $OPENL_DIR/local
ENV OPENL_HOME_SHARED $OPENL_DIR/shared
ENV OPENL_APP $OPENL_DIR/app
ENV OPENL_LIB $OPENL_DIR/lib

WORKDIR $OPENL_DIR
COPY target/lib/ lib
COPY target/example-app-0.1-SNAPSHOT.jar app.jar
COPY target/zips/ openl-repository
COPY src/main/resources/application.properties application.properties

ENTRYPOINT ["java", "-jar", "app.jar"]
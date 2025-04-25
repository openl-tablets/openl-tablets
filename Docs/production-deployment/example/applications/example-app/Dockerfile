# syntax=docker/dockerfile:1
FROM openltablets/ws:latest

COPY target/zips/ /opt/openl/openl-repository
COPY src/main/resources/application.properties /opt/openl/application.properties
COPY target/lib/ /opt/openl/app/webapps/ROOT/WEB-INF/lib

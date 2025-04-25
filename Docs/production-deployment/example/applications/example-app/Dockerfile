# syntax=docker/dockerfile:1

# Use the predefined image from Docker Hub as the base
FROM openltablets/ws:latest

COPY target/projects /opt/openl/openl-repository
COPY src/main/resources/application.properties /opt/openl/application.properties
# Use the default script from the base image to start the application
CMD ["/opt/openl/start.sh"]
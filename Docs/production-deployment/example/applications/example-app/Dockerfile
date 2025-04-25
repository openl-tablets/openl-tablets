# syntax=docker/dockerfile:1
FROM alpine:3.19 AS unpacker

RUN apk add --no-cache unzip
# Copy the zipped artifacts from Maven build output
COPY target/zips/ /tmp/zips/

# Unpack each zip into its own folder
RUN mkdir -p /tmp/projects && \
    for file in /tmp/zips/*.zip; do \
        name=$(basename "$file" .zip); \
        mkdir -p "/tmp/projects/$name"; \
        unzip -o "$file" -d "/tmp/projects/$name"; \
    done
# Use the predefined image from Docker Hub as the base
FROM openltablets/ws:latest

COPY --from=unpacker /tmp/projects /opt/openl/openl-repository
COPY src/main/resources/application.properties /opt/openl/application.properties
COPY target/lib/ /opt/openl/app/webapps/ROOT/WEB-INF/lib
# Use the default script from the base image to start the application
CMD ["/opt/openl/start.sh"]
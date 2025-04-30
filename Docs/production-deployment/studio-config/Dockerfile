# /Users/aromashko/genesis/src/openl-tablets/Docs/production-deployment/studio-config/Dockerfile
# Start from the official WebStudio image
FROM openltablets/webstudio:latest

# Metadata
LABEL maintainer="Your Name <your.email@example.com>"
LABEL description="WebStudio image with PostgreSQL JDBC driver included"

# Environment variables for the driver
ENV DRIVER_VERSION=42.7.3
ENV DRIVER_URL="https://jdbc.postgresql.org/download/postgresql-${DRIVER_VERSION}.jar"
ENV TARGET_DIR="/opt/openl/lib"
ENV TARGET_FILE="${TARGET_DIR}/postgresql-${DRIVER_VERSION}.jar"

USER root

ADD ${DRIVER_URL} ${TARGET_FILE}
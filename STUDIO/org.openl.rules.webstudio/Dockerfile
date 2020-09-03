FROM tomcat:9.0.37-jdk11-openjdk-slim

ENV JAVA_OPTS="-Xms128m -XX:MaxRAMPercentage=90.0"

COPY server.xml /usr/local/tomcat/conf/
# Delete existing Web applications
RUN rm -rf webapps/* &&\
    apt-get update && apt-get install -y fontconfig

# Copy WebStudio application
COPY target/webstudio/ webapps/ROOT/

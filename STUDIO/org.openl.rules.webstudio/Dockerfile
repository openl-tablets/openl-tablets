FROM tomcat:9.0.30-jdk11

ENV JAVA_OPTS="-Xms128m -XX:MaxRAMPercentage=90.0"

COPY server.xml /usr/local/tomcat/conf/
# Delete existing Web applications
RUN rm -rf webapps/*

# Copy WebStudio application
COPY target/webstudio/ webapps/ROOT/

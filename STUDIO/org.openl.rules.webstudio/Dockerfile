FROM tomcat:9.0.30-jdk11

COPY server.xml /usr/local/tomcat/conf/
# Delete existing Web applications
RUN rm -rf webapps/*

# Copy WebStudio application
COPY target/webstudio/ webapps/ROOT/

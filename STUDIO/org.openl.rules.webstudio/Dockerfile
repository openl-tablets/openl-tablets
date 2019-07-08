FROM tomcat:9-jdk11

# Delete existing Web applications
RUN rm -rf webapps/*

# Copy WebStudio application
COPY target/webstudio/ webapps/ROOT/

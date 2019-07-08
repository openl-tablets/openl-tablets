FROM tomcat:9-jdk8

# Delete existing Web applications
RUN rm -rf webapps/*

# Copy WebStudio application
COPY target/webstudio/ webapps/ROOT/

FROM tomcat:9-jdk11

# Delete existing Web applications
RUN rm -rf webapps/*

# Copy WebService application
COPY target/webservice/ webapps/ROOT/

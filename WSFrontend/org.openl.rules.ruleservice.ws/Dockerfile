FROM tomcat:9.0.37-jdk11

ENV JAVA_OPTS="-Xms32m -XX:MaxRAMPercentage=90.0"

# Delete existing Web applications
RUN rm -rf webapps/*

# Copy WebService application
COPY target/webservice/ webapps/ROOT/

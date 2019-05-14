FROM tomcat:8.5-jre11

RUN apt-get update && apt-get install -y --no-install-recommends \
      bash \
      unzip \
      ttf-dejavu \
    && rm -rf /var/lib/apt/lists/*

COPY ./target/*.war /tmp/

# Unzip war into webapps dir && Remove temporal ws war && Make .openl dir
RUN unzip /tmp/*.war -d /usr/local/tomcat/webapps/webstudio && chmod u=rw,g=r,o=r $(find /usr/local/tomcat/webapps/webstudio -type f) && rm /tmp/*.war && mkdir /root/.openl && mkdir -p /usr/local/tomcat/shared/lib

# Copy tomcat configuration files
COPY ./docker/server.xml /usr/local/tomcat/conf/
RUN chmod u=rw,g=r,o=r /usr/local/tomcat/conf/server.xml
COPY ./docker/catalina.properties /usr/local/tomcat/conf/
RUN chmod u=rw,g=r,o=r /usr/local/tomcat/conf/catalina.properties

EXPOSE 80

#Start Tomcat
CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]

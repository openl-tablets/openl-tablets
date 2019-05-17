FROM tomcat:8.5-alpine

RUN apk add --update \
      bash \
      unzip \
      ttf-dejavu \
    && rm -rf /var/cache/apk/*

COPY ./target/*.war /tmp/

# Unzip war into webapps dir && Remove temporal ws war && Make .openl dir
RUN unzip /tmp/*.war -d /usr/local/tomcat/webapps/webstudio && chmod u=rw,g=r,o=r $(find /usr/local/tomcat/webapps/webstudio -type f) && rm /tmp/*.war && mkdir /root/.openl && mkdir -p /usr/local/tomcat/shared/lib

# Copy tomcat configuration files
COPY ./docker/server.xml /usr/local/tomcat/conf/
COPY ./docker/catalina.properties /usr/local/tomcat/conf/
RUN chmod u=rw,g=r,o=r /usr/local/tomcat/conf/server.xml && chmod u=rw,g=r,o=r /usr/local/tomcat/conf/catalina.properties

EXPOSE 80

#Start Tomcat
CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]
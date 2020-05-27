FROM openjdk:11.0.5-stretch

ENV TOMCAT_VERSION=9.0.30

ENV JAVA_OPTS="-XX:MaxRAMPercentage=90.0"
ENV CONTAINER="true"

RUN apt-get update && apt-get install -y --no-install-recommends \
      bash \
      unzip \
      procps \
      && rm -rf /var/lib/apt/lists/*

COPY ./target/*.zip /tmp/
    
# Unzip war into webapps dir && Remove temporal ws war && Make .openl dir
RUN unzip /tmp/*.zip -d /usr/local/ && rm /tmp/*.zip

RUN chmod +x /usr/local/apache-tomcat-${TOMCAT_VERSION}/*.sh && chmod +x /usr/local/apache-tomcat-${TOMCAT_VERSION}/bin/*.sh

WORKDIR /usr/local/apache-tomcat-${TOMCAT_VERSION}

EXPOSE 8080

#Start Tomcat
CMD ./start.sh && tail -f /dev/null

## Configuration of OpenL Rule Services

OpenL Rule Services support a **similar** way of externalizing configuration as in
[Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config).

Configuration can be provided through:

1. init-params from the Servlet Context (if applicable).
2. JNDI attributes from `java:comp/env` (if applicable).
3. Java System properties.
4. OS environment variables.
5. application.properties files, located at:
    1. the user's home directory (~/application.properties).
    2. the current working directory (./application.properties).
    3. inside the JAR file (classpath:application.properties).

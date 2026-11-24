## OpenL Rule Services in Spring Boot

OpenL Rule Services runs inside a Spring Boot application in one of two ways:

- **As a web service** — the embedded server of Spring Boot serves the REST services of the projects, their OpenAPI
  documents and the Rule Services pages, as the standalone OpenL Rule Services does. The application depends on
  `org.openl.rules.ruleservice.ws`.
- **As a Java library** — no web service is published. The beans of the application run the rules through the
  `RulesFrontend` bean. The application depends on `org.openl.rules.ruleservice`.

A complete Maven build of such an application, with rules projects, an extension, tests and a Docker image, is the
[OpenL Rule Services Example](https://github.com/openl-tablets/openl-tablets/tree/main/Docs/examples/production/example).

This section explains the following:

- [Running OpenL Rule Services as a Web Service](#running-openl-rule-services-as-a-web-service)
- [Running OpenL Rule Services as a Java Library](#running-openl-rule-services-as-a-java-library)
- [Limitations](#limitations)

### Running OpenL Rule Services as a Web Service

Add OpenL Rule Services, the web starter of Spring Boot and the rules projects to the dependencies of the application:

```xml
<dependency>
    <groupId>org.openl.rules</groupId>
    <artifactId>org.openl.rules.ruleservice.ws</artifactId>
    <version>${openl.version}</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>com.example</groupId>
    <artifactId>example-rules</artifactId>
    <version>1.0.0</version>
</dependency>
```

Register the servlet, the filter and the context listener of OpenL Rule Services by scanning their package:

```java
@SpringBootApplication
@ServletComponentScan("org.openl.rules.ruleservice.servlet")
public class RulesApplication {
    public static void main(String[] args) {
        SpringApplication.run(RulesApplication.class, args);
    }
}
```

#### Rules Projects

The default data source, [Classpath JAR](configuration.md#classpath-jar), deploys the projects found in the classpath
of the application:

- **A JAR or a ZIP dependency** with `rules.xml`, `deployment.xml` or `deployment.yaml` at its root, such as a project
  built by `openl-maven-plugin` with the `openl-jar` packaging. Spring Boot packs it into `BOOT-INF/lib`.
- **A ZIP archive in `src/main/resources/openl/`**. Spring Boot packs it into `BOOT-INF/classes/openl/`.

The executable JAR built by `spring-boot-maven-plugin` runs as it is:

```bash
java -jar target/rules-application.jar
```

OpenL Rule Services reads the projects inside the executable JAR in place, through Spring Boot. This needs the default
loader of the plugin, which stores the nested JAR and ZIP files uncompressed. A project the JAR keeps otherwise, such
as one packed with the `CLASSIC` loader implementation, is skipped with a warning in the log.

> [!Note]
> Before OpenL Tablets 7.1.0, the projects of an executable JAR are deployed only when the JAR is unpacked first, as
> the Docker image of the example does: it extracts the layers of the JAR and starts the `JarLauncher` class.

To deploy the projects from another data source, such as a folder outside the application, configure it as
described in [Configuring a Data Source](configuration.md#configuring-a-data-source).

#### Configuration

OpenL Rule Services runs in a Spring application context of its own, beside the one of Spring Boot. It reads its
properties from:

- `application.properties` in the classpath and in the working folder, the same files Spring Boot reads;
- Java system properties;
- environment variables.

It does not read the command-line arguments of Spring Boot, such as `--production-repository.factory=repo-zip`. Pass a
setting as a system property or as an environment variable instead:

```bash
java -Dproduction-repository.factory=repo-zip -Dproduction-repository.uri=/opt/openl/deployments \
     -jar target/rules-application.jar
```

```bash
PRODUCTION_REPOSITORY_FACTORY=repo-zip PRODUCTION_REPOSITORY_URI=/opt/openl/deployments \
     java -jar target/rules-application.jar
```

The beans of the Spring Boot application are not visible to OpenL Rule Services. Declare an extension, such as an
authorization checker, as a Spring configuration of OpenL Rule Services, as described in
[OpenL Rule Services Customization Algorithm](advanced-configuration.md#openl-rule-services-customization-algorithm).

#### Spring MVC Controllers and Spring Boot Actuator

The servlet of OpenL Rule Services is mapped to `/*`, so it answers every request, and a controller of Spring MVC or an
endpoint of Spring Boot Actuator answers `404` with `No service was found`. Choose one of the following:

- **Serve Actuator on a port of its own** — Actuator gets a server of its own, and OpenL Rule Services keeps the root
  path:

  ```properties
  management.server.port=8081
  ```

- **Serve OpenL Rule Services under a path** — register the servlet, the filter and the context listener of OpenL Rule
  Services yourself, instead of `@ServletComponentScan`. The servlet and the filter take the same path. The services
  are then available at `/rules/<service>`, the Rule Services pages at `/rules/`, and the OpenAPI documents name
  the path in their server address.

  ```java
  @SpringBootApplication
  public class RulesApplication {

      public static void main(String[] args) {
          SpringApplication.run(RulesApplication.class, args);
      }

      @Bean
      ServletListenerRegistrationBean<SpringInitializer> openlContext() {
          return new ServletListenerRegistrationBean<>(new SpringInitializer());
      }

      @Bean
      ServletRegistrationBean<CXFServlet> openlServlet() {
          var registration = new ServletRegistrationBean<>(new CXFServlet(), "/rules/*");
          registration.setLoadOnStartup(1);
          registration.addInitParameter("service-list-path", "cxf-services");
          registration.addInitParameter("hide-service-list-page", "true");
          return registration;
      }

      @Bean
      FilterRegistrationBean<RuleServicesFilter> openlFilter() {
          var registration = new FilterRegistrationBean<>(new RuleServicesFilter());
          registration.addUrlPatterns("/rules/*");
          registration.addInitParameter("xForwardedPrefixStrategy", "PREPEND");
          return registration;
      }
  }
  ```

  The three classes are in the `org.openl.rules.ruleservice.servlet` package.

#### Logging

OpenL Rule Services logs through SLF4J and the Log4j API. The `spring-boot-starter-logging` starter, which the web
starter includes, routes both to Logback, so OpenL Rule Services writes to the log of the application, and no
dependency needs to be excluded.

#### Service Discovery

The application registers itself in a service registry, such as Eureka, through the client of Spring Cloud. Add the
client with the Spring Cloud release that matches the version of Spring Boot:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

Then name the application and the registry in `application.properties`:

```properties
spring.application.name=openl-rules
eureka.client.service-url.defaultZone=http://eureka.example.com:8761/eureka/
```

The client registers the address of the embedded server, the one the REST services of OpenL Rule Services answer on.
The standalone OpenL Rule Services, the web application and its Docker image, has no client of a service registry.

### Running OpenL Rule Services as a Java Library

Add the core of OpenL Rule Services and the rules projects to the dependencies of the application:

```xml
<dependency>
    <groupId>org.openl.rules</groupId>
    <artifactId>org.openl.rules.ruleservice</artifactId>
    <version>${openl.version}</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>
```

Import the bean configuration of OpenL Rule Services and inject the `RulesFrontend` bean, as described in
[Rule Services Core](core.md):

```java
@SpringBootApplication
@ImportResource("classpath:openl-ruleservice-beans.xml")
public class RulesApplication {

    public static void main(String[] args) {
        SpringApplication.run(RulesApplication.class, args);
    }

    @Bean
    CommandLineRunner greet(RulesFrontend rules) {
        return args -> System.out.println(rules.execute("example-rules", "hello", "World"));
    }
}
```

The beans of OpenL Rule Services are beans of the Spring Boot application, so they read the properties of Spring Boot,
the command-line arguments included, and the beans of the application inject them as any other bean.

> [!Note]
> When `org.openl.rules.ruleservice.ws` is in the classpath too, set `ruleservice.publishers=JAVA`. Otherwise, the
> application fails to start: the default `RESTFUL` publisher of that module is not available without the web service.

### Limitations

- OpenL Tablets provides no Spring Boot starter: the application declares the dependencies and the registration shown
  above itself.
- As a web service, OpenL Rule Services keeps its own Spring application context, so its beans and the beans of the
  application do not see each other, and it reads its own property sources, as described in
  [Configuration](#configuration).

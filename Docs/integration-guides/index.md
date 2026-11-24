# Integration Guides

These pages describe how to extend and observe OpenL Rule Services.

- [Spring Framework](spring.md) — add Spring configurations to Rule Services, in Java or in
  `META-INF/openl/extension-*.xml`.
- [Spring Boot](../user-guides/rule-services/spring-boot.md) — run Rule Services inside a Spring Boot application,
  register it in Eureka, and keep Spring MVC and Actuator beside it.
- [OpenAPI](openapi.md) — change the OpenAPI schema of a service with `openapi-configuration*.json` files.
- [Apache CXF](cxf.md) — add CXF features, such as the logging feature, to the REST endpoints.
- [OpenTelemetry](opentelemetry.md) — trace services with the OpenTelemetry Java agent of the Docker images.

## Related Documentation

- [Rule Services Guide](../user-guides/rule-services/index.md) — configuration and deployment of Rule Services.
- [Developer Guide](../developer-guides/index.md) — rules projects, the language and externalized configuration.
- [Deployment](../DEPLOYMENT.md) — Docker images and their environment variables.

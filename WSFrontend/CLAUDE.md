# WSFrontend Module - Rule Services Conventions

**Module**: WSFrontend (Web Services & Rule Deployment)
**Location**: `/home/user/openl-tablets/WSFrontend/`
**Version**: 6.0.0-SNAPSHOT
**Last Updated**: 2025-11-05

---

## Module Purpose

The WSFrontend module provides **production rule deployment and web services**, including:
- Rule service deployment and management
- RESTful web services (JAX-RS)
- SOAP web services (JAX-WS)
- Apache Kafka message processing
- Request/response logging and storage
- Service annotations and metadata
- Dynamic service generation from rule projects

**Critical**: This is a production-facing module. Changes affect deployed services and runtime performance.

---

## Submodules Overview

| Submodule | Purpose | Technology | Sensitivity |
|-----------|---------|------------|-------------|
| **`org.openl.rules.ruleservice`** | **Core service engine** | Spring Boot | 🔴 CRITICAL |
| **`org.openl.rules.ruleservice.ws`** | Web services (WAR) | Apache CXF, JAX-RS | 🔴 CRITICAL |
| **`org.openl.rules.ruleservice.ws.all`** | WS with all plugins | Spring Boot | 🟡 Medium risk |
| **`org.openl.rules.ruleservice.deployer`** | Service deployer | Spring | 🔴 CRITICAL |
| **`org.openl.rules.ruleservice.kafka`** | Kafka integration | Spring Kafka | 🟡 Medium risk |
| **`org.openl.rules.ruleservice.annotation`** | Service annotations | Java | 🟢 Low risk |
| **`org.openl.rules.ruleservice.common`** | Common utilities | Java | 🟢 Low risk |
| **`org.openl.rules.ruleservice.ws.annotation`** | WS annotations | JAX-RS | 🟢 Low risk |
| **`org.openl.rules.ruleservice.ws.common`** | WS utilities | Apache CXF | 🟡 Medium risk |
| **`org.openl.rules.ruleservice.ws.storelogdata`** | Logging abstraction | Spring | 🟡 Medium risk |
| **`org.openl.rules.ruleservice.ws.storelogdata.db`** | Database logging | JPA | 🟡 Medium risk |

---

## Technology Stack

### Core Technologies
- **Framework**: Spring Boot 3.5.6
- **Web Services**: Apache CXF 4.1.3
- **REST**: JAX-RS 3.1.0
- **SOAP**: JAX-WS 4.0.0
- **Messaging**: Spring Kafka 3.3.5
- **Serialization**: Jackson 2.19.0
- **Build Tool**: Maven 3.9.9+

### Service Protocols
- **REST/JSON**: Primary protocol
- **SOAP/XML**: Legacy protocol (still supported)
- **Apache Kafka**: Message-based invocation
- **OpenAPI**: Service documentation

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│              Client Applications                        │
│   (REST Clients, SOAP Clients, Kafka Producers)       │
└────────────┬───────────────┬───────────┬───────────────┘
             │               │           │
    ┌────────▼──────┐  ┌────▼─────┐  ┌─▼──────────┐
    │  REST/JSON    │  │ SOAP/XML │  │   Kafka    │
    │  (JAX-RS)     │  │ (JAX-WS) │  │  Consumer  │
    └────────┬──────┘  └────┬─────┘  └─┬──────────┘
             │               │           │
             └───────────────┼───────────┘
                             │
          ┌──────────────────▼─────────────────────┐
          │   org.openl.rules.ruleservice.ws       │
          │   (Apache CXF Service Layer)           │
          ├────────────────────────────────────────┤
          │  - Service publishing                  │
          │  - Request/response interceptors       │
          │  - Exception handling                  │
          │  - Request logging                     │
          └──────────────────┬─────────────────────┘
                             │
          ┌──────────────────▼─────────────────────┐
          │   org.openl.rules.ruleservice          │
          │   (Core Service Engine)                │
          ├────────────────────────────────────────┤
          │  - Service deployer                    │
          │  - Service manager                     │
          │  - Rule compilation                    │
          │  - Version management                  │
          └──────────────────┬─────────────────────┘
                             │
          ┌──────────────────▼─────────────────────┐
          │   DEV Module                           │
          │   (Core Rules Engine)                  │
          │   - Compilation                        │
          │   - Execution                          │
          └────────────────────────────────────────┘
```

---

## Core Concepts

### 1. Rule Service

A **Rule Service** is a dynamically generated web service that exposes OpenL rule methods as service operations.

**Flow**:
```
Rule Project → Service Deployment → Interface Generation → Service Publishing
```

**Example**:
```
Excel File: AutoInsurance.xlsx
Rule Method: calculatePremium(Driver driver, Vehicle vehicle)
↓
Generated Service: AutoInsuranceService
REST Endpoint: POST /AutoInsuranceService/calculatePremium
SOAP Operation: <calculatePremium> SOAP message
```

### 2. Service Deployment

Services are deployed from **rule projects** stored in repositories.

**Deployment Configuration** (`rules-deploy.xml`):
```xml
<rules>
  <serviceName>AutoInsuranceService</serviceName>
  <url>AutoInsuranceService</url>
  <serviceClass>org.example.AutoInsuranceService</serviceClass>
  <ruleServiceConfiguration>
    <project>rules/AutoInsurance</project>
    <version>1.0.0</version>
  </ruleServiceConfiguration>
</rules>
```

### 3. Service Versioning

Multiple versions of a service can be deployed simultaneously:

```
AutoInsuranceService v1.0.0
AutoInsuranceService v1.1.0
AutoInsuranceService v2.0.0 (breaking changes)
```

**Accessing Specific Versions**:
- REST: `/AutoInsuranceService/v2.0.0/calculatePremium`
- SOAP: Use WSDL version parameter

---

## Coding Conventions

### Service Configuration

#### Deployment Descriptor
```xml
<!-- rules-deploy.xml -->
<rules>
  <serviceName>MyRuleService</serviceName>
  <url>MyRuleService</url>
  <serviceClass>com.example.IMyRuleService</serviceClass>

  <ruleServiceConfiguration>
    <project>rules/MyRules</project>
    <lazy-modules-for-compilation>false</lazy-modules-for-compilation>
    <support-variations>true</support-variations>
    <support-runtime-context>true</support-runtime-context>
  </ruleServiceConfiguration>

  <publishers>
    <publisher>RESTFUL</publisher>
    <publisher>WEBSERVICE</publisher>
    <publisher>KAFKA</publisher>
  </publishers>
</rules>
```

### Custom Service Annotations

```java
/**
 * Annotate service interface methods to customize behavior
 */
@ServiceExtraMethod(
    value = ServiceExtraMethodHandler.class,
    runtimeContextInGlobalContext = true
)
public class CustomRuleService {

    @VariationPath("variation")
    public Result calculate(
            @RuntimeContext IRulesRuntimeContext context,
            Input input) {
        // Service method implementation
        return ruleService.calculate(context, input);
    }
}
```

### Exception Handling

```java
/**
 * Custom exception handler for rule service exceptions
 */
@Component
public class RuleServiceExceptionHandler implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        // Log the exception
        log.error("Service invocation failed", exception);

        // Create error response
        ErrorResponse error = new ErrorResponse(
            exception.getMessage(),
            ErrorCode.INTERNAL_ERROR
        );

        // Return appropriate HTTP status
        return Response
            .status(Status.INTERNAL_SERVER_ERROR)
            .entity(error)
            .build();
    }
}
```

### Request/Response Logging

```java
/**
 * Configure request/response logging
 */
@Configuration
public class LoggingConfiguration {

    @Bean
    public StoreLogDataService storeLogDataService() {
        return new DatabaseStoreLogDataService(
            dataSource(),
            LoggingLevel.DETAILED
        );
    }

    @Bean
    public LoggingInterceptor loggingInterceptor() {
        LoggingInterceptor interceptor = new LoggingInterceptor();
        interceptor.setStoreRequest(true);
        interceptor.setStoreResponse(true);
        interceptor.setStoreExceptions(true);
        return interceptor;
    }
}
```

---

## Module-Specific Guidelines

### 1. Core Service Engine (`org.openl.rules.ruleservice`)

**Purpose**: Core engine for deploying and managing rule services

#### Key Components

**ServiceManager**: Manages service lifecycle
```java
public interface ServiceManager {
    void deploy(ServiceDescription serviceDescription);
    void undeploy(String serviceName);
    Collection<ServiceDescription> getServices();
    Service getServiceByName(String serviceName);
}
```

**RuleServiceDeployer**: Deploys services from configurations
```java
public interface RuleServiceDeployer {
    void deploy() throws RuleServiceDeployException;
    void undeploy(String serviceName);
    void redeploy(String serviceName);
}
```

**RuleServiceInstantiationStrategy**: Creates service instances
```java
public interface RuleServiceInstantiationStrategy {
    Object instantiate(CompiledOpenClass compiledClass);
    void reset();
}
```

#### Best Practices
- ✅ Validate service configuration before deployment
- ✅ Handle concurrent deployments safely
- ✅ Clean up resources on undeployment
- ✅ Log all deployment events
- ⚠️ Never modify services while they're being invoked
- ⚠️ Be careful with class loaders (can cause memory leaks)

### 2. Web Services (`org.openl.rules.ruleservice.ws`)

**Purpose**: Exposes rule services as REST/SOAP endpoints

#### REST Service Publishing

**Automatic**:
```java
// Service interface is automatically exposed as REST
// POST /MyService/methodName
// Content-Type: application/json
```

**Custom JAX-RS Annotations**:
```java
@Path("/custom")
@Produces(MediaType.APPLICATION_JSON)
public interface IMyService {

    @POST
    @Path("/calculate")
    Double calculate(Input input);

    @GET
    @Path("/version")
    String getVersion();
}
```

#### SOAP Service Publishing

**Automatic WSDL Generation**:
```
http://localhost:8080/MyService?wsdl
```

**Custom SOAP Annotations**:
```java
@WebService
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
public interface IMyService {

    @WebMethod
    @WebResult(name = "result")
    Double calculate(@WebParam(name = "input") Input input);
}
```

#### Best Practices
- ✅ Use DTOs for service contracts (not internal classes)
- ✅ Version your service interfaces
- ✅ Document with OpenAPI/Swagger annotations
- ✅ Validate all inputs
- ✅ Handle exceptions gracefully
- ⚠️ Never expose internal implementation details
- ⚠️ Be careful with large payloads (use streaming)

### 3. Kafka Integration (`org.openl.rules.ruleservice.kafka`)

**Purpose**: Process rule invocations via Kafka messages

#### Configuration

```yaml
# application.yml
ruleservice:
  kafka:
    enabled: true
    consumer:
      bootstrap-servers: localhost:9092
      group-id: openl-ruleservice
      topics:
        - rule-requests
    producer:
      bootstrap-servers: localhost:9092
      topic: rule-responses
```

#### Message Format

**Request**:
```json
{
  "serviceName": "AutoInsuranceService",
  "methodName": "calculatePremium",
  "parameters": [
    {
      "type": "Driver",
      "value": {"age": 25, "yearsLicensed": 7}
    },
    {
      "type": "Vehicle",
      "value": {"make": "Toyota", "model": "Camry", "year": 2020}
    }
  ]
}
```

**Response**:
```json
{
  "result": 1250.50,
  "error": null
}
```

#### Best Practices
- ✅ Use correlation IDs for request tracking
- ✅ Configure dead letter topics for failures
- ✅ Implement idempotent processing
- ✅ Monitor consumer lag
- ⚠️ Handle deserialization errors gracefully
- ⚠️ Be careful with message size limits

### 4. Request/Response Logging (`org.openl.rules.ruleservice.ws.storelogdata`)

**Purpose**: Store service invocation logs for audit and debugging

#### Database Schema

```sql
-- Service invocation log
CREATE TABLE service_log (
    id BIGINT PRIMARY KEY,
    service_name VARCHAR(255),
    method_name VARCHAR(255),
    request CLOB,
    response CLOB,
    exception CLOB,
    timestamp TIMESTAMP,
    duration_ms BIGINT,
    status VARCHAR(50)
);
```

#### Configuration

```yaml
ruleservice:
  logging:
    enabled: true
    store-request: true
    store-response: true
    store-exception: true
    datasource:
      url: jdbc:h2:mem:servicedb
      driver: org.h2.Driver
```

#### Custom Log Storage

```java
/**
 * Implement custom log storage (e.g., ElasticSearch)
 */
@Component
public class ElasticSearchStoreLogDataService implements StoreLogDataService {

    @Override
    public void save(StoreLogData logData) {
        // Store in ElasticSearch
        elasticSearchClient.index(
            "service-logs",
            logData.toJson()
        );
    }
}
```

---

## Testing Requirements

### Unit Tests

**Framework**: JUnit 5, Mockito

**Example**:
```java
@ExtendWith(MockitoExtension.class)
class ServiceManagerTest {

    @Mock
    private RuleServiceLoader ruleServiceLoader;

    @InjectMocks
    private ServiceManagerImpl serviceManager;

    @Test
    void testDeployService() {
        // Arrange
        ServiceDescription desc = new ServiceDescription();
        desc.setName("TestService");

        when(ruleServiceLoader.load(any()))
            .thenReturn(createMockService());

        // Act
        serviceManager.deploy(desc);

        // Assert
        assertNotNull(serviceManager.getServiceByName("TestService"));
    }
}
```

### Integration Tests

**Location**: `/ITEST/itest.webservice.rest/`, `/ITEST/itest.webservice.soap/`

**Requirements**:
- Test with real rule projects
- Test REST and SOAP endpoints
- Test error scenarios
- Test concurrent access
- Use TestContainers for Kafka tests

**Example**:
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "production-repository.uri=classpath:rules"
})
class RuleServiceIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testInvokeRuleService() {
        // Arrange
        String url = "http://localhost:" + port + "/MyService/calculate";
        Input input = new Input("test");

        // Act
        ResponseEntity<Double> response = restTemplate.postForEntity(
            url,
            input,
            Double.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(123.45, response.getBody());
    }
}
```

---

## Common Tasks

### Task: Deploy a New Rule Service

**Steps**:
1. Create rule project in WebStudio
2. Publish to production repository
3. Create `rules-deploy.xml` configuration
4. Deploy to WSFrontend
5. Verify service is accessible
6. Test with REST/SOAP client
7. Monitor logs for errors

**Configuration Example**:
```xml
<rules>
  <serviceName>NewService</serviceName>
  <url>NewService</url>
  <serviceClass>com.example.INewService</serviceClass>
  <ruleServiceConfiguration>
    <project>rules/NewService</project>
  </ruleServiceConfiguration>
</rules>
```

### Task: Add Kafka Support to Existing Service

**Steps**:
1. Add Kafka publisher to `rules-deploy.xml`
2. Configure Kafka connection properties
3. Create request/response topics
4. Test with Kafka producer/consumer
5. Monitor consumer lag
6. Set up dead letter topics

### Task: Add Custom Logging

**Steps**:
1. Implement `StoreLogDataService` interface
2. Register as Spring bean
3. Configure logging properties
4. Test log storage
5. Create log cleanup job (if needed)

---

## Performance Considerations

### Service Initialization
- **Lazy compilation**: Only compile when first invoked
- **Eager compilation**: Compile all services on startup (recommended for production)
- **Class caching**: Cache compiled classes to avoid recompilation

### Runtime Optimization
- **Connection pooling**: For database logging
- **Async logging**: Don't block service invocation
- **Thread pools**: Configure appropriate pool sizes
- **Request batching**: For Kafka processing

### Memory Management
- **Class loader cleanup**: Undeploy properly to avoid memory leaks
- **Request/response limits**: Prevent large payloads
- **Log retention**: Clean up old logs regularly

**Configuration**:
```yaml
ruleservice:
  performance:
    lazy-compilation: false  # Eager compilation
    use-single-module-mode: false
    thread-pool-size: 100
    cache-size: 1000
```

---

## Security Considerations

### Service Authentication

**Spring Security Integration**:
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/*/swagger-ui/**").permitAll()
                .requestMatchers("/**").authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .build();
    }
}
```

### Input Validation
```java
/**
 * Validate all service inputs
 */
@Component
public class InputValidator {

    public void validate(Object input) {
        if (input == null) {
            throw new ValidationException("Input cannot be null");
        }

        // Validate fields
        if (input instanceof Driver driver) {
            if (driver.getAge() < 16 || driver.getAge() > 120) {
                throw new ValidationException("Invalid age");
            }
        }
    }
}
```

### Rate Limiting
```java
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter = RateLimiter.create(100.0); // 100 req/sec

    @Override
    public boolean preHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler) {
        if (!rateLimiter.tryAcquire()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return false;
        }
        return true;
    }
}
```

---

## Common Issues & Solutions

### Issue: ClassNotFoundException After Undeployment
**Symptom**: Classes from undeployed service still referenced
**Solution**: Ensure proper class loader cleanup, restart if needed

### Issue: OutOfMemoryError
**Symptom**: Too many deployed services or large rule projects
**Solution**: Increase heap size, enable lazy compilation, reduce logging

### Issue: Slow Service Response
**Symptom**: First invocation slow, subsequent fast
**Solution**: Use eager compilation, warm up services on startup

### Issue: Kafka Consumer Lag
**Symptom**: Messages backing up in Kafka topics
**Solution**: Increase consumer threads, scale horizontally, optimize rule performance

---

## Production Deployment

### Configuration

**application.yml**:
```yaml
spring:
  profiles:
    active: production

ruleservice:
  production-repository:
    uri: s3://my-bucket/rules
    aws:
      region: us-east-1
      access-key: ${AWS_ACCESS_KEY}
      secret-key: ${AWS_SECRET_KEY}

  performance:
    lazy-compilation: false
    thread-pool-size: 200

  logging:
    enabled: true
    store-request: true
    store-response: false  # Reduce storage
    retention-days: 30

  kafka:
    enabled: true
    consumer:
      bootstrap-servers: kafka1:9092,kafka2:9092,kafka3:9092
```

### Monitoring

**Metrics to Monitor**:
- Service invocation rate
- Service response time (p50, p95, p99)
- Error rate
- Active services count
- Memory usage
- Kafka consumer lag (if using Kafka)

**Using Actuator**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

---

## Documentation References

- **Module Analysis**: [WSFrontend Overview](../docs/analysis/studio-wsfrontend-util-overview.md)
- **Root Conventions**: [/CLAUDE.md](../CLAUDE.md)
- **Architecture**: [docs/architecture/](../docs/architecture/)
- **Production Deployment**: [docs/operations/production-deployment.md](../docs/operations/production-deployment.md)
- **Performance Tuning**: [docs/guides/performance-tuning.md](../docs/guides/performance-tuning.md)

---

## When in Doubt

1. **Validate inputs** - Never trust client data
2. **Test in isolation** - Use integration tests
3. **Monitor in production** - Track metrics and logs
4. **Version carefully** - Breaking changes need new major version
5. **Document changes** - Update service documentation
6. **Ask for review** - WSFrontend changes affect production systems

---

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT
**Status**: Production-ready
**Criticality**: HIGH - Production-facing module

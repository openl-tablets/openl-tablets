# WSFrontend Module - Claude Code Conventions

**Module**: OpenL Tablets WSFrontend (Rule Services)
**Version**: 6.0.0-SNAPSHOT
**Last Updated**: 2025-12-02

This file provides specific guidance for the WSFrontend module. For general project conventions, see the root `/AGENTS.md`.

---

## Module Overview

WSFrontend provides rule services as REST/SOAP web services. It exposes compiled rules from DEV through a variety of integration methods.

**Key Components**:
- **Rule Services REST API** - Primary integration point
- **SOAP Services** - Legacy integration (Apache CXF)
- **Event Publishing** - Kafka integration
- **Deployment Service** - Rule artifact deployment
- **Logging & Tracing** - Observability (OpenTelemetry)
- **Cloud Storage** - MinIO/S3 integration
- **Metrics** - Prometheus/Actuator metrics

**Architecture Note**: WSFrontend is **stateless and horizontally scalable**. Rules are compiled once in DEV, deployed, and executed read-only in WSFrontend.

---

## Module Structure

```
WSFrontend/
├── org.openl.rules.ruleservice.ws/    # Main rule service application
│   ├── src/main/java/org/openl/rules/ruleservice/
│   │   ├── core/                      # Core rule service logic
│   │   ├── binding/                   # Service method binding
│   │   ├── publishing/                # Output/event publishing
│   │   ├── management/                # Service management
│   │   ├── rest/                      # REST API controllers
│   │   ├── soap/                      # SOAP endpoints
│   │   ├── servlet/                   # Servlets, filters
│   │   └── util/                      # Utilities
│   ├── src/main/resources/
│   │   ├── application*.properties    # Configuration
│   │   ├── rules.xml                  # Rule service definitions
│   │   └── openapi/                   # OpenAPI specs
│   └── test/
│
├── org.openl.rules.ruleservice.kafka/ # Kafka publishing
│   ├── src/main/java/org/openl/rules/ruleservice/kafka/
│   └── test/
│
├── org.openl.rules.ruleservice.deployer/ # Service deployer
├── org.openl.rules.store.logdata/    # Audit logging
├── org.openl.rules.ruleservice.tracing/ # OpenTelemetry
├── org.openl.rules.ruleservice.metrics/ # Prometheus metrics
└── ... other supporting modules
```

---

## Build & Development

### Building WSFrontend

```bash
# Full build with tests
cd WSFrontend && mvn clean install

# Quick build (recommended for development)
cd WSFrontend && mvn clean install -Dquick -DnoPerf -T1C

# Skip tests
cd WSFrontend && mvn clean install -DskipTests

# Build specific submodule
cd WSFrontend/org.openl.rules.ruleservice.ws && mvn clean install -DskipTests
```

### Running Rule Services

```bash
# Via Docker Compose
docker compose up
# Services: http://localhost:8081

# Via Maven
cd WSFrontend/org.openl.rules.ruleservice.ws
mvn spring-boot:run
```

### Testing

```bash
# All tests
cd WSFrontend && mvn test

# Integration tests
cd ITEST/itest.smoke && mvn verify

# Specific test
mvn test -Dtest=RuleServiceTest
```

---

## Architecture

### Request Flow

```
Client HTTP/SOAP Request
    ↓
Rule Service REST Controller / SOAP Endpoint
    ↓
Service Method Binding (map HTTP to rule method)
    ↓
Compiled Rule Execution (from DEV engine)
    ↓
Output Publishing (optional: Kafka, logging)
    ↓
Response (JSON/XML/SOAP)
```

### Key Systems

**Rule Service Registry**:
- Discovers deployed rule services
- Maps HTTP paths to rule methods
- Handles method invocation
- Manages service versioning

**Service Publishing**:
- **REST**: JAX-RS, Apache CXF
- **SOAP**: Apache CXF
- **Kafka**: Event publishing
- **Logging**: Audit trail, data logging

**Deployment**:
- Hot-deploy rule artifacts
- Version management
- Rollback capability
- Multi-tenant support (optional)

---

## REST API Development

### Controller Development

**Location**: `org.openl.rules.ruleservice.ws/src/org/openl/rules/ruleservice/rest/`

**Pattern**:

```java
@RestController
@RequestMapping("/api")
@OpenAPIDefinition(...)
public class RuleServiceController {

  private final RuleServiceFactory factory;

  @GetMapping("/services")
  @Operation(summary = "List rule services")
  public List<ServiceDescriptor> listServices() {
    return factory.getServices();
  }

  @PostMapping("/services/{serviceName}/methods/{methodName}")
  @Operation(summary = "Execute rule method")
  public ResponseEntity<?> executeMethod(
      @PathVariable String serviceName,
      @PathVariable String methodName,
      @RequestBody ExecutionRequest request) {
    return factory.executeMethod(serviceName, methodName, request);
  }
}
```

### OpenAPI Documentation

**File**: `src/main/resources/openapi/openapi.yaml`

```yaml
openapi: 3.0.0
info:
  title: OpenL Rule Services API
  version: 6.0.0
paths:
  /api/services:
    get:
      summary: List services
      responses:
        '200':
          description: Services list
```

**Requirements**:
- Document all endpoints
- Include request/response examples
- Document error codes
- Provide client code examples

### Input Validation

```java
@PostMapping("/services/{serviceName}/methods/{methodName}")
public ResponseEntity<?> executeMethod(
    @PathVariable
    @NotBlank
    String serviceName,
    @RequestBody
    @Valid
    ExecutionRequest request) {
  // ...
}
```

### Error Handling

```java
@RestControllerAdvice
public class RuleServiceExceptionHandler {

  @ExceptionHandler(RuleExecutionException.class)
  public ResponseEntity<ErrorResponse> handleRuleError(RuleExecutionException e) {
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(new ErrorResponse("Rule execution failed", e.getMessage()));
  }

  @ExceptionHandler(ServiceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(ServiceNotFoundException e) {
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(new ErrorResponse("Service not found", e.getMessage()));
  }
}
```

---

## SOAP Services (CXF)

### Endpoint Development

**Location**: `org.openl.rules.ruleservice.ws/src/org/openl/rules/ruleservice/soap/`

**Pattern**:

```java
@WebService(name = "RuleService", targetNamespace = "http://openl.org/ruleservice")
public interface IRuleService {

  @WebMethod
  ServiceInfoResponse getServiceInfo(
      @WebParam(name = "serviceName") String serviceName);

  @WebMethod
  ExecutionResponse execute(
      @WebParam(name = "serviceName") String serviceName,
      @WebParam(name = "methodName") String methodName,
      @WebParam(name = "request") String jsonRequest);
}

@WebService(
    name = "RuleService",
    targetNamespace = "http://openl.org/ruleservice",
    endpointInterface = "org.openl.rules.ruleservice.soap.IRuleService")
public class RuleServiceImpl implements IRuleService {

  @Override
  public ServiceInfoResponse getServiceInfo(String serviceName) {
    // Implementation
  }

  @Override
  public ExecutionResponse execute(String serviceName, String methodName, String jsonRequest) {
    // Implementation
  }
}
```

### WSDL Generation

WSDL is automatically generated at deployment from `@WebService` annotations.

**Access WSDL**:
```
http://localhost:8081/ws/services?wsdl
```

---

## Kafka Integration

### Publishing Events

**Location**: `org.openl.rules.ruleservice.kafka/`

**Pattern**:

```java
@Service
public class RuleExecutionPublisher {

  private final KafkaTemplate<String, RuleExecutionEvent> kafkaTemplate;

  public void publishExecution(
      String serviceName,
      String methodName,
      Object result) {
    RuleExecutionEvent event = new RuleExecutionEvent(
      serviceName,
      methodName,
      result,
      LocalDateTime.now());

    kafkaTemplate.send("rule-executions", event);
  }
}
```

### Configuration

**File**: `application.properties`

```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
openl.kafka.topic.executions=rule-executions
```

---

## Observability & Monitoring

### OpenTelemetry Tracing

**Configuration**:

```properties
# Enable OpenTelemetry
otel.javaagent.enabled=true
otel.sdk.disabled=false
otel.exporter.otlp.endpoint=http://localhost:4317
```

**Instrumenting Code**:

```java
private static final Tracer tracer = GlobalOpenTelemetry.getTracer("openl-ruleservice");

public void executeRule(String serviceName) {
  try (Scope scope = tracer.spanBuilder("execute-rule")
      .setAttribute("service.name", serviceName)
      .startScope()) {
    // Execute rule
  }
}
```

### Prometheus Metrics

**Built-in Metrics**:
- Rule execution count
- Rule execution duration
- Rule execution errors
- Service deployment count

**Configuration**:

```properties
management.endpoints.web.exposure.include=metrics,prometheus
```

**Access Metrics**:
```
http://localhost:8081/actuator/prometheus
```

---

## Critical Areas - Handle with Care

### 1. Rule Execution Path

**Location**: `org.openl.rules.ruleservice.ws/core/`

**Why Critical**:
- Executes production rules
- Performance-critical
- Errors cause service failures

**Rules**:
- Minimal overhead per invocation
- No synchronization hotspots
- Thread-safe rule execution
- Proper error handling/recovery

### 2. Deployment Mechanism

**Location**: `org.openl.rules.ruleservice.deployer/`

**Why Critical**:
- Hot-deploys rule artifacts
- Zero-downtime updates
- Rollback capability

**Rules**:
- Atomic deployment
- Version tracking
- Cleanup of old versions
- Validate before deploy

### 3. Service Registry

**Location**: `org.openl.rules.ruleservice.ws/management/`

**Why Critical**:
- Exposes services to clients
- Method discovery
- Service lifecycle

**Rules**:
- Accurate service metadata
- Consistent method signature discovery
- Proper initialization/cleanup

### 4. Data Logging/Audit

**Location**: `org.openl.rules.store.logdata/`

**Why Critical**:
- Compliance/audit trail
- Debugging production issues
- Performance impact

**Rules**:
- Asynchronous logging
- Don't log sensitive data (PII)
- Efficient storage
- Configurable retention

---

## Testing Strategy

### Unit Tests

**Location**: `org.openl.rules.ruleservice.ws/test/`

**Framework**: JUnit 5, Mockito, Spring Test

```java
@ExtendWith(SpringExtension.class)
@SpringBootTest
class RuleServiceTest {

  @MockBean
  private RuleServiceFactory factory;

  @Autowired
  private RuleServiceController controller;

  @Test
  void testExecuteMethod_validRequest_returns200() {
    // Arrange
    ExecutionRequest request = new ExecutionRequest(...);
    when(factory.executeMethod("service1", "method1", request))
      .thenReturn(new ExecutionResponse(200, "Success"));

    // Act
    ResponseEntity<?> response = controller.executeMethod("service1", "method1", request);

    // Assert
    assertEquals(200, response.getStatusCode());
  }
}
```

### Integration Tests

**Location**: `ITEST/itest.smoke/`

**Scope**:
- End-to-end rule execution
- REST/SOAP endpoint functionality
- Kafka publishing
- OpenTelemetry integration

```bash
cd ITEST/itest.smoke && mvn verify
```

### Load Testing

**Tools**: JMeter, Gatling

**Scenarios**:
- Sustained load (typical usage)
- Peak load (10x typical)
- Spike load (sudden spikes)
- Soak tests (long-running stability)

---

## Common Development Tasks

### Task: Add New REST Endpoint

**Steps**:
1. Create controller method in `RuleServiceController`
2. Add OpenAPI annotations
3. Implement business logic
4. Add input validation
5. Add error handling
6. Write unit tests
7. Write integration tests
8. Update OpenAPI documentation
9. Document in client guide

### Task: Add Kafka Publishing

**Steps**:
1. Define event class (POJO)
2. Create Kafka configuration
3. Create publisher service
4. Inject into rule execution path
5. Implement event publishing
6. Configure topic name
7. Write integration tests
8. Document event schema

### Task: Add Metrics

**Steps**:
1. Import Micrometer
2. Create `MeterRegistry` bean
3. Record metrics in relevant code paths
4. Configure Prometheus scraping
5. Create Grafana dashboard (optional)
6. Document metric name and labels

### Task: Optimize Performance

**Steps**:
1. Profile with production-like load
2. Identify bottleneck (rule execution, IO, etc.)
3. Implement optimization (caching, batching, async, etc.)
4. Verify with load tests
5. Monitor production metrics
6. Document performance improvement

---

## Configuration Management

### Application Properties

**Location**: `org.openl.rules.ruleservice.ws/src/main/resources/`

**Files**:
- `application.properties` - Default
- `application-dev.properties` - Development
- `application-prod.properties` - Production

**Key Properties**:

```properties
server.port=8081
spring.datasource.url=jdbc:postgresql://localhost/openl

# Rule service configuration
openl.ruleservice.deploy.path=/deployments
openl.ruleservice.auto-discovery=true

# Kafka
spring.kafka.bootstrap-servers=localhost:9092

# OpenTelemetry
otel.exporter.otlp.endpoint=http://localhost:4317
```

### Environment Variables

For Docker:

```bash
SERVER_PORT=8081
DATABASE_URL=jdbc:postgresql://db:5432/openl
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
```

---

## Performance Considerations

### Rule Execution Performance

**Optimization**:
- Compiled bytecode (from DEV) provides near-native performance
- Rule caching: compiled rules cached in memory
- Method dispatch: optimized overload resolution

**Monitoring**:
- Execution time percentiles (p50, p95, p99)
- Error rate
- Throughput (requests/second)

### Horizontal Scaling

**Design Principles**:
- Stateless: No session affinity needed
- Shared storage: Deployed rules in shared location
- Load balancing: Simple round-robin works
- Auto-scaling: Based on CPU/memory

**Deployment**:

```bash
# Scale to 3 instances
kubectl scale deployment rule-service --replicas=3
```

---

## Build Guidelines

### Maven Build

```bash
# Full build
cd WSFrontend && mvn clean install

# Quick build
cd WSFrontend && mvn clean install -Dquick -DnoPerf -T1C

# Production build (with optimization)
cd WSFrontend && mvn clean package -DskipTests -Pproduction
```

### Docker Image

```bash
# Build from WSFrontend
docker build -f Dockerfile.services -t openl-services:latest .

# Run
docker run -p 8081:8081 openl-services:latest
```

---

## Code Review Checklist for WSFrontend

Before committing WSFrontend changes:

- [ ] Follows Spring Boot conventions
- [ ] REST/SOAP endpoints documented with OpenAPI
- [ ] Input validation on all endpoints
- [ ] Error handling comprehensive
- [ ] No sensitive data logged
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Load tests (if performance-critical)
- [ ] OpenTelemetry instrumented (if applicable)
- [ ] Metrics added (if monitoring-relevant)
- [ ] Configuration externalized
- [ ] Backward compatible (no breaking API changes)

---

## For More Information

- **Root Project Conventions**: `/AGENTS.md`
- **Architecture**: `/Docs/ARCHITECTURE.md`
- **API Guide**: `/Docs/API_GUIDE.md`
- **Deployment**: `/Docs/DEPLOYMENT.md`
- **Integration Guides**: `/Docs/integration-guides/`
- **Spring Boot**: https://spring.io/projects/spring-boot
- **Apache CXF**: https://cxf.apache.org/
- **OpenTelemetry**: https://opentelemetry.io/

---

**Last Updated**: 2025-12-02
**Version**: 6.0.0-SNAPSHOT

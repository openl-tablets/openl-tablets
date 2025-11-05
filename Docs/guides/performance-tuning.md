# OpenL Tablets Performance Tuning Guide

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT

---

## Table of Contents

- [Overview](#overview)
- [JVM Tuning](#jvm-tuning)
- [Caching Strategy](#caching-strategy)
- [Database Optimization](#database-optimization)
- [Repository Performance](#repository-performance)
- [Web Application Tuning](#web-application-tuning)
- [Rule Service Optimization](#rule-service-optimization)
- [Network and I/O](#network-and-io)
- [Monitoring and Profiling](#monitoring-and-profiling)
- [Performance Testing](#performance-testing)

---

## Overview

This guide provides comprehensive performance tuning recommendations for OpenL Tablets across all deployment scenarios.

### Performance Goals

| Metric | Target | Critical Threshold |
|--------|--------|-------------------|
| **API Response Time** | < 200ms (p95) | < 500ms |
| **Rule Compilation** | < 5s for typical project | < 15s |
| **Project Load Time** | < 2s | < 5s |
| **Memory Usage** | < 2GB (typical) | < 4GB |
| **Database Query Time** | < 50ms (p95) | < 200ms |
| **Concurrent Users** | 100+ | 50+ |

### Performance Principles

1. **Measure First**: Profile before optimizing
2. **Cache Aggressively**: Cache compiled rules and metadata
3. **Lazy Loading**: Load resources on demand
4. **Connection Pooling**: Reuse expensive connections
5. **Async Processing**: Use async for long-running tasks
6. **Minimize I/O**: Reduce disk and network operations

---

## JVM Tuning

### Memory Configuration

#### Development Environment

```bash
JAVA_OPTS="-Xms512m -Xmx2g -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m"
```

#### Production Environment

```bash
# Docker/Kubernetes (recommended)
JAVA_OPTS="-Xms32m -XX:MaxRAMPercentage=90.0"

# Traditional deployment
JAVA_OPTS="-Xms2g -Xmx4g -XX:MetaspaceSize=512m -XX:MaxMetaspaceSize=1g"
```

**Explanation**:
- `-Xms`: Initial heap size
- `-Xmx`: Maximum heap size
- `-XX:MaxRAMPercentage`: Use percentage of container memory (Docker/K8s)
- `-XX:MetaspaceSize`: Initial metaspace (class metadata)
- `-XX:MaxMetaspaceSize`: Maximum metaspace

### Garbage Collection Tuning

#### G1 Garbage Collector (Recommended)

```bash
JAVA_OPTS="$JAVA_OPTS \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:G1HeapRegionSize=16m \
  -XX:InitiatingHeapOccupancyPercent=45 \
  -XX:+ParallelRefProcEnabled"
```

**Benefits**:
- Low pause times (< 200ms)
- Good throughput
- Predictable GC behavior

#### ZGC (Low Latency)

```bash
JAVA_OPTS="$JAVA_OPTS \
  -XX:+UseZGC \
  -XX:ZCollectionInterval=5 \
  -XX:ZAllocationSpikeTolerance=5"
```

**Benefits**:
- Ultra-low pause times (< 10ms)
- Scales to large heaps (TB+)
- Java 21+ recommended

### GC Logging

```bash
JAVA_OPTS="$JAVA_OPTS \
  -Xlog:gc*:file=/var/log/openl/gc.log:time,uptime,level,tags \
  -Xlog:gc*:file=/var/log/openl/gc.log:time,uptime:filecount=5,filesize=100M"
```

### JVM Monitoring Options

```bash
# Enable JMX
JAVA_OPTS="$JAVA_OPTS \
  -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9010 \
  -Dcom.sun.management.jmxremote.local.only=false \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false"

# Enable Flight Recorder
JAVA_OPTS="$JAVA_OPTS \
  -XX:StartFlightRecording=disk=true,dumponexit=true,filename=/tmp/openl.jfr"
```

### Thread Pool Tuning

```yaml
# application.yml
server:
  tomcat:
    threads:
      max: 200          # Maximum threads
      min-spare: 10     # Minimum idle threads
    max-connections: 10000
    accept-count: 100   # Queue size

spring:
  task:
    execution:
      pool:
        core-size: 8
        max-size: 16
        queue-capacity: 100
```

---

## Caching Strategy

### Cache2K Configuration

OpenL Tablets uses **Cache2K** for caching.

**Configuration File**: `STUDIO/org.openl.rules.webstudio/resources/cache2k.xml`

```xml
<cache2k version="1.2">
  <defaults>
    <entryCapacity>6000</entryCapacity>
    <expireAfterWrite>10m</expireAfterWrite>
    <loader>
      <threadCount>2</threadCount>
    </loader>
  </defaults>

  <templates>
    <cache name="default">
      <entryCapacity>6000</entryCapacity>
    </cache>
  </templates>

  <caches>
    <!-- ACL Cache -->
    <cache name="aclCache" template="default">
      <entryCapacity>6000</entryCapacity>
      <expireAfterWrite>2m</expireAfterWrite>
    </cache>

    <!-- User Info OAuth2 Cache -->
    <cache name="userInfoOAuth2Cache" template="default">
      <entryCapacity>100</entryCapacity>
      <expireAfterWrite>30m</expireAfterWrite>
    </cache>
  </caches>
</cache2k>
```

### Tuning Cache Settings

#### Increase Cache Size

```xml
<cache name="aclCache">
  <entryCapacity>10000</entryCapacity>  <!-- Increased from 6000 -->
  <expireAfterWrite>5m</expireAfterWrite>
</cache>
```

#### Add Project Cache

```xml
<cache name="projectCache">
  <entryCapacity>100</entryCapacity>
  <expireAfterWrite>1h</expireAfterWrite>
  <loader>
    <threadCount>4</threadCount>
  </loader>
</cache>
```

### H2-Based Project Version Cache

OpenL Tablets uses H2 database for project version caching:

**Classes**:
- `ProjectVersionCacheManager`: Cache management
- `ProjectVersionH2CacheDB`: H2 storage backend
- `ProjectVersionCacheMonitor`: Cache monitoring

**Configuration**:
```properties
# Enable project version cache
project.version.cache.enabled=true

# Cache database location
project.version.cache.db.path=/path/to/cache/db

# Cache size
project.version.cache.size=1000
```

### Spring Cache Configuration

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new CaffeineCacheManager("projects", "users", "permissions");
    }

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats();
    }
}
```

### Caching Best Practices

1. **Cache Compiled Rules**: Most expensive operation
2. **Cache User Permissions**: Frequent access, infrequent changes
3. **Cache Repository Metadata**: Reduce repository calls
4. **Don't Cache Large Objects**: Keep cache entries small
5. **Monitor Hit Rates**: Aim for > 80% hit rate

---

## Database Optimization

### Connection Pooling (HikariCP)

```yaml
spring:
  datasource:
    hikari:
      # Pool size
      minimum-idle: 10
      maximum-pool-size: 20

      # Connection timeout
      connection-timeout: 30000  # 30 seconds
      idle-timeout: 600000       # 10 minutes
      max-lifetime: 1800000      # 30 minutes

      # Leak detection
      leak-detection-threshold: 60000  # 60 seconds

      # Performance
      auto-commit: false
      connection-test-query: SELECT 1

      # Monitoring
      register-mbeans: true
```

### Hibernate Optimization

```yaml
spring:
  jpa:
    properties:
      hibernate:
        # Batch processing
        jdbc.batch_size: 50
        order_inserts: true
        order_updates: true

        # Query optimization
        default_batch_fetch_size: 16
        max_fetch_depth: 3

        # Statistics (disable in production)
        generate_statistics: false

        # Connection provider
        connection:
          provider_class: org.hibernate.hikaricp.internal.HikariCPConnectionProvider
```

### Query Optimization

#### Use Pagination

```java
// Bad: Load all projects
List<Project> projects = projectRepository.findAll();

// Good: Use pagination
Pageable pageable = PageRequest.of(0, 20);
Page<Project> projects = projectRepository.findAll(pageable);
```

#### Avoid N+1 Queries

```java
// Bad: N+1 query
@Entity
public class Project {
    @ManyToMany(fetch = FetchType.LAZY)
    private List<User> users;  // Lazy loaded, causes N+1
}

// Good: Use JOIN FETCH
@Query("SELECT p FROM Project p LEFT JOIN FETCH p.users WHERE p.id = :id")
Project findByIdWithUsers(@Param("id") Long id);
```

#### Use Indexes

```sql
-- Add indexes on frequently queried columns
CREATE INDEX idx_project_name ON projects(name);
CREATE INDEX idx_user_username ON users(username);
CREATE INDEX idx_acl_object ON acl_entry(acl_object_identity);

-- Composite indexes for multi-column queries
CREATE INDEX idx_project_name_version ON projects(name, version);
```

### Database-Specific Tuning

#### PostgreSQL

```properties
# postgresql.conf

# Memory
shared_buffers = 2GB
effective_cache_size = 6GB
work_mem = 16MB
maintenance_work_mem = 512MB

# Checkpoint
checkpoint_completion_target = 0.9
wal_buffers = 16MB

# Planner
random_page_cost = 1.1  # For SSD
effective_io_concurrency = 200

# Monitoring
log_min_duration_statement = 1000  # Log slow queries
```

#### MySQL

```cnf
# my.cnf

[mysqld]
# InnoDB
innodb_buffer_pool_size = 2G
innodb_log_file_size = 256M
innodb_flush_log_at_trx_commit = 2

# Query cache
query_cache_type = 1
query_cache_size = 256M

# Connection
max_connections = 200
```

### Query Monitoring

```java
// Enable DataSource Proxy in tests
@Bean
public DataSource dataSource() {
    return ProxyDataSourceBuilder
        .create(realDataSource())
        .countQuery()
        .logSlowQueryBySlf4j(1000, TimeUnit.MILLISECONDS)
        .build();
}
```

---

## Repository Performance

### Git Repository Optimization

```yaml
repository:
  design:
    type: git
    uri: file:///path/to/repository

    # Shallow clone for faster operations
    depth: 1

    # Connection pooling
    connection-pool-size: 10

    # Caching
    cache-enabled: true
    cache-size: 100
```

#### Git Best Practices

1. **Use Shallow Clones**: `git clone --depth=1` for CI/CD
2. **Enable Git LFS**: For large Excel files
3. **Prune Regularly**: `git gc --aggressive --prune=now`
4. **Use .gitattributes**: Optimize diff for binary files

```gitattributes
# .gitattributes
*.xlsx binary
*.xls binary
*.jar binary
```

### AWS S3 Repository Optimization

```yaml
repository:
  design:
    type: aws
    bucket: openl-repository
    region: us-east-1

    # Performance
    max-connections: 50
    connection-timeout: 10000

    # Caching
    metadata-cache-ttl: 300  # 5 minutes

    # Multipart upload
    multipart-threshold: 5242880  # 5 MB
```

#### S3 Best Practices

1. **Use S3 Transfer Acceleration**: For cross-region access
2. **Enable CloudFront**: CDN for frequently accessed files
3. **Use Multipart Upload**: For files > 5MB
4. **Set Lifecycle Policies**: Archive old versions

### Azure Blob Storage Optimization

```yaml
repository:
  design:
    type: azure
    container: openl-repository

    # Performance
    max-connections: 50
    connection-timeout: 10000

    # Caching
    cache-enabled: true
```

---

## Web Application Tuning

### Jetty Configuration

```properties
# Jetty settings
jetty.server.Request.maxFormContentSize=-1
jetty.server.Request.maxFormKeys=-1
jetty.httpConfig.requestHeaderSize=32768
jetty.httpConfig.responseHeaderSize=32768
jetty.httpConfig.outputBufferSize=32768
jetty.httpConfig.sendServerVersion=false
jetty.httpConfig.sendDateHeader=false
```

### GZIP Compression

```yaml
server:
  compression:
    enabled: true
    mime-types:
      - text/html
      - text/xml
      - text/plain
      - text/css
      - text/javascript
      - application/javascript
      - application/json
      - application/xml
    min-response-size: 1024  # 1KB
```

### Static Resource Caching

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/")
            .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS)
                .cachePublic());
    }
}
```

### HTTP/2 Configuration

```yaml
server:
  http2:
    enabled: true
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
```

---

## Rule Service Optimization

### Rule Compilation Caching

```properties
# Cache compiled rules
ruleservice.compilation.cache.enabled=true
ruleservice.compilation.cache.size=100
ruleservice.compilation.cache.ttl=3600  # 1 hour
```

### Lazy Loading

```java
// Lazy load rules on first request
@Service
public class RuleService {

    private final Map<String, CompiledRules> cache = new ConcurrentHashMap<>();

    public CompiledRules getRules(String projectName) {
        return cache.computeIfAbsent(projectName, this::compileRules);
    }
}
```

### Parallel Rule Execution

```java
@Configuration
public class RuleServiceConfig {

    @Bean
    public ExecutorService ruleExecutor() {
        return Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2
        );
    }
}
```

### Kafka Integration Performance

```yaml
spring:
  kafka:
    producer:
      # Batching
      batch-size: 16384
      buffer-memory: 33554432
      linger-ms: 10

      # Compression
      compression-type: lz4

      # Acks
      acks: 1  # Leader acknowledgment only

    consumer:
      # Fetch size
      max-poll-records: 500
      fetch-min-size: 1024
      fetch-max-wait-ms: 500

      # Session timeout
      session-timeout-ms: 30000
```

---

## Network and I/O

### Request Timeouts

```yaml
spring:
  mvc:
    async:
      request-timeout: 30000  # 30 seconds

server:
  tomcat:
    connection-timeout: 20000  # 20 seconds
```

### File Upload Optimization

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
      file-size-threshold: 2MB
      location: /tmp/uploads
```

### Network Buffer Sizes

```properties
# TCP settings (Linux)
net.core.rmem_max=16777216
net.core.wmem_max=16777216
net.ipv4.tcp_rmem=4096 87380 16777216
net.ipv4.tcp_wmem=4096 65536 16777216
```

---

## Monitoring and Profiling

### OpenTelemetry Integration

OpenL Tablets includes OpenTelemetry support:

```yaml
# application.yml
management:
  tracing:
    enabled: true
    sampling:
      probability: 0.1  # Sample 10% of requests

otel:
  exporter:
    otlp:
      endpoint: http://localhost:4318
  service:
    name: openl-tablets
  traces:
    sampler: parentbased_traceidratio
    sampler.arg: 0.1
```

### JMX Metrics

```java
@Component
public class PerformanceMetrics {

    private final MeterRegistry registry;

    @Autowired
    public PerformanceMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordRuleExecution(String ruleName, long durationMs) {
        Timer.builder("rule.execution")
            .tag("rule", ruleName)
            .register(registry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
```

### Spring Boot Actuator

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,threaddump,heapdump
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

### Profiling with Async Profiler

```bash
# Download Async Profiler
wget https://github.com/jvm-profiling-tools/async-profiler/releases/latest/download/async-profiler-linux-x64.tar.gz
tar -xzf async-profiler-linux-x64.tar.gz

# Profile application
./profiler.sh -d 60 -f /tmp/profile.html <pid>

# CPU profiling
./profiler.sh -e cpu -d 60 -f /tmp/cpu-profile.html <pid>

# Allocation profiling
./profiler.sh -e alloc -d 60 -f /tmp/alloc-profile.html <pid>
```

### Flight Recorder

```bash
# Start Flight Recorder on JVM startup
java -XX:StartFlightRecording=disk=true,dumponexit=true,filename=/tmp/recording.jfr

# Or start on running JVM
jcmd <pid> JFR.start name=MyRecording settings=profile duration=60s filename=/tmp/recording.jfr

# Analyze with JMC
jmc /tmp/recording.jfr
```

---

## Performance Testing

### Load Testing with JMeter

```xml
<!-- JMeter Test Plan -->
<ThreadGroup>
  <stringProp name="ThreadGroup.num_threads">100</stringProp>
  <stringProp name="ThreadGroup.ramp_time">60</stringProp>
  <stringProp name="ThreadGroup.duration">300</stringProp>
</ThreadGroup>

<HTTPSamplerProxy>
  <stringProp name="HTTPSampler.domain">localhost</stringProp>
  <stringProp name="HTTPSampler.port">8080</stringProp>
  <stringProp name="HTTPSampler.path">/api/projects</stringProp>
  <stringProp name="HTTPSampler.method">GET</stringProp>
</HTTPSamplerProxy>
```

### Load Testing with Gatling

```scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class ProjectLoadTest extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")

  val scn = scenario("Project Load Test")
    .exec(http("List Projects")
      .get("/api/projects")
      .check(status.is(200)))
    .pause(1)

  setUp(
    scn.inject(
      rampUsers(100) during (60 seconds)
    )
  ).protocols(httpProtocol)
}
```

### Performance Benchmarks

```java
@State(Scope.Benchmark)
public class RuleExecutionBenchmark {

    private RuleService ruleService;

    @Setup
    public void setup() {
        ruleService = new RuleService();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void testRuleExecution() {
        ruleService.executeRule("my-rule", inputData);
    }
}
```

---

## Performance Checklist

### Application Deployment

- [ ] Set appropriate JVM memory settings
- [ ] Enable G1GC or ZGC for low latency
- [ ] Configure connection pools (DB, HTTP)
- [ ] Enable caching for compiled rules
- [ ] Enable GZIP compression
- [ ] Configure appropriate timeouts
- [ ] Enable HTTP/2
- [ ] Set up monitoring (JMX, Prometheus)

### Database

- [ ] Create indexes on frequently queried columns
- [ ] Configure connection pooling
- [ ] Enable query logging for slow queries
- [ ] Optimize Hibernate settings
- [ ] Use pagination for large result sets
- [ ] Avoid N+1 queries

### Repository

- [ ] Use appropriate repository type for use case
- [ ] Enable repository caching
- [ ] Configure connection pooling for remote repositories
- [ ] Use shallow clones for Git
- [ ] Enable S3 Transfer Acceleration (if using S3)

### Monitoring

- [ ] Enable OpenTelemetry tracing
- [ ] Configure Spring Boot Actuator
- [ ] Set up Prometheus/Grafana
- [ ] Monitor GC logs
- [ ] Set up alerting for performance degradation

---

## Related Documentation

- [Docker Guide](../operations/docker-guide.md) - Docker performance tuning
- [CI/CD Pipeline](../operations/ci-cd.md) - Build performance
- [Testing Guide](testing-guide.md) - Performance testing
- [Troubleshooting](../onboarding/troubleshooting.md) - Performance issues

---

**Last Updated**: 2025-11-05
**Maintainer**: OpenL Tablets Team

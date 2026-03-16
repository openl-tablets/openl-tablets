# WSFrontend Module — OpenL Rule Services

Exposes compiled OpenL rules as REST web services. Stateless and horizontally scalable.

## Submodules

**Core**:
- **org.openl.rules.ruleservice** — Core service logic: `core/` (compilation, execution), `loader/` (rule loading), `management/` (lifecycle), `publish/` (service publishing), `conf/` (configuration)
- **org.openl.rules.ruleservice.ws** — Web service layer: REST (`jaxrs/`), Kafka (`kafka/`), admin API (`admin/`), store log data (`storelogdata/`), servlets, Spring config
- **org.openl.rules.ruleservice.ws.all** — Aggregate packaging module
- **org.openl.rules.ruleservice.deployer** — Hot-deploy rule artifacts

**Annotations & common**:
- **org.openl.rules.ruleservice.annotation** — Service annotations
- **org.openl.rules.ruleservice.common** — Shared types
- **org.openl.rules.ruleservice.ws.annotation** — WS-specific annotations
- **org.openl.rules.ruleservice.ws.common** — WS shared utilities

**Data logging**:
- **org.openl.rules.ruleservice.ws.storelogdata** — Audit logging abstraction
- **org.openl.rules.ruleservice.ws.storelogdata.db** — DB-backed log storage
- **org.openl.rules.ruleservice.ws.storelogdata.db.annotation** — Annotations for DB logging

**Integration**:
- **org.openl.rules.ruleservice.kafka** — Kafka event publishing

## Request Flow

```
HTTP Request → REST Endpoint (JAX-RS via CXF)
  → Service Method Binding → Compiled Rule Execution (DEV engine)
  → Optional: Kafka publish, audit logging → Response (JSON/XML)
```

## Key Concerns

- **Performance**: Rule execution is the hot path. No synchronization hotspots, minimal overhead per invocation.
- **Thread safety**: Rule execution must be thread-safe.
- **Hot deployment**: Rules deploy atomically with zero downtime. Old versions cleaned up.
- **Data logging**: Asynchronous. Never log PII. Configurable retention.

# Tracing with OpenTelemetry Java agent

OpenL Docker images come with the [OpenTelemetry](https://opentelemetry.io) Java agent. By default, OpenTelemetry is disabled.
To enable OpenTelemetry, it is required to set the `OTEL_JAVAAGENT_ENABLED=true` environment variable or define some `OTEL_EXPORTER_******_ENDPOINT` environment property.
To run OpenL Tablets Rule Services with enabled OpenTelemetry, you can use the following command:

```bash
docker run --rm -p 8080:8080 \
    -e OTEL_EXPORTER_OTLP_ENDPOINT=http://host.docker.internal:4317 \
    opentablets/ws
```

This minimal configuration allows logging both OpenL rules execution and default metrics such as DB queries, HTTP networking, Kafka messaging, etc.
To suppress some undesired agent instrumentation, you can add `OTEL_INSTRUMENTATION_[NAME]_ENABLED=false` to the configuration where `NAME` is the corresponding instrumentation name.
See [Suppressing specific agent instrumentation](https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/#suppressing-specific-agent-instrumentation).
The instrumentation name of the OpenL rules agent is `openl-rules`.

Note: In large projects, when a large number of OpenL methods can be invoked internally (much more than 1,000), it can significantly decrease performance.
So, in this case, it is reasonable to disable tracing of OpenL rules execution via defining `OTEL_INSTRUMENTATION_OPENL_RULES_ENABLED=false`:

```bash
docker run --rm -p 8080:8080 \
    -e OTEL_EXPORTER_OTLP_ENDPOINT=http://172.17.0.1:4317 \
    -e OTEL_INSTRUMENTATION_OPENL_RULES_ENABLED=false \
    opentablets/ws
```

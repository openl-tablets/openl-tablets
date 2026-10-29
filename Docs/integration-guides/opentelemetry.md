# Tracing with OpenTelemetry Java Agent

OpenL Docker images come with the [OpenTelemetry](https://opentelemetry.io) Java agent. By default, OpenTelemetry is disabled.
To enable OpenTelemetry, set the `OTEL_JAVAAGENT_ENABLED=true` environment variable or define any `OTEL_EXPORTER_*` environment variable, such as `OTEL_EXPORTER_OTLP_ENDPOINT`.
To run OpenL Tablets Rule Services with enabled OpenTelemetry, use the following command:

```bash
docker run --rm -p 8080:8080 \
    -e OTEL_EXPORTER_OTLP_ENDPOINT=http://host.docker.internal:4318 \
    openltablets/ws
```

This minimal configuration traces both OpenL rules execution and the default instrumentations, such as HTTP networking and Kafka messaging.
To suppress the undesired agent instrumentation, add `OTEL_INSTRUMENTATION_[NAME]_ENABLED=false` to the configuration where `NAME` is the corresponding instrumentation name.
See [Suppressing specific agent instrumentation](https://opentelemetry.io/docs/zero-code/java/agent/disable/#suppressing-specific-agent-instrumentation).
The instrumentation name of the OpenL rules agent is `openl-rules`.

The images do not trace Hibernate and JDBC by default: they give a span for every DB query, and one request of OpenL Studio makes so many of them that the trace is unreadable and the collector cannot keep up.
To trace DB queries, set `OTEL_INSTRUMENTATION_HIBERNATE_ENABLED=true`, `OTEL_INSTRUMENTATION_JDBC_ENABLED=true` or both.
The images set these two variables, so a file given by `OTEL_JAVAAGENT_CONFIGURATION_FILE` cannot turn them on; use the variables instead.

Note: In large projects, when a significant number (much more than 1,000) of OpenL methods is invoked internally, it can considerably decrease performance.
In this case, it is reasonable to disable tracing of OpenL rules execution by defining `OTEL_INSTRUMENTATION_OPENL_RULES_ENABLED=false` as follows:

```bash
docker run --rm -p 8080:8080 \
    -e OTEL_EXPORTER_OTLP_ENDPOINT=http://172.17.0.1:4318 \
    -e OTEL_INSTRUMENTATION_OPENL_RULES_ENABLED=false \
    openltablets/ws
```

## OpenL Studio: the MCP server and the browser

OpenL Studio sends telemetry from two more places than its Java backend: its built-in MCP server, and the pages a user
opens in the browser. Both follow the same environment variables as the Java agent, so one configuration covers all
three:

```bash
docker run --rm -p 8080:8080 \
    -e OTEL_EXPORTER_OTLP_ENDPOINT=http://host.docker.internal:4318 \
    -e OTEL_SERVICE_NAME=OpenL \
    openltablets/webstudio
```

Each part reports a service of its own, named after `OTEL_SERVICE_NAME` (`OpenL` when it is not set):

| Part          | Service name | What it sends                                                              |
|---------------|--------------|----------------------------------------------------------------------------|
| Java backend  | `OpenL`      | What the Java agent instruments: REST requests, rules.                     |
| MCP server    | `OpenL-mcp`  | A span for each request at `/mcp`, each tool call, each call to Studio.    |
| Browser pages | `OpenL-ui`   | A span for each request a page sends to OpenL Studio.                      |

A request carries its trace on in the `traceparent` header, so one trace shows a page, the MCP server and the backend
together. The MCP server reports the metrics `http.server.request.duration`, `http.client.request.duration` and
`mcp.server.operation.duration` (by tool, in `gen_ai.tool.name`); the pages report `http.client.request.duration`
with delta temporality: each tab sends what it measured since its last export, so the tabs of many users add up in one
series, and an idle tab sends nothing. A backend that stores cumulative metrics only, such as Prometheus, needs the
`deltatocumulative` processor of the OpenTelemetry Collector for them.

- **When it is on.** Telemetry of the MCP server and the pages is on when the Java agent is on: when
  `OTEL_JAVAAGENT_ENABLED=true` or any `OTEL_EXPORTER_*` variable is defined, and neither
  `OTEL_JAVAAGENT_ENABLED=false` nor `OTEL_SDK_DISABLED=true` is set. The MCP server stays off when both
  `OTEL_TRACES_EXPORTER` and `OTEL_METRICS_EXPORTER` are `none`.
- **What is read.** The MCP server reads the standard `OTEL_*` variables of the OpenTelemetry SDK for Node.js, such as
  `OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_EXPORTER_OTLP_HEADERS`, `OTEL_EXPORTER_OTLP_PROTOCOL`, `OTEL_TRACES_EXPORTER`,
  `OTEL_METRICS_EXPORTER` and `OTEL_RESOURCE_ATTRIBUTES`.
- **The browser posts to OpenL Studio, not to the collector.** A page reads its settings from `GET /rest/telemetry` and
  posts its spans and metrics in OTLP/HTTP to `/rest/telemetry/v1/traces` and `/rest/telemetry/v1/metrics`, and OpenL
  Studio passes them on to the collector with the headers of `OTEL_EXPORTER_OTLP_HEADERS`. The collector needs to be
  reachable from OpenL Studio only, and its credentials never reach the browser. Only a signed-in user's page sends
  telemetry. OpenL Studio reads `OTEL_EXPORTER_OTLP_TIMEOUT` in milliseconds or with a unit, such as `10s`. An endpoint
  the HTTP client of Java cannot call, such as one without `http://`, keeps the pages from sending that signal, and a
  header the HTTP client of Java refuses to set, such as `Host`, is left out; OpenL Studio logs both when it starts.
  OpenL Studio accepts OTLP/JSON only, refuses a post larger than 4 MB with `413` (a batch of a page is far smaller),
  passes on only the resources of the signal (`resourceSpans`, `resourceMetrics`), each named after the service of the
  pages, so a signed-in user cannot send data in the name of another service. A span of a page records the address of a
  request without its credentials, the values of its query, the token of an e-mail verification and its fragment.
- **The browser needs OTLP/HTTP.** The pages send a signal only when its exporter is `otlp`, the default, and its
  protocol is `http/protobuf`, the default, or `http/json`. With `OTEL_EXPORTER_OTLP_PROTOCOL=grpc` the pages send
  nothing; the Java backend and the MCP server still do.
- **The `-mcp` and `-ui` suffixes are fixed.** `OTEL_SERVICE_NAME`, or the `service.name` of
  `OTEL_RESOURCE_ATTRIBUTES`, names the backend, and the other two parts follow it.

OpenL Studio deployed as a WAR in a servlet container of your own runs no Java agent, but the MCP server and the pages
still follow the variables of the environment of the container in the same way.

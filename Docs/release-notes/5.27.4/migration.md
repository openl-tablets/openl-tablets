---
title: OpenL Tablets 5.27.4 Migration Notes
---

### Quick Role-Based Pointers

* **If you are an Administrator** → pay special attention to section **1**

---

### 1. OpenTelemetry OTLP Protocol Change

The default OTLP export protocol has changed from `grpc` to `http/protobuf` in line with the OpenTelemetry specification.

If you are using Jaeger or another OTLP-compatible collector, update your connection accordingly:

* Connect via the HTTP endpoint: `http://jaeger:4318`
* Or, to continue using the gRPC endpoint (port 4317), set:

```bash
OTEL_EXPORTER_OTLP_PROTOCOL=grpc
```

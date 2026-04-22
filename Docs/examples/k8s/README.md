# OpenL Studio — Kubernetes Example

Deploys OpenL Studio in multi-user mode on Kubernetes with PostgreSQL as the user management database.

## Files

- **`01-postgres-config.yaml`** — PostgreSQL deployment, PVC, ClusterIP Service, and the shared credentials Secret
- **`02-openl-studio.yaml`** — OpenL Studio deployment, two PVCs (local and shared workspaces), and a LoadBalancer
  Service

## Prerequisites

- A running Kubernetes cluster (tested with Rancher Desktop on macOS)
- `kubectl` configured to talk to that cluster
- Outbound HTTPS from the cluster — the init container downloads the PostgreSQL JDBC driver from `jdbc.postgresql.org`

## Quick start

```bash
kubectl apply -f 01-postgres-config.yaml
kubectl apply -f 02-openl-studio.yaml
```

Wait for both pods to become ready:

```bash
kubectl get pods -w
```

Access Studio at `http://localhost:8080` (Rancher Desktop routes the LoadBalancer Service to localhost automatically).

## Design notes

- **Single replica** — OpenL Studio stores session state in Jetty's in-memory session store, so only one pod is
  supported. The Deployment strategy is set to `Recreate` to avoid PVC conflicts during rollout.
- **Rootless** — the container runs as UID 1000 (`openl` system user baked into the image). Pod and container security
  contexts enforce `runAsNonRoot` and drop all Linux capabilities.
- **JDBC driver** — an init container using `busybox` downloads the PostgreSQL JDBC driver and places it in an
  `emptyDir` volume. The jar is mounted into `/opt/openl/lib/jdbc.jar`, where Jetty picks it up via `--lib`.
- **Persistence** — `/opt/openl/local` (instance metadata) and `/opt/openl/shared` (shared rule projects) are backed by
  separate ReadWriteOnce PVCs so data survives pod restarts.
- **Credentials** — stored in a Kubernetes Secret (`openl-db-credentials`). Both the PostgreSQL pod and the Studio pod
  reference it. For production, replace it with an external secrets manager.
- **Configuration** — OpenL Studio properties are set via environment variables using Spring Boot relaxed binding (
  `USER_MODE` → `user.mode`, `DB_URL` → `db.url`, etc.). `JAVA_OPTS` is not overridden so the image default (
  `-XX:MaxRAMPercentage=90.0`) applies; the JVM heap scales with the pod memory limit.

## Customisation

- **Credentials** — edit `stringData` in `01-postgres-config.yaml` before the first `apply`.
- **Storage** — adjust the `storage` field in each PVC (`1Gi` for Postgres, `2Gi` local, `5Gi` shared).
- **Memory** — raise `resources.limits.memory` in `02-openl-studio.yaml` for large rule sets; the JVM adjusts
  automatically.
- **JDBC version** — update the `wget` URL in the init container of `02-openl-studio.yaml`.
- **Air-gapped clusters** — host the JDBC jar internally and change the `wget` URL to point to your internal registry.
- **Ingress** — change the Service type in `02-openl-studio.yaml` to `ClusterIP` and add an Ingress resource for TLS
  termination or a custom domain.

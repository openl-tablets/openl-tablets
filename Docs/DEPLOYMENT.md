# Deployment

OpenL Studio and OpenL Rule Services are Jakarta EE 10 web applications. They are delivered as WAR files and as
Docker images. The OpenL Tablets DEMO package runs both on Jetty without Docker; see the
[Demo Package Guide](user-guides/getting-started/demo-package/index.md).

## Artifacts

| Product                  | Docker image                         | WAR                    |
|--------------------------|--------------------------------------|------------------------|
| OpenL Studio             | `openltablets/webstudio`             | `openl-studio.war`     |
| OpenL Rule Services      | `openltablets/ws`                    | `ruleservices.war`     |
| OpenL Rule Services, all | `openltablets/ws`, tag suffix `-all` | `ruleservices-all.war` |

- **Releases** — the WARs are assets of the [GitHub release](https://github.com/openl-tablets/openl-tablets/releases),
  each with its OpenPGP signature in the `.asc` asset of the same name. Maven Central gets the libraries only, not the
  WARs. The images are published to [Docker Hub](https://hub.docker.com/u/openltablets) with the release version and
  `latest` as tags.
- **Nightly builds** — the images are pushed to the GitHub Container Registry as `ghcr.io/openl-tablets/webstudio`
  and `ghcr.io/openl-tablets/ws` with the tags `latest` and `<version>-<commit>`. The "all" variant of Rule Services
  has the tags `latest-all` and `<version>-<commit>-all`.
- **The "all" variant** — adds the Git, AWS S3 and Azure Blob repositories and the database log storage to
  Rule Services.

## Docker Deployment

```bash
docker run -p 8080:8080 openltablets/webstudio
docker run -p 8081:8080 openltablets/ws
```

The image runs Jetty on a Temurin JRE, listens on port `8080`, and deploys the application as the `ROOT` web
application. The process runs as the non-root user `openl` (UID `1000`).

### Directories

| Variable            | Path                | Purpose                                                  |
|---------------------|---------------------|----------------------------------------------------------|
| `OPENL_HOME`        | `/opt/openl/local`  | One instance: compiled projects, repository clones, user database. |
| `OPENL_HOME_SHARED` | `/opt/openl/shared` | A cluster shares: user workspaces, locks, saved settings. |
| `OPENL_LIB`         | `/opt/openl/lib`    | Extra JAR files for the classpath, such as JDBC drivers. |
| `OPENL_APP`         | `/opt/openl/app`    | Jetty home. The application is in `webapps/ROOT`.        |

Mount volumes on `OPENL_HOME` and `OPENL_HOME_SHARED` to keep the data between runs.

- **Logs folder** — `/opt/openl/logs` is writable for the `openl` user.
- **`setenv.sh`** — `/opt/openl/setenv.sh` is sourced by the start script. It sets the Jetty request limits through
  `JAVA_OPTS`; edit or replace it to add JVM options.
- **JDBC drivers** — put the driver of the database in use into `/opt/openl/lib`, as the Docker Compose and
  Kubernetes examples below do.

### Environment variables

| Variable                                   | Default                             | Purpose                  |
|--------------------------------------------|-------------------------------------|--------------------------|
| `JAVA_OPTS`                                | `-Xms32m -XX:MaxRAMPercentage=90.0` | JVM options.             |
| `LOGGING_FORMAT`                           | `ecs`                               | Log output format.       |
| `LOGGING_LEVEL_ROOT`                       | `INFO`                              | Root log level.          |
| `OTEL_EXPORTER_OTLP_ENDPOINT`              |                                     | OTLP endpoint.           |
| `OTEL_JAVAAGENT_ENABLED`                   | `false` without `OTEL_EXPORTER_*`   | Agent on or off.         |
| `OTEL_INSTRUMENTATION_OPENL_RULES_ENABLED` | `true`                              | Tracing of OpenL rules.  |
| `OTEL_SERVICE_NAME`                        | `OpenL`                             | Service name.            |
| `SERVICE_VERSION`                          | `0`                                 | `service.version` field. |
| `ENVIRONMENT`                              | `production`                        | `service.environment` field. |

A `JAVA_OPTS` value replaces the default. `OTEL_SERVICE_NAME`, `SERVICE_VERSION` and `ENVIRONMENT` also fill the
fields of the `ecs` log format. Any variable that starts with `OTEL_EXPORTER_` turns OpenTelemetry on, and
`OTEL_INSTRUMENTATION_OPENL_RULES_ENABLED=false` turns off the tracing of OpenL rules. `LOGGING_FORMAT` takes these
values:

- **`ecs`** — JSON lines in the Elastic Common Schema on the standard output.
- **`plain`** — text lines on the standard output.
- **`otel`** — no console appender; the OpenTelemetry agent writes the log events to the standard output in the OTLP
  JSON format.
- **`none`** — no console output.
- **Another value** — a warning is printed and `ecs` is used.

### Application settings

Settings of the application can be given as environment variables. A variable name is the property name with dots
written as underscores, in lower or upper case. A hyphen may stay in the name, and a `$ref` token of a property name is
written as `_ref_`.

| Property                          | Environment variable                |
|-----------------------------------|-------------------------------------|
| `user.mode`                       | `USER_MODE`                         |
| `db.url`                          | `DB_URL`                            |
| `ruleservice.deployer.enabled`    | `RULESERVICE_DEPLOYER_ENABLED`      |
| `production-repository.$ref`      | `PRODUCTION-REPOSITORY__REF_`       |

Settings saved in the **Administration** area of OpenL Studio are kept in the file
`${openl.home.shared}/<application-name>.properties`. The application name comes from the context path, so the file of
the `ROOT` application in the image is `/opt/openl/shared/.properties`. See [Configuration](user-guides/installation-guide/configuration.md) for
the sources of settings and their priority, and
[Rule Services Configuration](user-guides/rule-services/configuration.md) for the settings of Rule Services.

## Docker Compose

[`compose.yaml`](https://github.com/openl-tablets/openl-tablets/blob/main/compose.yaml) in the root of the repository
builds the images and starts a complete environment:

```bash
docker compose up --build
```

- **Services** — OpenL Studio (port `8080`), OpenL Rule Services (port `8081`), PostgreSQL, and an nginx proxy
  that serves `/studio/` and `/services/` on port `80`.
- **Initialization** — the `init` service downloads the PostgreSQL JDBC driver into a volume shared with both
  applications, creates the `studio` and `repository` schemas, and writes the Studio `.properties` file: multi-user
  mode, the administrator `admin`, and a JDBC deployment repository.
- **Rule Services** — read the deployments from the same PostgreSQL repository
  (`PRODUCTION-REPOSITORY__REF_=repo-jdbc`), with the deployer enabled (`RULESERVICE_DEPLOYER_ENABLED=true`).
- **Purpose** — the file is a development environment: it opens the debug ports `5005` and `5006` and uses fixed
  database credentials.

## Kubernetes Deployment

[`examples/k8s`](examples/k8s/README.md) deploys OpenL Studio in multi-user mode with PostgreSQL:

- **One replica** — the `Recreate` strategy replaces the pod without running two pods on the same volumes.
- **Volumes** — separate persistent volume claims for `/opt/openl/local` and `/opt/openl/shared`.
- **JDBC driver** — an init container downloads it into an `emptyDir` volume mounted as `/opt/openl/lib/jdbc.jar`.
- **Probes** — `/healthcheck/startup` for the startup probe and `/healthcheck/readiness` for the readiness probe,
  both without authentication.
- **Settings** — environment variables such as `USER_MODE` and `DB_URL`; credentials come from a Secret.

## Application Server Deployment

A WAR runs in a Jakarta EE 10 servlet container with WebSocket support: Eclipse Jetty 12.1 with the `ee10-deploy` and
`ee10-websocket-jakarta` modules, as in the Docker image, or Apache Tomcat 10.1. See
[Supported Platforms](supported-platforms.md).

- **Context path** — the context path of the application gives the name of its settings file. The `ROOT` application
  uses `.properties`; the application deployed as `/webstudio` uses `webstudio.properties`.
- **Home directories** — `openl.home` and `openl.home.shared` are Java system properties or environment variables.

## Production Setup

[Production Deployment](Production_Deployment.md) describes how to move rules from authoring to production, and
[`examples/production`](examples/index.md) holds a Maven project, a Dockerfile and a Docker Compose setup for it.

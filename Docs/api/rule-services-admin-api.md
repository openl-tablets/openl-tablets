# OpenL Rule Services Admin API

OpenL Rule Services publishes a management API under the `/admin` path of the application. It lists the deployed
services, reports their errors, answers the health probes of an orchestrator, and, when enabled, deploys and deletes
rules projects.

- **Base path** — `{context-path}/admin`, for example `http://localhost:8081/admin/services`.
- **Authorization** — the paths `/admin/healthcheck/`, `/admin/info/` and `/admin/config/` never ask for a
  credential. The other paths follow the security settings of Rule Services
  ([Security](../configuration/security.md)).
- **Availability** — the admin paths answer while the services are being redeployed. The service endpoints wait for
  the redeployment to finish.

## Endpoints

- **`GET /admin/services`** — the deployed services.
- **`GET /admin/ui/info`** — the deployed services and the settings of the OpenL Rule Services page: `appTitle`,
  `version`, `buildDate`, `buildNumber`, `startedAt`, `startedMilli`, `url` and `deployerEnabled`.
- **`GET /admin/services/{servicePath}/errors/`** — the messages of a service; `404` for an unknown service.
- **`GET /admin/services/{servicePath}/MANIFEST.MF`** — the manifest of a service; `404` for an unknown service.
- **`GET /admin/swagger-ui.json`** — the OpenAPI documents of the services that have a REST URL, as a list of
  `name` and `url`.
- **`GET /admin/info/sys.json`** — the JVM metrics.
- **`GET /admin/info/openl.json`** — the OpenL version properties.
- **`GET /admin/info/build.json`** — the build information.
- **`GET /admin/config/application.properties`** — the composed default properties with their descriptions, as plain
  text.
- **`GET /admin/healthcheck/startup`** — `200` with `UP` as soon as the application runs.
- **`GET /admin/healthcheck/readiness`** — `200` with `READY` or `EMPTY`, or `503`; see below.

### Services

`GET /admin/services` answers a list like this:

```json
[
  {
    "name": "multi_project_project1(version=2.0)",
    "servicePath": "multimodule/project1",
    "deploymentName": "multimodule",
    "status": "DEPLOYED",
    "hasManifest": false,
    "urls": {
      "RESTFUL": "2.0/multi_project/project1"
    }
  }
]
```

- **`servicePath`** — the deployment name and the project folder. It is the `{servicePath}` of the endpoints above.
- **`startedTime`** — when the service started.
- **`status`** — `DEPLOYED` or `FAILED`. `/admin/services/{servicePath}/errors/` lists the messages of a failed
  service.
- **`urls`** — the addresses of the service by protocol. A REST service has `RESTFUL`, an address relative to the
  application. The OpenAPI document of the service is at `{address}/openapi.json` and `{address}/openapi.yaml`.

### Readiness

The readiness probe answers:

- **`503`** — while the deployer of the JAR files in the classpath has not finished, while no service is deployed and
  the service manager has not started, or when any service has the status `FAILED`.
- **`200` with `EMPTY`** — nothing is deployed, and the service manager has started.
- **`200` with `READY`** — every deployed service started.

## Deployment

The deployment endpoints exist when `ruleservice.deployer.enabled` is `true`; it is `false` by default. The request
body is a ZIP archive of a deployment ([Deployment Project ZIP Structure](../user-guides/rule-services/appendices/deployment-structure.md)).

- **`POST /admin/deploy`** — deploys the archive sent as `application/zip`. It answers `201`, or `400` with a message
  for a wrong archive.
- **`POST /admin/deploy/{deployPath}`** — the same, under the given deployment path.
- **`GET /admin/deploy/{deploymentName}.zip`** — downloads the deployment as an archive; `404` for an unknown
  deployment.
- **`DELETE /admin/deploy/{deploymentName}`** — deletes the deployment; `200`, or `404` when there is nothing to
  delete.

The same operations are available from Java; see
[Programmatic Deployment](../user-guides/rule-services/appendices/programmatic-deployment.md).

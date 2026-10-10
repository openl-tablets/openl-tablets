# Technology Stack

This page lists the technologies the build declares. The versions are the properties of the root `pom.xml` and the
dependencies of `STUDIO/studio-ui/package.json` and `STUDIO/studio-mcp/package.json`; they are not repeated here.

## Language and Build

- **Java** — the code compiles with `--release 21`; the nightly build runs on Java 21, 25 and 27.
- **Maven** — one multi-module build. `frontend-maven-plugin` installs Node.js and npm and builds `studio-ui` and
  `studio-mcp`.
- **Node.js** — runs the built-in MCP server of OpenL Studio; the Docker image carries the runtime.
- **Lombok** and **JSpecify** — generated accessors and constructors, and nullness annotations.
- **Spotless** — formatting, applied locally and checked in CI. **JaCoCo** — coverage, enabled with `-Dsonar`.

## Rules Engine

- **JavaCC** — the parser of the expression language (`bexgrammar.jj`).
- **ASM** — bytecode generation. **Byte Buddy** — the OpenTelemetry agent extension.
- **Apache POI** — reads and writes Excel workbooks.
- **Groovy** — sources of rules projects.
- **Apache Velocity** — templates of the code generator. **JCodeModel** — Java source generation.

## Server

- **Spring Framework**, **Spring Boot** auto-configuration, **Spring Security**, **Spring Integration** (JDBC), and
  **Spring LDAP**.
- **Jakarta EE** — Servlet, JAX-RS, XML Bind, Mail and Activation APIs.
- **Apache CXF** — the REST services of OpenL Rule Services.
- **Eclipse Jetty** — the servlet container of the Docker image, the DEMO package and the integration tests. **Apache
  Tomcat** is the other supported container ([Supported Platforms](../supported-platforms.md)).
- **Apache HttpComponents** — HTTP client. **Netty**.
- **Jackson** — JSON. **JAXB** — the XML of `rules.xml` and `rules-deploy.xml`.
- **Swagger** (OpenAPI core and parser) and the **victools** JSON schema generator — OpenAPI.
- **Kafka clients** — the Kafka integration of Rule Services.

## Data and Storage

- **Hibernate ORM** and **Hibernate Validator**, **HikariCP**, **Flyway** — the user database and its migrations.
- **H2** — the embedded default database. The integration tests run against PostgreSQL, MySQL, SQL Server and Oracle;
  the bundled MariaDB driver also serves MySQL.
- **JGit** — Git repositories. **AWS SDK** — S3 repositories. **Azure Storage Blob** — Azure Blob repositories.
- **Cache2k** through JCache — caches of access control lists.

## Security

- **OpenSAML** — SAML. **Nimbus JOSE + JWT** and **jose4j** — tokens. **Bouncy Castle**.

## Observability

- **Log4j 2** with **SLF4J**, **Micrometer**, and **OpenTelemetry** — the Java agent and the extension that traces rules
  (`Util/openl-rules-opentelemetry`).

## Frontend

- **React**, **TypeScript**, **Ant Design** with **antd-style**, **React Router**, **Zustand**, **i18next**, **Day.js**.
- **@stomp/stompjs** — the WebSocket client. **CodeMirror** — code editors, and the code in the user guides.
  **dnd-kit** — drag and drop.
- **Cytoscape** with **dagre** and **Mermaid** — graphs and diagrams. **RapiDoc** — the API documentation page.
- **react-markdown**, **unified**, **remark** and **rehype**, and **MiniSearch** — the user guides viewer.
- **Vite** — dev server and bundler. **Vitest**, **Testing Library**, **jsdom** — tests. **ESLint** — linting.

## Tests

- **JUnit**, **JUnit Pioneer**, **Mockito**, **Awaitility**, **XMLUnit**.
- **Testcontainers** — Kafka, Keycloak, PostgreSQL, MySQL, SQL Server and Oracle. **S3Mock**, **GreenMail**.
- **datasource-proxy** — query counts.

# Development Setup

## Prerequisites

- **JDK 21 or later** — the code compiles with `--release 21`; the CI builds run on Java 21, 25 and 27.
- **Maven** — the repository has no Maven wrapper. The Maven build downloads Node.js and npm for the frontend itself
  (`frontend-maven-plugin`; the versions are `node.version` and `npm.version` in the root `pom.xml`).
- **Git**.
- **Docker** — optional. The Docker-based tests, `docker compose` and the Docker image need it. Pass `-DnoDocker` to
  skip the tests that start containers.
- **Node.js 24 or later and npm 11 or later** — only to run the `npm` scripts of `STUDIO/studio-ui` yourself.
- **Lombok in the IDE** — the code uses Lombok, so enable annotation processing.

## Get the Code

```bash
git clone https://github.com/openl-tablets/openl-tablets.git
cd openl-tablets
```

## Build

```bash
mvn clean install -Dquick -DnoPerf -T1C   # fast development build
mvn clean install -DskipTests              # no tests; also drops ITEST and the archetypes from the reactor
mvn clean install                          # everything, with all tests
```

| Flag                   | Effect                                                                            |
|------------------------|-----------------------------------------------------------------------------------|
| `-Dquick`              | Skips heavy tests.                                                                |
| `-DnoPerf`             | Relaxes the memory limits of the tests.                                           |
| `-DnoDocker`           | Skips the tests that start Docker containers.                                     |
| `-DskipTests`          | Skips all tests and drops the integration-test modules from the reactor.          |
| `-Pitest`              | Adds the integration-test modules back to a `-DskipTests` build.                  |
| `-Dnpm.test.skip`      | Skips the Vitest suite of `studio-ui`.                                            |
| `-Dnpm.typecheck.skip` | Skips the `tsc --noEmit` pass of `studio-ui`.                                     |
| `-Dnpm.build.skip`     | Skips the production bundle of `studio-ui`.                                       |
| `-Dsonar`              | Enables JaCoCo, which writes the coverage report of `verify`.                     |

The build writes the web applications of OpenL Studio and Rule Services to
`STUDIO/org.openl.rules.webstudio/target/webapp` and `WSFrontend/org.openl.rules.ruleservice.ws/target/webapp`.
The coverage report is `jacoco-report/target/site/jacoco-aggregate/jacoco.xml`.

## Run

### The whole stack in Docker

```bash
docker compose up --build
```

- **OpenL Studio** — <http://localhost:8080>, or <http://localhost/studio/> behind the nginx proxy.
- **OpenL Rule Services** — <http://localhost:8081>, or <http://localhost/services/>.
- **Sign-in** — the page at <http://localhost/> shows the account `admin` with the password `admin`.
- **Debug ports** — `5005` for Studio and `5006` for Rule Services.

The file is [`compose.yaml`](https://github.com/openl-tablets/openl-tablets/blob/main/compose.yaml); see
[Deployment](../DEPLOYMENT.md#docker-compose) for what it starts.

### The frontend with hot reload

With a backend on port `8080`, for example the stack above:

```bash
cd STUDIO/studio-ui
npm install
npm run start
```

The Vite dev server listens on <http://localhost:3100> and proxies `/rest`, `/ws`, `/login`, `/logout` and `/docs` to
`http://localhost:8080`.

### The DEMO package

`DEMO/` builds the package that starts OpenL Studio and Rule Services on Jetty; see the
[Demo Package Guide](../user-guides/getting-started/demo-package/index.md).

## Test

```bash
mvn test -pl <module-path>                       # one module
mvn test -pl <module-path> -Dtest=ClassName#method   # one test
mvn verify -pl ITEST/itest.smoke -am             # one integration suite
cd STUDIO/studio-ui && npx vitest run src/<file>.test.tsx   # one frontend test
mvn test -pl STUDIO/studio-docs                  # the user guides: links, images, Markdown
```

New or changed Java code keeps at least 80% line coverage on the diff. The tests of integration suites are described in
[`ITEST/AGENTS.md`](https://github.com/openl-tablets/openl-tablets/blob/main/ITEST/AGENTS.md).

## Format

`.editorconfig` sets LF line endings, a 4-space indent for Java and XML, and 120 characters per line. Run

```bash
mvn validate -N
```

before every commit. Spotless formats the files locally, and CI fails on a file that is not formatted. The same
command checks that the versions copied into the `Dockerfile` and the `DEMO/start*` scripts match the Maven
properties.

## Conventions

- [`AGENTS.md`](https://github.com/openl-tablets/openl-tablets/blob/main/AGENTS.md) in the root of the repository and
  the `AGENTS.md` file of each module hold the coding rules and the commit convention.
- [Codebase Tour](codebase-tour.md) shows where the code lives.

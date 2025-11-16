![Build](https://github.com/openl-tablets/openl-tablets/workflows/Build/badge.svg)
![Maven Central](https://img.shields.io/maven-central/v/org.openl/org.openl.core)
![Commit activity](https://img.shields.io/github/commit-activity/m/openl-tablets/openl-tablets)
[![WebSite](https://img.shields.io/website?label=WebSite&url=https%3A%2F%2Fopenl-tablets.org)](https://openl-tablets.org)

# Easy Business Rules

**OpenL Tablets** targets the infamous gap between business requirements (rules and policies) and software implementation.

Designed to be straightforward and intuitive for ***business people***, OpenL Tablets made its rules representation impressively close to documents usually created by business(it intends business requirements etc).

Users can focus on logic as all data, syntax and typing errors are checked while they write. Convenient tools help to ensure rules integrity while further using.

One-click deployment of rules as efficient, scalable and standardized services for ***SOA-based*** integration makes business logic simple to embed in application.

For Java developers, OpenL Tablets provides many rich usage scenarios in which all rules and business data are exposed through reflection-like API or wrapped as Java class.

All of OpenL Tablets is open sourced under **LGPL** license.

[Visit our website](//openl-tablets.org)

## MCP Server for AI Integration

The **Model Context Protocol (MCP) Server** provides AI coding agents with seamless access to OpenL Tablets through a type-safe API.

**Features:**
- 19 MCP tools for managing rules, projects, and deployments
- Support for multiple authentication methods (Basic Auth, API Key, OAuth 2.1)
- 11 expert guidance prompts for complex workflows
- Full TypeScript implementation with Zod validation

**Quick Start:**
```bash
cd mcp-server
npm install
npm run build
npm start
```

**Documentation:** See [mcp-server/README.md](mcp-server/README.md) for complete setup instructions, tool reference, and usage examples.

## How to build

#### Requirements:

* JDK 21+
* Maven 3.9.9
* Docker 27.5.0
* Docker compose 2.32.4
* 1 GiB RAM free
* 2 GiB Disk space free

#### Build Maven artifacts:

`mvn` - full build with tests

Estimated build time: ~10...30 minutes (with all tests)

`mvn -Dquick -DnoPerf -T1C` - rapid building with less amount of the tests

It is possible to use the following settings:

`-DnoPerf` - to run tests without extreme memory limitation

`-DnoDocker` - to skip dockerized tests

`-Dquick` - to skip heavy or not important tests

`-DskipTests` - to skip all tests


Artifacts:
* **OpenL Studio** - STUDIO/org.openl.rules.webstudio/target/webapp.war
* **RuleService WS** - WSFrontend/org.openl.rules.ruleservice.ws/target/webapp.war
* **DEMO App** - DEMO/target/openl-tablets-demo.zip

#### Run OpenL Docker images with the typical configuration:

`docker compose up` and open http://localhost in the browser.

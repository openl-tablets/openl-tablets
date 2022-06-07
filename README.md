![Vulneribilities](https://img.shields.io/snyk/vulnerabilities/github/openl-tablets/openl-tablets)
![Build](https://github.com/openl-tablets/openl-tablets/workflows/Build/badge.svg)
![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.openl/org.openl.core/badge.svg)
![Commit activity](https://img.shields.io/github/commit-activity/m/openl-tablets/openl-tablets)

[![DEMO](https://img.shields.io/website?label=DEMO&url=https%3A%2F%2Fdemo.openl-tablets.org)](https://demo.openl-tablets.org)
[![WebSite](https://img.shields.io/website?label=WebSite&url=https%3A%2F%2Fopenl-tablets.org)](https://openl-tablets.org)

# Easy Business Rules

**OpenL Tablets** targets the infamous gap between business requirements (rules and policies) and software implementation.

Designed to be straightforward and intuitive for ***business people***, OpenL Tablets made its rules representation impressively close to documents usually created by business(it intends business requirements etc).

Users can focus on logic as all data, syntax and typing errors are checked while they write. Convenient tools help to ensure rules integrity while further using.

One-click deployment of rules as efficient, scalable and standardized services for ***SOA-based*** integration makes business logic simple to embed in application.

For Java developers, OpenL Tablets provides many rich usage scenarios in which all rules and business data are exposed through reflection-like API or wrapped as Java class.

All of OpenL Tablets is open sourced under **LGPL** license.

[Visit our website](//openl-tablets.org)

## How to build

#### Requirements:

* JDK 11
* Maven 3.8.3
* 512 MiB RAM free
* 2 GiB Disk space free

#### Build Maven artifacts:

`mvn`

Estimated build time: ~25 minutes

Artifacts:
* **WebStudio** - STUDIO\org.openl.rules.webstudio\target\webapp.war
* **RuleService WS** - WSFrontend\org.openl.rules.ruleservice.ws\target\webapp.war
* **DEMO App** - DEMO\org.openl.rules.demo\target\openl-tablets-demo.zip

#### Build Docker images:

`docker-compose -f Dockerfiles\docker-compose.yaml build`

![Build](https://github.com/openl-tablets/openl-tablets/workflows/Build/badge.svg)
![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.openl/org.openl.core/badge.svg)
![Commit activity](https://img.shields.io/github/commit-activity/m/openl-tablets/openl-tablets)

[![DEMO](https://img.shields.io/website?label=DEMO&url=https%3A%2F%2Fdemo.openl-tablets.org%2Fwebstudio%2F)](https://demo.openl-tablets.org)
[![Nightly Build DEMO](https://img.shields.io/website?label=Nightly%20Build%20DEMO&url=https%3A%2F%2Fdemo.openl-tablets.org%2Fnightly%2Fwebstudio%2F)](https://demo.openl-tablets.org/nightly/)
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
* Maven 3.9.4
* Docker 24.0.4
* Docker compose 1.29.2
* 512 MiB RAM free
* 2 GiB Disk space free

#### Build Maven artifacts:

`mvn` - full build with tests

Estimated build time: ~30 minutes (with all tests)

`mvn -Dquick -DnoPerf -T1C` - rapid building with less amount of the tests

It is possible to use the following settings:

`-DnoPerf` - to run tests without extreme memory limitation

`-DnoDocker` - to skip dockerized tests

`-Dquick` - to skip heavy or not important tests

`-DskipTests` - to skip all tests


Artifacts:
* **WebStudio** - STUDIO/org.openl.rules.webstudio/target/webapp.war
* **RuleService WS** - WSFrontend/org.openl.rules.ruleservice.ws/target/webapp.war
* **DEMO App** - DEMO/target/openl-tablets-demo.zip

#### Build Docker images:

`docker-compose build`

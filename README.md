![Build](https://github.com/openl-tablets/openl-tablets/workflows/Build/badge.svg)

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

* JDK 8
* Maven 3.6.3
* 512 MiB RAM free

#### Build command:

`mvn clean verify`

Estimated build time: ~15 minutes

#### Build artifacts:
* **WebStudio** - STUDIO\org.openl.rules.webstudio\target\*.war
* **RuleService WS** - WSFrontend\org.openl.rules.ruleservice.ws\target\*.war
* **DEMO App** - DEMO\org.openl.rules.demo\target\*.zip

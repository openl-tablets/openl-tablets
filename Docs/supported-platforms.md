---
title: Supported Platforms
description: Supported platforms, application servers, databases, browsers, Java versions, and requirements for OpenL Tablets.
---

## Supported Environments

#### Application/Web Server
* Eclipse Jetty 12.0
* Apache Tomcat 10.1

#### Operating System
* Ubuntu 24.04 LTS
* macOS 15
* Windows 11
* Windows Server 2022

#### Container OS/ARCH
* linux/amd64
* linux/arm64

#### Web Browser
* Firefox 140 ESR
* Chrome 145
* Edge 145

#### Database
* PostgreSQL 16.1
* MariaDB 10.6
* SQL Server 2019
* Azure SQL Database
* MySQL 8.0
* Oracle 19c

## Requirements

### OpenL Rule Services

| Resource       | Minimum | Recommended |
|----------------|---------|-------------|
| **Memory**     | 512 MB  | 1 GB        |
| **CPU**        | 1       | 2           |
| **Disk Space** | 100 MB  | 500 MB      |
| **Network**    | 1 Mbps  | 10 Mbps     |

### OpenL Studio

The following are the minimum and recommended system requirements for running OpenL Studio, the integrated development
environment (IDE) for OpenL Tablets. The recommended requirements are based on typical usage scenarios and may vary
depending on the size and complexity of the projects being developed, on the count of concurrent users.

| Resource       | Minimum | Recommended |
|----------------|---------|-------------|
| **Memory**     | 2 GB    | 16 GB       |
| **CPU**        | 2 cores | 4 cores     |
| **Disk Space** | 100 MB  | 1 GB        |
| **Network**    | 1 Mbps  | 10 Mbps     |

## Specifications, APIs, and Frameworks

* Jakarta EE 10.0
* Spring Framework 6.2
* SAML 2.0
* OAuth 2.0
* OpenID Connect 1.0
* OpenAPI 3.0
* Java 21
* Groovy 4.0
* JavaScript ES2023

## Java Version Compatibility Matrix

| OpenL Version | Java 6 | Java 7 | Java 8 | Java 11 | Java 17 | Java 21 | Java 25 | Java 29 |
|---------------|--------|--------|--------|---------|---------|---------|---------|---------|
| 5.10.0        | Y      | Y      |        |         |         |         |         |         |
| 5.16.0        | Y      | Y      | Y      |         |         |         |         |         |
| 5.20.0        |        | Y      | Y      |         |         |         |         |         |
| 5.22.0        |        |        | Y      | Y       |         |         |         |         |
| 5.26.0        |        |        |        | Y       |         |         |         |         |
| 5.27.8        |        |        |        | Y       | Y       | Y       |         |         |
| 5.27.12       |        |        |        | Y       | Y       | Y       | Y       |         |
| 6.0.0         |        |        |        |         |         | Y       | Y       |         |
| 7.0.0         |        |        |        |         |         |         | Y       |         |
| 8.0.0         |        |        |        |         |         |         | Y       | Y       |

## Support References

- [Oracle Java SE Support Roadmap](http://www.oracle.com/java/technologies/java-se-support-roadmap.html)
- [Java Releases](https://www.java.com/releases/)
- [Azul Support Roadmap](https://www.azul.com/products/azul-support-roadmap/)
- [Temurin™ Support](https://adoptium.net/support/)
- [Java Is Still Free 3.0.0](https://medium.com/@javachampions/java-is-still-free-3-0-0-ocrt-2021-bca75c88d23b)
- [Jakarta Releases](https://jakarta.ee/release/)
- [Apache Tomcat Versions](https://tomcat.apache.org/whichversion.html)
- [Eclipse Jetty Versions](https://jetty.org/download.html)
- [Spring Framework Support](https://spring.io/projects/spring-framework#support)

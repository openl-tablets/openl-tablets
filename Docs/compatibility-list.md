# OpenL Compatibility List

## Environments

| Component              | Version                                                                               |
|------------------------|---------------------------------------------------------------------------------------|
| Application/Web Server | Eclipse Jetty 12.0<br/> Apache Tomcat 10.1                                            |
| Operating System       | Ubuntu 24.04 LTS<br/> macOS 15<br/> Windows 11<br/> Windows Server 2022               |
| Container OS/ARCH      | linux/amd64<br/> linux/arm64                                                          |
| Browser                | Firefox 140 ESR<br/> Chrome 145<br/> Edge 145                                         |
| Database               | PostgreSQL 16.1<br/> MariaDB 10.6<br/> SQL Server 2019<br/> MySQL 8.0<br/> Oracle 19c |

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

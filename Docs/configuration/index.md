# Configuration

OpenL Studio and OpenL Rule Services read their settings from properties files, environment variables, and Java system
properties. This section holds the security setup of Rule Services. The other settings are described next to the
product that owns them.

## Pages

- [Security](security.md) — enable the OAuth2 authentication of OpenL Rule Services, or replace it with an own
  `AuthorizationChecker`.

## Where Settings Are Described

- [Externalized Configuration](../developer-guides/externalized-config.md) — the sources of the settings, their priority,
  the names of the files, and the property reference of a running instance.
- [OpenL Studio Configuration](../user-guides/installation-guide/configuration.md) — the properties of OpenL Studio:
  user modes, repositories, database, and mail.
- [Rule Services Configuration](../user-guides/rule-services/configuration.md) — the properties of OpenL Rule Services:
  data sources, publishers, and the deployment descriptor.
- [Deployment](../DEPLOYMENT.md) — the Docker image directories, the environment variables, and the logging.

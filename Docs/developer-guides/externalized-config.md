## Externalized Configuration

OpenL Studio and OpenL Rule Services read their settings from several sources, so the same build works in different
environments. The code is in `DEV/org.openl.spring` (`org.openl.spring.env`).

The following topics are included in this section:

-   [Sources and Priority](#sources-and-priority)
-   [Default Properties Files](#default-properties-files)
-   [Application Property Files](#application-property-files)
-   [Profile-Specific Properties](#profile-specific-properties)
-   [Command Line and Environment](#command-line-and-environment)
-   [Property Reference of a Running Instance](#property-reference-of-a-running-instance)

### Sources and Priority

Each source overrides the previous one in this list:

1.  OpenL default properties: the `openl-default.properties` files.
2.  Application property files, such as `application.properties`.
3.  The settings saved in the **Administration** area of OpenL Studio, kept in
    `${openl.home.shared}/<application-name>.properties`.
4.  Operating system environment variables.
5.  Java system properties.
6.  JNDI attributes under `java:comp/env`.
7.  Servlet context initialization parameters.

Only property keys that match `openl.config.key-pattern.allowed` and do not match `openl.config.key-pattern.denied`
are visible to the application.

### Default Properties Files

OpenL scans `openl-default.properties` in all resources of the classpath and composes one default configuration.
Because the files are not merged in a defined order, follow these rules:

1.  Create the `openl-default.properties` file in the module where the property is used.
2.  Keep the property names unique across all `openl-default.properties` files.

To override defaults in your own build, add a file `application-default.properties` or
`application-{custom}-default.properties` to the classpath, for example to `WEB-INF/classes` of the `.war`. The
`application*-default.properties` files are loaded alphabetically, and the last one wins.

### Application Property Files

The locations are listed from the lowest priority to the highest; a later location overrides the earlier ones:

-   `classpath:`
-   `classpath:config/`
-   the current directory
-   `conf/` of the current directory
-   `config/` of the current directory
-   the user's home directory

In every location the engine looks for these file names, where `{appName}` is the application name, taken from the
context path of the application, and `{profile}` is an active profile:

-   `application.properties`
-   `application-{profile}.properties`
-   `{appName}.properties`
-   `{appName}-{profile}.properties`

The default locations and names are the values of the properties `openl.config.location` and `openl.config.name`.
Override them with `openl.config.location` or `spring.config.location`, and `openl.config.name` or
`spring.config.name`, as a system property or an environment variable. Setting them in an application property file
has no effect.

### Profile-Specific Properties

The profiles are the active Spring profiles, for example `-Dspring.profiles.active=dev,app01`. A profile-specific file
overrides the plain files, and the file of a later profile overrides the file of an earlier one.

### Command Line and Environment

Java system properties and environment variables define any setting without a file.

-   **System property** — `-Dopenl.home=/srv/openl`.
-   **Environment variable** — the property name as it is, or in upper case with underscores: `OPENL_HOME`. A `$ref`
    token of a name is written as `_ref_`, so `repository.production.$ref` is `REPOSITORY_PRODUCTION__REF_`.

### Property Reference of a Running Instance

The instance publishes the composition of all default properties, with their descriptions:

-   **OpenL Studio** — `/application.properties` under the context path of the application.
-   **OpenL Rule Services** — `/admin/config/application.properties`.

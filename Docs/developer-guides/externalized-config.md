## Externalized Configuration

OpenL Studio and OpenL Rule Services read their settings from several sources, so the same build works in different
environments. The code is in `DEV/org.openl.spring` (`org.openl.spring.env`).

The following topics are included in this section:

-   [Sources and Priority](#sources-and-priority)
-   [References and Read-Only Settings](#references-and-read-only-settings)
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

### References and Read-Only Settings

A property with no value of its own takes one through a `$ref` key of a parent path: with `repository.design.$ref =
repo-git`, the property `repository.design.uri` takes the value of `repo-git.uri`. The nearest parent path with a
`$ref` key wins, and the property a reference points at may itself be resolved through a reference, up to two
references deep. `RefPropertySource` resolves the references, after every other source.

OpenL Studio shows a setting as read-only when its value comes from sources 4–7 — the environment, system properties,
JNDI or the servlet context. `DisablePropertySource` answers it as the property `_sys_disable_<name>`, following the
same path `RefPropertySource` resolves:

-   A value set directly in sources 4–7 is read-only.
-   A value resolved through a reference is read-only when one of the `$ref` keys on its path comes from sources 4–7.
-   The value a reference points at does not count. Set in the environment, it is a default every property pointing at
    it shares, and a property may still override it.
-   A value set directly in sources 1–3 is resolved through no reference, so it is never read-only through one.

A request that changes a read-only setting is refused with `400 Bad Request`, naming the setting. The request is
compared with the settings as they are answered (`ReadOnlySettings`), so a value sent back as it was answered passes,
even when the settings model reformats it. A setting the request leaves out or gives no value is unchanged: reset, a
setting the environment sets keeps its value. The value of a secret setting is compared too, though it is never
answered. The value of a read-only setting is never written to the settings file
(`SettingsServiceImpl.commit`): written there, it would take the place of a value reached through a reference. While
the list of the repositories, `design-repository-configs` or `production-repository-configs`, is read-only, a
repository of that list can be neither added nor removed, one at a time or in a batch.
`GET /admin/settings/repos/{design|production}/configs` answers whether the list is read-only, so the UI offers neither
the button that adds a repository nor the one that removes it.

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

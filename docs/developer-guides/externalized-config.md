## Externalized Configuration
OpenL Tablets supports externalizing application configuration to work with the same application in different environments. For configuration externalization, properties files and command-line arguments are used.

The following topics are included in this section:

-   [Accessing Command Line Properties](#accessing-command-line-properties)
-   [Using Property Files](#using-property-files)

### Accessing Command Line Properties
By default, OpenL Tablets converts any command line option arguments into a `property` which starts with `-D`, such as `-Druleservice.datasource.filesystem.supportDeployments=true,` and adds it to the OpenL Tablets environment.

**Example**:
`$: /startup.cmd -Druleservice.datasource.filesystem.supportDeployments=true`

### Using Property Files
This section describes how to externalize application configuration using application property files and profile-specific properties and includes the following topics:

-   [Default Properties Files](#default-properties-files)
-   [Application Property Files](#application-property-files)
-   [Profile-Specific Properties](#profile-specific-properties)

#### Default Properties Files
OpenL Tablets scans `openl-default.properties` in all resources in the classpath and composes single default configuration. The order of scanning is undefined, so when equal properties are retrieved, the random value wins. To ensure a predictable result, follow these rules:

1.  Create the` openl-default.properties` file in the module where the property is used.
2.  Ensure that property names are unique across all `openl-default.properties` files.

When OpenL WebStudio and OpenL Rule Services builds are created, the `application.properties` file is generated with all default settings.

To override default values for your own build of OpenL WebStudio or OpenL Rule Services, add the `application-default.properties` file to the `WEB-INF/classes `folder of the .`war` application.

It is possible to define several `application-{custom}-default.properties `files which will be loaded alphabetically with last-win strategy to override default properties.

#### Application Property Files
OpenL Tablets loads properties from the `application.property` files to the following locations and adds them to the OpenL Tables environment:

-   `%USER_HOME%` directory
-   `/config` subdirectory of the current directory
-   `/conf` subdirectory of the current directory
-   current directory
-   `classpath:config/`
-   `classpath:`

	**Note:** The list is ordered by precedence, that is, properties defined in locations higher in the list override those defined in lower locations.

OpenL Tablets supports the `{appName}.properties` alias of the `application.property` file, where `{appName}` is the application name of the running application context.

#### Profile-Specific Properties
Profile-specific properties can be also defined by using the following naming convention:

-   `application-{profile}.properties`
-   `{appName}-{profile}.properties`

Profile-specific properties are loaded from the same locations as the standard `application.properties` file and always override default properties.

If several profiles are specified in the `spring.profile.active` property, the existing property file of the last profile overrides the previous ones. For example, the property `spring.profile.active` contains the `dev-openl,app01-openl` values, so the OpenL Tablets Engine will be looking for the next property files:

-   `{appName}-app01-openl.properties`
-   `{appName}-dev-openl.properties`
-   `{appName}.properties`
-   `application-app01-openl.properties`
-   `application-dev-openl.properties`
-   `application.properties`

	**Note:** The list ordered is ordered by precedence, that is, properties defined in locations higher in the list override those defined in lower locations)


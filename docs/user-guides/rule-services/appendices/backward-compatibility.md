## Appendix F: Backward Compatibility Settings

This appendix describes backward compatibility settings and includes the following topics:

-   [Version in Deployment Name](#version-in-deployment-name)
-   [Custom Spreadsheet Type](#custom-spreadsheet-type)

### Version in Deployment Name

If the Deployment repository is created in an OpenL Tablets version older than 5.20, the **Version in deployment name** option must be enabled for backward compatibility.

The 5.20 version of the OpenL Tablets Deployment repository contains only actual deployments which are exposed as services. Each new deployment updates the current deployment, while older versions are hidden in history and cannot be loaded into the RuleService directly. Different API versions of services are located in different deployments. They are distinguished by a suffix generated in OpenL Studio according to the API version in `rules-deploy.xml`. As a result, services are exposed more quickly. However, if a user created a repository in the OpenL Tablets version older than 5.20 and migrated to a newer OpenL Rule Services, enable the **Version in deployment name** option to expose services correctly.

In this case, add the following property to the `application.properties` file:

```properties
version-in-deployment-name = true
```

If you create a new repository, omit this property or set it to false.

### Custom Spreadsheet Type

In OpenL Tablets, **custom spreadsheet type** is used by default. To enable support of the previously created rules based on other types, in the `application.properties` configuration file, set this property to `false`.


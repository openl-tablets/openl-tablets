## Appendix H: Manifest File for Deployed Projects

When a user deploys the OpenL Tablets project from OpenL Studio or using the OpenL Tablets Maven plugin, the MANIFEST.MF file is generated. This file contains information about deployment author, deployment time, project version, and OpenL Tablets version used for deployment.

If OpenL Tablets Maven plugin is used for deployment, the manifest file contains the following information:

| Attribute              | Description                                                                    |
|------------------------|--------------------------------------------------------------------------------|
| Build-Date             | Current zone datetime in the ISO8601 format.                                   |
| Built-By               | Name of the user currently logged in.                                          |
| Created-By             | OpenL Maven Plugin \<OpenL version\>                                           |
| Implementation-Title   | Deployment project name. Default format is project.groupId:project.artifactId. |
| Implementation-Version | Project version from the Maven pom.xml file.                                   |
| Implementation-Vendor  | Deployment project vendor. By default, it is project organization.             |

If the project is deployed in OpenL Studio, the manifest file contains the following information:

| Attribute            | Description                                                   |
|----------------------|---------------------------------------------------------------|
| Build-Date           | Current zone datetime.                                        |
| Build-Number         | Git revision ID or database revision value.                   |
| Built-By             | Name of the user currently logged in OpenL Studio. |
| Implementation-Title | Deployment project name.                                      |
| Branch-Name          | Git branch if the project is connected to Git.                |
| Created-By           | OpenL Studio version.                              |

The manifest file is available in OpenL Rule Services, on the main page, for each deployed service.

![](../../../assets/images/rule-services/989c0347237015276cece6779d16e9a8.png)

*Manifest file available for the deployed project*

If the project was deployed in a different way and it does not contain the manifest file, no link to it appears after the project name.

An example of the file contents is as follows:

```json
{
  "entries": {},
  "mainAttributes": {
    "Manifest-Version": "1.0",
    "Build-Date": "2022-05-26T00:47:06.894013+02:00",
    "Built-By": "openl",
    "Implementation-Title": "Sample Project",
    "Implementation-Version": "1.0-SNAPSHOT",
    "Created-By": "OpenL Studio v5.26.0",
    "Build-Branch": "master",
    "Build-Number": "0123abcd968574142536fedc01cc",
  }
}
```
```
Release 5.27
OpenL Tablets Documentation is licensed under a Creative Commons Attribution 3.0 United States License.
```

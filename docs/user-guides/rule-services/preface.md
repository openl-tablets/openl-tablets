## Preface

OpenL Tablets is a Business Rules Management System (BRMS) based on the tables presented in Excel documents. Using unique concepts, OpenL Tablets facilitates treating business documents containing business logic specifications as executable source code.

OpenL Tablets provides a set of tools addressing BRMS related capabilities including *OpenL Rule Services* *application* designed for integration of business rules into different customers’ applications.

The goal of this document is to explain how to configure Rule Services Core, that is, configure OpenL Rule Services or integrate the Rule Services Core module into the existing application, for different working environments and how to customize the services to meet particular customer requirements.

The following topics are included in this chapter:

-   [Audience](#audience)
-   [How This Guide Is Organized](#how-this-guide-is-organized)
-   [Related Information](#related-information)
-   [Typographic Conventions](#typographic-conventions)

### Audience

This guide is targeted at rule developers who integrate the Rule Services Core module and set up, configure, and customize OpenL Rule Services to facilitate the needs of customer rules management applications.

Basic knowledge of Java, Apache Tomcat, Ant, Maven, and Excel is required to use this guide effectively.

### How This Guide Is Organized

| Section                                                                                                                         | Description                                                                                                                         |
|---------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| [Introduction](#introduction)                                                                                                  | Provides overall information about OpenL Rule Services.                                                                      |
| [Rule Services Core](#rule-services-core)                                                                                       | Introduces Rule Services Core functionality.                                                                                        |
| [OpenL Rule Services Configuration](#openl-rule-services-configuration)                                                              | Describes the default configuration of OpenL Rule Services, introduces Service <br/>Manager, and explains main configuration points. |
| [OpenL Rule Services Advanced Configuration and Customization](#openl-rule-services-advanced-configuration-and-customization)                                 | Describes OpenL Rule Services advanced services configuration and customization.                                             |
| [Appendix A: Tips and Tricks](#appendix-a-using-openl-tablets-rest-services-from-java-code)                                     | Describes how to use OpenL Rule Services from Java code.                                                                     |
| [Appendix B: Projects on the OpenL Rule Services Launch](#appendix-b-projects-on-the-openl-rule-services-launch)                                       | Explains how projects appear upon OpenL Rule Services launch.                                                                |
| [Appendix C: Types of Exceptions in OpenL Rule Services](#appendix-c-types-of-exceptions-in-openl-rule-services) | Explains typical exceptions in OpenL Rule Services.                                                                          |
| [Appendix D: OpenAPI Support](#appendix-d-openapi-support)                                                                            | Explains Swagger support in OpenL Tablets.                                                                                          |
| [Appendix E: Programmatically Deploying Rules to a Repository](#appendix-e-programmatically-deploying-rules-to-a-repository)    | Describes how to locate a project with rules in the database repository <br/>without OpenL Studio deploy functionality.      |
| [Appendix F: Backward Compatibility Settings](#appendix-f-backward-compatibility-settings)                                      | Describes backward compatibility settings.                                                                                          |
| [Appendix G: Deployment Project ZIP Structure](#appendix-g-deployment-project-zip-structure)                                    | Describes ZIP structure for single and multiple project deployment.                                                                 |
| [Appendix H: Manifest File for Deployed Projects](#appendix-h-manifest-file-for-deployed-projects)                              | Introduces manifest files created during project deployment from OpenL Studio <br/>or using the OpenL Tablets Maven plugin.  |

### Related Information

The following table lists sources of information related to contents of this guide:

| Title                                                                                                                                                       | Description                                                                                                   |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| [OpenL Studio Guide](https://openldocs.readthedocs.io/en/latest/documentation/guides/webstudio_user_guide) | Describes OpenL Studio, a web application for managing OpenL Tablets projects through web browser. |
| [OpenL Tablets Reference Guide](https://openldocs.readthedocs.io/en/latest/documentation/guides/reference_guide)             | Provides overview of OpenL Tablets technology, as well as its basic concepts and principles.                  |
| [OpenL Tablets Installation Guide](https://openldocs.readthedocs.io/en/latest/documentation/guides/installation_guide)       | Describes how to install and set up OpenL Tablets software.                                                   |
| [https://openl-tablets.org/](http://openl-tablets.org/)                                                                                                   | OpenL Tablets open source project website.                                                                    |

### Typographic Conventions

The following styles and conventions are used in this guide:

| Convention                 | Description                                                                                                                                                                                                                                                                                                                         |
|----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Bold**                   | Represents user interface items such as check boxes, command buttons, dialog boxes, drop-down list values, field names, menu commands, <br/>menus, option buttons, perspectives, tabs, tooltip labels, tree elements, views, and windows. <br/>Represents keys, such as **F9** or **CTRL+A**. <br/>Represents a term the first time it is defined. |
| `Courier`                  | Represents file and directory names, code, system messages, and command-line commands.                                                                                                                                                                                                                                              |
| Select **File \> Save As** | Represents a command to perform, such as opening the **File** menu and selecting **Save As**.                                                                                                                                                                                                                                       |
| *Italic*                   | Represents any information to be entered in a field. Represents documentation titles.                                                                                                                                                                                                                                               |
| \< \>                      | Represents placeholder values to be substituted with user specific values.                                                                                                                                                                                                                                                          |
| Hyperlink                  | Represents a hyperlink. Clicking a hyperlink displays the information topic or external source.                                                                                                                                                                                                                                     |



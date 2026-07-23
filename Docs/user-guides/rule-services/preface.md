## Preface

OpenL Tablets is a Business Rules Management System (BRMS) based on the tables presented in Excel documents. Using unique concepts, OpenL Tablets facilitates treating business documents containing business logic specifications as executable source code.

OpenL Tablets provides a set of tools addressing BRMS related capabilities including *OpenL Rule Services* *application* designed for integration of business rules into different customers’ applications.

The goal of this document is to explain how to configure Rule Services Core, that is, configure OpenL Rule Services or integrate the Rule Services Core module into the existing application, for different working environments and how to customize the services to meet particular customer requirements.

The following topics are included in this chapter:

-   [Audience](#audience)
-   [Related Information](#related-information)
-   [Typographic Conventions](#typographic-conventions)

### Audience

This guide is targeted at rule developers who integrate the Rule Services Core module and set up, configure, and customize OpenL Rule Services to facilitate the needs of customer rules management applications.

Basic knowledge of Java, Apache Tomcat, Ant, Maven, and Excel is required to use this guide effectively.

### Related Information

The following table lists sources of information related to contents of this guide:

| Title                                                                                                                                                       | Description                                                                                                   |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| [OpenL Studio Guide](../openl-studio/index.md) | Describes OpenL Studio, a web application for managing OpenL Tablets projects through web browser. |
| [OpenL Tablets Reference Guide](../reference-guide/index.md)             | Provides overview of OpenL Tablets technology, as well as its basic concepts and principles.                  |
| [OpenL Tablets Installation Guide](../installation-guide/index.md)       | Describes how to install and set up OpenL Tablets software.                                                   |
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

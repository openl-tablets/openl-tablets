## Introducing OpenL Tablets

This chapter introduces OpenL Tablets and describes its main concepts.

The following topics are included in this section:

-   [What Is OpenL Tablets?](#what-is-openl-tablets)
-   [Basic Concepts](#basic-concepts)
-   [System Overview](#system-overview)
-   [Installing OpenL Tablets](#installing-openl-tablets)
-   [Tutorials and Examples](#tutorials-and-examples)

### What Is OpenL Tablets?

**OpenL Tablets** is a Business Rules Management System (BRMS) and Business Rules Engine (BRE) based on tables presented in Excel documents. Using unique concepts, OpenL Tablets facilitates treating business documents containing business logic specifications as executable source code. Since the format of tables used by OpenL Tablets is familiar to business users, OpenL Tablets bridges a gap between business users and developers, thus reducing costly enterprise software development errors and dramatically shortening the software development cycle.

In a very simplified overview, OpenL Tablets can be considered as a table processor that extracts tables from Excel documents and makes them accessible from software applications.

The major advantages of using OpenL Tablets are as follows:

-   OpenL Tablets removes the gap between software implementation and business documents, rules, and policies.
-   Business rules become transparent to developers.
-   OpenL Tablets verifies syntax and type errors in all project document data, providing convenient and detailed error reporting.
-   OpenL Tablets can directly point to a problem in an Excel document.
-   OpenL Tablets provides calculation explanation capabilities, enabling expansion of any calculation result by pointing to source arguments in the original documents.
-   OpenL Tablets provides cross-indexing and search capabilities within all project documents.
-   OpenL Tablets provides the ability to create compact and easily readable business rules that become a part of business documentation.
-   Knowledge of Java or any other programming language is not required to create business rules with OpenL Tablets.

OpenL Tablets supports the `.xls, .xlsx, `and `.xlsm` file formats.

### Basic Concepts

This section describes the following main OpenL Tablets concepts:

-   [Rules](#rules)
-   [Tables](#tables)
-   [Projects](#projects)

#### Rules

In OpenL Tablets, a **rule** is a logical statement consisting of conditions and actions. If a rule is called and all its conditions are true, then the corresponding actions are executed. Basically, a rule is an IF-THEN statement. The following is an example of a rule expressed in human language:

*If a service request costs less than 1,000 dollars and takes less than 8 hours to execute, then the service request must be approved automatically.*

Instead of executing actions, rules can also return data values to the calling program.

#### Tables

Basic information OpenL Tablets deals with, such as rules and data, is presented in **tables**. Tables within one project must be unique and it is denoted by table name and input parameters. Nevertheless, different versions of the same table can have the same name and input parameters.

Tables are referenced by calling their names.

Different types of tables serve different purposes. For more information on table types, see [Table Types](#table-types).

#### Projects

An **OpenL Tablets project** is a container of all resources required for processing rule related information. Usually, a project contains Excel files, which are called **modules** of the project, and optionally Java code, library dependencies, and other components. For more information on projects, see [Working with Projects](#working-with-projects).

There can be situations where OpenL Tablets projects are used in the development environment but not in production, depending on the technical aspects of a solution.

### System Overview

The following diagram displays how OpenL Tablets is used by different types of users.

![](../../assets/images/reference/OpenLTabletsOverview.png)

*OpenL Tablets overview*

A typical lifecycle of an OpenL Tablets project is as follows:

1.  A business analyst creates an OpenL Tablets project in OpenL Studio.
2.  Optionally, development team may provide the analyst with a project in case of complex configuration.
3.  The business analyst creates correctly structured tables in Excel files based on requirements and includes them in the project.
    
    Typically, this task is performed through Excel or OpenL Studio in a web browser.
    
1.  Business analyst performs unit and integration tests by creating test tables and performance tests on rules through OpenL Studio.
    
    As a result, fully working rules are created and ready to be used.
    
1.  Development team creates other parts of the solution and employs business rules directly through the OpenL Tablets engine or remotely through web services.
2.  Whenever required, a business user updates or adds new rules to project tables.

OpenL Tablets business rules management applications, such as OpenL Studio, Rules Repository, and OpenL Rule Services, can be set up to provide self-service environment for business user changes.

### Installing OpenL Tablets

OpenL Tablets installation instructions are provided in [OpenL Tablets Installation Guide > Deploying OpenL Studio](https://openldocs.readthedocs.io/en/latest/documentation/guides/installation_guide/#deploying-openl-tablets-webstudio).
The development environment is required only for creating OpenL Tablets projects and launching OpenL Studio or OpenL Rule Services. If OpenL Tablets projects are accessed through OpenL Studio or web services, no specific software needs to be installed.

### Tutorials and Examples

OpenL Tablets provides a number of preconfigured projects developed for new users who want to learn working with OpenL Tablets quickly.

These projects are organized into following groups:

-   [Tutorials](#tutorials)
-   [Examples](#examples)

#### Tutorials

OpenL Tablets provides a set of the tutorial projects demonstrating basic OpenL Tablets features starting from very simple and following with more advanced projects. Files in the tutorial projects contain detailed comments allowing new users to grasp basic concepts quickly.

To create a tutorial project, proceed as follows:

1.  To open Repository Editor, in OpenL Studio, in the top line menu, click the **Repository** item.
2.  Click the **Create Project** button ![](../../assets/images/reference/22ea40949420dc3bdd150c652a5e91a7.png).
3.  In the **Create Project from** window, click the required tutorial name.
4.  Click **Create** to complete.
    
    The project appears in the **Projects** list of Repository Editor.
    
    ![](../../assets/images/reference/1d796f4911707c50e63e853581e010e6.png)
   
    *Creating tutorial projects*
    
1.  In the top line menu, click **Rules Editor**.

The project is displayed in the **Projects** list and available for usage. It is highly recommended to start from reading Excel files for examples and tutorials which provide clear explanations for every step involved.

![](../../assets/images/reference/454d965cb7e291d25f4f1b419e074285.png)

*Tutorial project in the OpenL Studio*

#### Examples

In addition to tutorials, OpenL Tablets provides several example projects that demonstrate how OpenL Tablets can be used in various business domains.

To create an example project, follow the steps described in [Tutorials](#tutorials), and in the **Create Project from** dialog, select an example to explore. When completed, the example appears in the OpenL Studio Rules Editor.


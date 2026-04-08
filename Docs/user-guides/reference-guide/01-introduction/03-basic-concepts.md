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

Different types of tables serve different purposes. For more information on table types, see [Table Types](../02-working-with-openl-tables/03-table-types/02-decision-table/01-decision-table-overview-and-structure.md#decision-table).

#### Projects

An **OpenL Tablets project** is a container of all resources required for processing rule related information. Usually, a project contains Excel files, which are called **modules** of the project, and optionally Java code, library dependencies, and other components. For more information on projects, see [Project Structure](../04-working-with-projects/01-project-structure.md#project-structure).

There can be situations where OpenL Tablets projects are used in the development environment but not in production, depending on the technical aspects of a solution.

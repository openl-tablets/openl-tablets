#### Configuration Table

This section describes the structure of the **configuration** table and includes the following topics:

-   [Configuration Table Description](#configuration-table-description)
-   [Defining Dependencies between Modules in the Configuration Table](02-defining-dependencies-between-modules-in-the-configuration-table.md#defining-dependencies-between-modules-in-the-configuration-table)

##### Configuration Table Description

OpenL Tablets allows splitting business logic into multiple Excel files, or modules. There are cases when rule tables of one module need to call rule tables placed in another module. A **configuration table** is used to indicate module dependency.

Another common purpose of a configuration table is when OpenL Tablets rules need to use objects and methods defined in the Java environment. To enable use of Java objects and methods in Excel tables, the module must have a configuration table. A **configuration table** provides information to the OpenL Tablets engine about available Java packages.

A configuration table is identified by the keyword **Environment** in the first row. No additional parameters are required. Starting with the second row, a configuration table must have two columns. The first column contains commands, and the second column contains input strings for commands.

The following commands are supported in configuration tables:

| Command      | Description                                                                                                                                                                                                                                                                                                                                       |
|--------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `dependency` | Adds a dependency module by its name. All data from that module becomes accessible in the current module. <br/>A dependency module can be located in the current project or its dependency projects. In simple words, this is how modules, <br/>often represented by Excel files, ‘communicate’ with each other if tables are split into different modules. |
| `import`     | Imports the specified Java package, class, or library so that its objects and methods can be used in tables.                                                                                                                                                                                                                                      |
| `language`   | Provides language import functionality.                                                                                                                                                                                                                                                                                                           |
| `extension`  | Expands OpenL Tablets capabilities with external set of rules. After adding, external rules are complied with OpenL Tablets rules and work jointly.                                                                                                                                                                                               |

For more information on dependency and import configurations, see [Project, Module, and Rule Dependencies](../../../04-working-with-projects/02-project-module-and-rule-dependencies.md#project-module-and-rule-dependencies).

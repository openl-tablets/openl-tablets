#### Data Table

A **data table** contains relational data that can be referenced by its table name from other OpenL Tablets tables or Java code as an array of data.

Data tables are widely used during testing rules process when a user defines all input test data in data tables and reuses them in several test tables of a project by referencing the data table from test tables. As a result, different tests use the same data tables to define input parameter values, for example, to avoid duplicating data.

Data tables can contain data types supported by OpenL Tablets or types loaded in OpenL Tablets from other sources. For more information on data types, see [Datatype Table](../01-datatype-table/01-introducing-datatype-tables.md#datatype-table) and [Working with Data Types](../../../03-openl-tablets-functions-and-supported-data-types/02-working-with-data-types.md#working-with-data-types).

The following topics are included in this section:

-   [Using Simple Data Tables](#using-simple-data-tables)
-   [Using Advanced Data Tables](02-using-advanced-data-tables.md#using-advanced-data-tables)
-   [Specifying Data in Data Tables with List and Map Fields](03-specifying-data-in-data-tables-with-list-and-map-fields.md#specifying-data-in-data-tables-with-list-and-map-fields)
-   [Specifying Data for Aggregated Objects](04-specifying-data-for-aggregated-objects.md#specifying-data-for-aggregated-objects)
-   [Ensuring Data Integrity](05-ensuring-data-integrity.md#ensuring-data-integrity)

##### Using Simple Data Tables

Simple data tables define a list of values of data types that have a simple structure.

1.  The first row is the header of the following format:
    
    `Data <data type> <data table name>`
    
    where data type is a type of data the table contains, it can be any predefined or vocabulary data type. For more information on predefined and vocabulary data types, refer to [Working with Data Types](../../../03-openl-tablets-functions-and-supported-data-types/02-working-with-data-types.md#working-with-data-types) and [Datatype Table](../01-datatype-table/01-introducing-datatype-tables.md#datatype-table).
    
1.  The second row is a keyword **this**.
2.  The third row is a descriptive table name intended for business users.
3.  In the fourth and following rows, values of data are provided.

An example of a data table containing an array of numbers is as follows.

![](../../../../ref_guide_images/simpleDataTable.png)

*Simple data table*

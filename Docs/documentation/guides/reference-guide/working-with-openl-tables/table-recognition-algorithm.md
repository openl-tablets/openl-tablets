### Table Recognition Algorithm

This section describes an algorithm of how the OpenL Tablets engine looks for supported tables in Excel files. It is important to build tables according to the requirements of this algorithm; otherwise, the tables are not recognized correctly.

OpenL Tablets utilizes Excel concepts of workbooks and worksheets, which can be represented and maintained in multiple Excel files. OpenL Tablets does not use any of Excel's formula capabilities though. Any calculations performed in OpenL Tablets are done using OpenL syntax, which is completely distinct from any formula syntax used by Excel. Excel worksheets can be named and arranged within one workbook in the order convenient to a user. Each worksheet, in its turn, is comprised of one or more tables. Workbooks can include tables of different types, each one supporting different underlying logic.

The general table recognition algorithm is as follows:

1.  The engine looks into each spreadsheet and tries to identify logical tables.
    
    Logical tables must be separated by at least one empty row or column or start at the very first row or column. Table parsing is performed from left to right and from top to bottom. The first populated cell that does not belong to a previously parsed table becomes the top-left corner of a new logical table.
    
1.  The engine reads text in the top left cell of a recognized logical table to determine its type.
    
    If the top left cell of a table starts with a predefined keyword, such table is recognized as an OpenL Tablets table.
    
    The following are the supported keywords:
    
    | Keyword               | Table type                                  |
    |-----------------------|---------------------------------------------|
    | Constants           | [Constants Table](table-types/constants-table.md#constants-table)        |
    | ColumnMatch         | [Column Match Table](table-types/column-match-table.md#column-match-table)   |
    | Data                | [Data Table](table-types/data-table/using-simple-data-tables.md#data-table)                  |
    | Datatype            | [Datatype Table](table-types/datatype-table/introducing-datatype-tables.md#datatype-table)           |
    | Environment         | [Configuration Table](table-types/configuration-table/configuration-table-description.md#configuration-table) |
    | Method              | [Method Table](table-types/method-table.md#method-table)               |
    | Properties          | [Properties Table](table-types/properties-table.md#properties-table)       |
    | Rules               | [Decision Table](table-types/decision-table/decision-table-overview-and-structure.md#decision-table)           |
    | Run                 | [Run Table](table-types/run-table.md#run-table)                    |
    | SimpleLookup        | [Simple Lookup Table](table-types/decision-table/simple-lookup-tables.md#simple-lookup-table) |
    | SimpleRules         | [Simple Rules Table](table-types/decision-table/simple-rules-tables.md#simple-rules-table)   |
    | SmartLookup         | [Smart Lookup Table](table-types/decision-table/smart-lookup-tables.md#smart-lookup-table)   |
    | SmartRules          | [Smart Rules Table](table-types/decision-table/smart-rules-tables.md#smart-rules-table)    |
    | Spreadsheet         | [Spreadsheet Table](table-types/spreadsheet-table/parsing-a-spreadsheet-table.md#spreadsheet-table)     |
    | TablePart           | [Table Part](table-types/table-part.md#table-part)                   |
    | TBasic or Algorithm | [TBasic Table](table-types/tbasic-table.md#tbasic-table)               |
    | Test                | [Test Table](table-types/test-table/understanding-test-tables.md#test-table)                  |
    
    All tables that do not have any of the preceding keywords in the top left cell are ignored. They can be used as comments in Excel files.
    
1.  The engine determines the width and height of the table using populated cells as clues.

It is a good practice to merge all cells in the first table row, so the first row explicitly specifies the table width. The first row is called the table **header**.

**Note:** To put a table title before the header row, an empty row must be used between the title and the first row of the actual table.


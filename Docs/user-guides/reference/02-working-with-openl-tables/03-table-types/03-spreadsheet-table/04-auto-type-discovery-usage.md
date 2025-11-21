##### Auto Type Discovery Usage

OpenL Tablets determines the cell data type automatically without its definition for a row or column. A user can turn on or off this behavior using the **autotype** property. If any row or column contains explicit data type definition, it supersedes automatically determined data type. The following example demonstrates that any data type can be correctly determined in auto mode. A user can put the mouse cursor over the “=” symbol to check the type of the cell value in OpenL Studio.

![](../../../../ref_guide_images/a4544122be707709459b3607776a75b0.png)

*Auto Type Discovery Property Usage inside Spreadsheet table*

The SpreadsheetResult cell type is automatically determined if a user refers to it from another table according to the following logic:

1.  Search for a cell with the same name is performed through all spreadsheets, and its type is set for the current cell.
2.  If several spreadsheets have cells with the same name but different types, the nearest common type is set for the current cell.

**Recommendation:** To ensure the system identifies types correctly, within the project, use data of the same type in the steps with the same name.

This logic also works when a user explicitly defines the type of the value as common SpreadsheetResult, for instance, in the following input parameter definition:

![](../../../../ref_guide_images/218ebc707a2d2761cb4186ed757517b9_2.jpeg)

*Defining the value type as SpreadsheetResult*

However, there are several limitations of auto type discovering when the system cannot possibly determine the cell data type:

-   Type identification algorithm is not able to properly identify the cell type when a cell refers to another cell with the same name because of occurred circular dependencies.
   
    ![](../../../../ref_guide_images/311742fa31d56cd04b21657dd6adeb5d_2.png)
    
    *Limitation for referring to another cell with the same name*
    
-   A user explicitly defines the return type of other Rules tables, such as Decision tables, as common SpreadsheetResult as follows:
    
    ![](../../../../ref_guide_images/2faa6c76b2b4654436c6b537b890774d.png)
    
    *Explicitly defining the return type of other rules tables*
    
    The type of undefined cells must be explicitly defined as a custom spreadsheet result type or any other suitable type to avoid uncertain Object typing.
    
-   There is a circular dependency in a spreadsheet table calling the same spreadsheet rule itself in a cell. This cell type must be explicitly defined to allow correct auto type discovering of the whole spreadsheet table as follows:
    
    ![](../../../../ref_guide_images/12ff48c76e91fa56fc1a4144fffdf0ea.png)
    
    *Defining a cell type explicitly*
    

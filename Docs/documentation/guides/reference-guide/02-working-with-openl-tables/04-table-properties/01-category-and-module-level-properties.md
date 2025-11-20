### Table Properties

For all OpenL Tablets table types, except for [Properties Table](../03-table-types/09-properties-table.md#properties-table), [Configuration Table](../03-table-types/09-configuration-table/01-configuration-table-description.md#configuration-table) and the **Other** type tables, that is, non-OpenL Tablets tables, properties can be defined as containing information about the table. A list of properties available in OpenL Tablets is predefined, and all values are expected to be of corresponding types. The exact list of available properties can vary between installations depending on OpenL Tablets configuration.

Table properties are displayed in the section which goes immediately after the table **header** and before other table contents. The properties section is optional and can be omitted in the table. The first cell in the properties row contains the **properties** keyword and is merged across all cells in column if more than one property is defined. The number of rows in the properties section is equal to the number of properties defined for the table. Each row in the properties section contains a pair of a property name and a property value in consecutive cells, that is, second and third columns.

![](../../../ref_guide_images/326e6522284b95a69e0afddbcb16d78b.png)

*Table properties example*

The following topics are included in this section:

-   [Category and Module Level Properties](../../04-table-properties/01-category-and-module-level-properties.md#category-and-module-level-properties)
-   [Default Value](../../04-table-properties/02-default-value.md#default-value)
-   [System Properties](../../04-table-properties/03-system-properties.md#system-properties)
-   [Properties for a Particular Table Type](../../04-table-properties/04-properties-for-a-particular-table-type.md#properties-for-a-particular-table-type)
-   [Rule Versioning](../../04-table-properties/05-rule-versioning.md#rule-versioning)
-   [Info Properties](../../04-table-properties/06-info-properties.md#info-properties)
-   [Dev Properties](../../04-table-properties/07-dev-properties.md#dev-properties)
-   [Properties Defined in the File Name](../../04-table-properties/08-properties-defined-in-the-file-name.md#properties-defined-in-the-file-name)
-   [Properties Defined in the Folder Name](../../04-table-properties/09-properties-defined-in-the-folder-name.md#properties-defined-in-the-folder-name)
-   [Keywords Usage in a File Name](../../04-table-properties/10-keywords-usage-in-a-file-name.md#keywords-usage-in-a-file-name)

#### Category and Module Level Properties

Table properties can be defined not only for each table separately, but for all tables in a specific category or a whole module. A separate [Properties Table](../03-table-types/09-properties-table.md#properties-table) is designed to define this kind of properties. Only properties allowed to be inherited from the category or module level can be defined in this table. Some properties, such as description, can only be defined for a table.

Besides the **Properties** table, the module level properties can also be defined in a name of the Excel file corresponding to the module. For more information on defining properties in the Excel file name, see [Properties Defined in the File Name](../../04-table-properties/08-properties-defined-in-the-file-name.md#properties-defined-in-the-file-name).

Properties defined at the category or module level can be overridden in tables. The priority of property values is as follows:

1.  Table.
2.  Category.
3.  Module.
4.  Default value.

    **Note:** The OpenL Tablets engine allows changing property values via the application code when loading rules.

#### Properties for a Particular Table Type

Some properties are used just for particular types of tables. It means that they make sense just for tables of a special type and can be defined only for those tables. Almost all properties can be defined for [Decision Tables](../table-types/decision-table/decision-table-overview-and-structure.md#decision-table), except for the **Datatype Package** property intended for [Datatype Tables](../table-types/datatype-table/introducing-datatype-tables.md#datatype-table), the **Scope** property used in [Properties Tables](../table-types/properties-table.md#properties-table), the [**Auto Type Discovery**](../table-types/spreadsheet-table/auto-type-discovery-usage.md#auto-type-discovery-usage) property used in [Spreadsheet Tables](../table-types/spreadsheet-table/parsing-a-spreadsheet-table.md#spreadsheet-table), and the **Precision** property designed for [Test Tables](../table-types/test-table/understanding-test-tables.md#test-table).

OpenL Tablets checks applicability of properties and produces an error if the property value is defined for table not intended to contain the property.

Applications using OpenL Tablets rules can utilize properties for different purposes. All properties are organized into the following groups:

| Group              | Description                                                     |
|--------------------|-----------------------------------------------------------------|
| Business dimension | [Business Dimension Properties](../../table-properties/rule-versioning.md#business-dimension-properties) |
| Version            | [Rule Versioning](../../table-properties/rule-versioning.md#rule-versioning)                             |
| Info               | [Info Properties](../../table-properties/info-properties.md#info-properties)                            |
| Dev                | [Dev Properties](../../table-properties/dev-properties.md#dev-properties)                               |

Properties of the **Business Dimension** and **Rule Versioning** groups are used for rule versioning. They are described in detail further on in this guide.


#### Properties for a Particular Table Type

Some properties are used just for particular types of tables. It means that they make sense just for tables of a special type and can be defined only for those tables. Almost all properties can be defined for [Decision Tables](../03-table-types/02-decision-tab../02-decision-table-overview-and-structure.md#decision-table), except for the **Datatype Package** property intended for [Datatype Tables](../03-table-types/01-datatype-table/01-introducing-datatype-tables.md#datatype-table), the **Scope** property used in [Properties Tables](../03-table-types/09-properties-table.md#properties-table), the [**Auto Type Discovery**](../03-table-types/03-spreadsheet-table/04-auto-type-discovery-usage.md#auto-type-discovery-usage) property used in [Spreadsheet Tables](../03-table-types/03-spreadsheet-table/01-parsing-a-spreadsheet-table.md#spreadsheet-table), and the **Precision** property designed for [Test Tables](../03-table-types/08-test-table/01-understanding-test-tables.md#test-table).

OpenL Tablets checks applicability of properties and produces an error if the property value is defined for table not intended to contain the property.

Applications using OpenL Tablets rules can utilize properties for different purposes. All properties are organized into the following groups:

| Group              | Description                                                     |
|--------------------|-----------------------------------------------------------------|
| Business dimension | [Business Dimension Properties](../../04-table-properties/05-rule-versioning.md#business-dimension-properties) |
| Version            | [Rule Versioning](../../04-table-properties/05-rule-versioning.md#rule-versioning)                             |
| Info               | [Info Properties](../../04-table-properties/06-info-properties.md#info-properties)                            |
| Dev                | [Dev Properties](../../04-table-properties/07-dev-properties.md#dev-properties)                               |

Properties of the **Business Dimension** and **Rule Versioning** groups are used for rule versioning. They are described in detail further on in this guide.


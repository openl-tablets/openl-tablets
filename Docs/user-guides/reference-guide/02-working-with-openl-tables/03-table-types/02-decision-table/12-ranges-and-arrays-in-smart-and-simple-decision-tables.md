##### Ranges and Arrays in Smart and Simple Decision Tables

Range and array data types can be used in simplified and smart rules and lookup tables. If a condition is represented as an array or range, the rule is executed for any value from that array or range. As an example, in the following image, there is the same Car Price for all regions of Belarus and Great Britain, so, using an array, three rows for each of these countries can be replaced by a single one as displayed in the following table.

![Simple lookup table with an array](../../../ref-guide-images/simpleLookupTableArray.png)

*Simple lookup table with an array*

If a string value contains a comma, the value must be delimited with the backslash (\\) separator followed by a comma as illustrated for **Driver\\, Passenger\\, Side** in the following example. Otherwise, it is treated as an array of string elements.

![Comma within a string value in a Simple Rule table](../../../ref-guide-images/commaWithinStringValueSimpleRule.png)

*Comma within a string value in a Simple Rule table*

The following example explains how to use a range in a simple rules table.

![Simple rules table with a Range](../../../ref-guide-images/simpleRulesTableRange.png)

*Simple rules table with a Range*

OpenL looks through the **Condition** column, that is, **ZIP Code**, meets a range, which is not necessarily the first one, and defines that all the data in the column are IntRange, where Integer is defined in the header, **Integer vehicleZip**.

Simple and smart rules and smart lookup tables support using arrays of ranges. In the following example, the Z100-Z105, Z107, Z109 condition is a string range array where single elements Z107, Z109 are treated by system as ranges Z107-Z107, Z109-Z109.

![Using arrays of ranges in a table](../../../ref-guide-images/usingArraysRangesTable.png)

*Using arrays of ranges in a table*

In a smart rules or smart lookup table, a String condition whose values parse as ranges, such as `A - Z` or `Z100-Z105`, becomes a StringRange condition, so a text with a dash between two words, such as `EE Choice Non-Taxable`, is read as the range from `EE Choice Non` to `Taxable`. To keep such a value a plain text, write it in double quotes: `"EE Choice Non-Taxable"` matches only the text `EE Choice Non-Taxable`. One quoted value makes every value of the column a plain text, matched as a simple rules table matches a String condition. Another way is a vocabulary data type that lists the values, since a value of a vocabulary is never a range.

**Note:** String ranges are only supported in smart rules tables. For more information on range data types in OpenL Tablets, see [Range Data Types](../../../03-functions-and-data-types/02-working-with-data-types.md#range-data-types).

###### Multiple Return Columns in Smart Rules Tables

A smart rules table can contain up to three return columns. If the first return column contains a non-empty result, it is returned, otherwise, the next return column is scanned until the non-empty result is found or the last return column is verified.

The following example illustrates a table with multiple return columns.

![](../../../../ref_guide_images/exampleSmartRulesTableMultipleReturn.jpeg)

*Example of a smart rules table with multiple return columns*

In this example, the **QuoteVolume** rule has one condition, **Coverage Type,** and two return columns, **Volume 1** and **Volume 2.** An example of the test table for this rule table is as follows.

![](../../../../ref_guide_images/exampleTestTableRuleTableMultiple.png)

*Example of the test table for a rule table with multiple return columns*

In the test table, **Plan 1** is not of the **Medical** coverage type, so the second rule line is applied. In the test table, for the first test case, both **History Premium** and **History Rate** are provided, so **Volume** is calculated as 480 by the rule of **Volume 1** column. For the second and third test case, one of inputs is missing, so **Volume 1** returns an empty result, and the second return column calls another rule causing the result of 500 returned.

**Note for experienced users:** In case of a complex return object, only one compound return consisting of several return columns is allowed. All other returns can be defined using the formulas, that is, the `new() `operator or by calling another rule that returns the object of the corresponding type. For more information on complex return objects, see [Result of Custom Data Type in Smart and Simple Rules Tables](06-result-of-custom-data-type-in-smart-and-simple-rules-tables.md#result-of-custom-data-type-in-smart-and-simple-rules-tables).



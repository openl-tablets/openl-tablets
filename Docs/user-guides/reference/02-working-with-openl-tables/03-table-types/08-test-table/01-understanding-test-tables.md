#### Test Table

This section describes test tables and context variables available in these tables. The following topics are included:

-   [Understanding Test Tables](#understanding-test-tables)
-   [Context Variables Available in Test Tables](02-context-variables-available-in-test-tables.md#context-variables-available-in-test-tables)
-   [Creating a Test Table for a Spreadsheet or Decision Table with SpreadsheetResult as Input Parameter](03-creating-a-test-table-for-a-spreadsheet-or-decision-table-with-spreadsheetresult-as-input-parameter.md#creating-a-test-table-for-a-spreadsheet-or-decision-table-with-spreadsheetresult-as-input-parameter)

##### Understanding Test Tables

A **test table** is used to perform unit and integration tests on executable rule tables, such as decision tables, spreadsheet tables, and method tables. It calls a particular table, provides test input values, and checks whether the returned value matches the expected value.

For example, in the following diagram, the table on the left is a decision table but the table on the right is a unit test table that tests data of the decision table.

![](../../../../ref_guide_images/decisionTableItsUnitTestTable.png)

*Decision table and its unit test table*

A test table has the following structure:

1.  The first row is the table header, which has the following format:
    
    `Test <rule table name> <test table name>`
    
    **Test** is a keyword that identifies a test table. The second parameter is the name of the rule table to be tested. The third parameter is the name of the test table and is optional.
   
1.  The second row provides a separate cell for each input parameter of the rule table followed by the **\_res\_** column, which typically contains the expected test result values.
2.  The third row contains display values intended for business users.
3.  Starting with the fourth row, each row is an individual test case.

For more information on how to specify values of input parameters and expected test results of complex constructions, see [Specifying Data for Aggregated Objects](../04-data-table/04-specifying-data-for-aggregated-objects.md#specifying-data-for-aggregated-objects) and [Ensuring Data Integrity](../04-data-table/05-ensuring-data-integrity.md#ensuring-data-integrity).

If a test table field is a list or a map, it can be used to create a data table or test table in the same way as for data tables as described in [Specifying Data in Data Tables with List and Map Fields](../04-data-table/03-specifying-data-in-data-tables-with-list-and-map-fields.md#specifying-data-in-data-tables-with-list-and-map-fields).

**Note for experienced users:** Test tables can be used to execute any Java method. In this case, a method table must be used as a proxy.

When a test table is called, the OpenL Tablets engine calls the specified rule table for every row in the test table and passes the corresponding input parameters to it.

If there are several rule tables with a different number of parameters but identical names and a test table is applicable to all rule tables, the test table is matched with the rule table which list of test input parameters matches exactly the list of rules input parameters in the test table. If there are extra parameters in all rule tables, or input parameters of multiple rule tables match test input parameters exactly, the **Method is ambiguous** message is displayed.

Application runtime context values are defined in the runtime environment. Test tables for a table, overloaded by business dimension properties, must provide values for the runtime context significant for the tested table. Runtime context values are accessed in the test table through the **\_context\_** prefix. An example of a test table with the context value Lob follows:

![](../../../../ref_guide_images/exampleTestTableContextValue.png)

*An example of a test table with a context value*

For a full list of runtime context variables available, their description, and related Business Dimension versioning properties, see [Context Variables Available in Test Tables](02-context-variables-available-in-test-tables.md#context-variables-available-in-test-tables).

Tests are numbered automatically. In addition to that, ID (*id*) can be assigned to the test table thus enabling a user to use it for running specific test tables by their IDs as described in [OpenL Studio Guide > Defining the ID Column for Test Cases](https://openldocs.readthedocs.io/en/latest/documentation/guides/webstudio_user_guide/#defining-the-id-column-for-test-cases).

The **\_description\_** column can be used for entering useful information.

The **\_error\_** column of the test table can be used for a test algorithm where the **error** function is used. The OpenL Tablets Engine compares an error message to the value of the **\_error\_** column to decide if test is passed.

![](../../../../ref_guide_images/exampleTestTableExpectedErrorColumn.png)

*An example of a test table with an expected error column*

If OpenL Tablets projects are accessed and modified through OpenL Studio, UI provides convenient utilities for running tests and viewing test results. For more information on using OpenL Studio, see [OpenL Studio Guide](https://openldocs.readthedocs.io/en/latest/documentation/guides/webstudio_user_guide).


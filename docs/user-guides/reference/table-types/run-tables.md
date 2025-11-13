If there are several rule tables with a different number of parameters but identical names and a test table is applicable to all rule tables, the test table is matched with the rule table which list of test input parameters matches exactly the list of rules input parameters in the test table. If there are extra parameters in all rule tables, or input parameters of multiple rule tables match test input parameters exactly, the **Method is ambiguous** message is displayed.

Application runtime context values are defined in the runtime environment. Test tables for a table, overloaded by business dimension properties, must provide values for the runtime context significant for the tested table. Runtime context values are accessed in the test table through the **\_context\_** prefix. An example of a test table with the context value Lob follows:

![](../../../assets/images/reference/eab9f9a058ae179b23d673221835818f.png)

*An example of a test table with a context value*

For a full list of runtime context variables available, their description, and related Business Dimension versioning properties, see [Context Variables Available in Test Tables](#context-variables-available-in-test-tables).

Tests are numbered automatically. In addition to that, ID (*id*) can be assigned to the test table thus enabling a user to use it for running specific test tables by their IDs as described in [OpenL Studio Guide > Defining the ID Column for Test Cases](https://openldocs.readthedocs.io/en/latest/documentation/guides/webstudio_user_guide/#defining-the-id-column-for-test-cases).

The **\_description\_** column can be used for entering useful information.

The **\_error\_** column of the test table can be used for a test algorithm where the **error** function is used. The OpenL Tablets Engine compares an error message to the value of the **\_error\_** column to decide if test is passed.

![](../../../assets/images/reference/2cf1f943280ccde74bb35ae41dec4219.png)

*An example of a test table with an expected error column*

If OpenL Tablets projects are accessed and modified through OpenL Studio, UI provides convenient utilities for running tests and viewing test results. For more information on using OpenL Studio, see [OpenL Studio Guide](https://openldocs.readthedocs.io/en/latest/documentation/guides/webstudio_user_guide).

##### Context Variables Available in Test Tables

The following runtime context variables are used in OpenL Tablets and their values can be specified in OpenL test tables using syntax \_`context_.<context name> `in a column header:

| Context          | Context name <br/>in rule tables | Type     | Related versioning <br/>properties | Property names <br/>in rule tables | Description                                                           |
|------------------|----------------------------------|----------|-------------------------------|------------------------------------|-----------------------------------------------------------------------|
| Current Date     | `currentDate`                    | `Date`   | Effective / Expiration dates  | `effectiveDate, expirationDate`    | Date on which the rule is performed. <br/>It is not equal to today’s date. |
| Request Date     | `requestDate`                    | `Date`   | Start / End Request dates     | `startRequestDate, endRequestDate` | Date when the rule is applied.                                        |

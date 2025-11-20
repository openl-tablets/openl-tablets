#### Run Table

A **run table** calls a particular rule table multiple times and provides input values for each individual call. Therefore, run tables are similar to test tables, except they do not perform a check of values returned by the called method.

**Note for experienced users:** Run tables can be used to execute any Java method.

An example of a run method table is as follows.

![](../../../../ref_guide_images/a9d71a0285081f13294c74f89eedd0b4.png)

*Run table*

This example assumes there is a rule `append` defined with two input parameters, `firstWord` and `secondWord`. The run table calls this rule three times with three different sets of input values.

A run table has the following structure:

1.  The first row is a table header, which has the following format:
    
    ```
    Run <name of rule table to call> <run table name>
    ```
    
    The run table name is optional.
    
1.  The second row contains cells with rule input parameter names.
2.  The third row contains display values intended for business users.
3.  Starting with the fourth row, each row is a set of input parameters to be passed to the called rule table.

For more information on how to specify values of input parameters which have complex constructions, see [Specifying Data for Aggregated Objects](03-data-table/04-specifying-data-for-aggregated-objects.md#specifying-data-for-aggregated-objects) and [Ensuring Data Integrity](03-data-table/05-ensuring-data-integrity.md#ensuring-data-integrity).


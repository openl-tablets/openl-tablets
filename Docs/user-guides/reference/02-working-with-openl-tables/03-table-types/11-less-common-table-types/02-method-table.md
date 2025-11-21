#### Method Table

A **method table** is a Java method described within a table. An example of a method table is as follows:

![](../../../../ref_guide_images/36c37ea2296c8f5f2bab226b9154a5ff.png)

*Method table*

The first row is a table header, which has the following format:

`<keyword> <return type> <table name> (<input parameters>)`

where `<keyword>` is either **Method** or **Code**.

The second row and the following rows are the actual code to be executed. They can reference parameters passed to the method and all Java objects and tables visible to the OpenL Tablets engine. Code rows may not contain the `<return>` keyword. In this case, the last row of the table is returned as the table result.

This table type is intended for users experienced in programming in developing rules of any logic and complexity.


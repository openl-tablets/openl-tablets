-   calculated using expressions, that is, formulas or by calling other rules

The input parameter attribute type is not specified when calling a nested rule.

In the following example, a nested rule table **HeapedCommissionStrategy** is called from the **CommissionCalculation** smart rule table.

![](../../../assets/images/reference/90e6c4e19266176d5a9806580bfd9065.png)

*Calling a nested rule table from a rule table*

The return value type of the nested rule table must match the return value type of the current rule table.

Sometimes specific values must be sent to the nested table. In this case, input parameter values can be specified as follows:

-   decalred in the quatation marks “” for String values
-   set to true or false for Boolean values
-   provided as a number for Double and Integer values
-   set to null for empty values

For example, usually the detailed information about children is not included in the insurance policy and so default values are used to get the rates:

![](../../../assets/images/reference/db14be6ea04a95ef48fbd0c2a540078a.png)

![](../../../assets/images/reference/6f985285bc51a83e1cec893c615a5b57.png)

*Declaring specific inputs when calling a nested rule table*

##### Using Referents from Return Column Cells

When a condition value from a cell in the Return column must be called, specify the value by using `$C<n>.<variable name> `in the **Return** column.

![](../../../assets/images/reference/EPBDS11443_1.png)

*A Decision table with referents inside the Return column*

![](../../../assets/images/reference/EPBDS11443_2.png)

*Tracing Decision table with referents*

Conditions, actions, and result parameters can be accessed from another condition, action, or result using simplified syntax. The same syntax can be also used for smart rules if external tables are used for condition, action, or result.

![](../../../assets/images/reference/86621f116d77a4afeb5c6c8c62241de2.png)

*Accessing a condition parameter from a return expression by simplified syntax*

##### Using Rule Names and Rule Numbers in the Return Column

Rule names and numbers can be used in the return expression to find out which rule is executed. `$RuleId` is an implicit number of the rule in the rule table. `$Rule` is used to get the rule name explicitly defined by the Rule column.

In the following rule example, the second rule row is executed, and rule ID \#2 is stored in the **priority** field of the return:

![](../../../assets/images/reference/ef7b4bd9e08bec2fe48e1d986f234440.png)

*Using $RuleId and $Rule in the rules table*

##### Using References to Expressions

References to expressions can be used in decision tables. They can be referenced from table headers and within table body.

-	**$Expr.C1** is used to reference the expression for condition C1. To address action or return expression use RET1 and A1 respectively.
![](../../../assets/images/reference/a3d9e45b2c71f8b9a0cbe8f7235a41d6.png)
-	**$Expr.$C1.param1** is an expression defined as a value in a column for the **param1** condition parameter. $C1 is optional. For instance, in the example below, parameter **cond** is the condition parameter for condition C2. It's important to use named parameters which is possible in decision tables of [Rules type](#decision-table-structure) or when working with [external conditions, actions, or returns](#external-tables-usage-in-smart-decision-tables) in smart tables.
![](../../../assets/images/reference/f49c7836d2a91e7c4b2e1098d6f375c4.png)

`$Expr.C1, $Expr.$C1.param1` return the expression type that contains following attributes:
- `ast` - returns AST (Abstract Syntax Tree) tree for the expression
- `textValue` - returns a string representing an expression

Note: If a cell, which is expected to contain an expression or formula, is empty, it will return null.

#### Datatype Table

This section describes datatype tables and includes the following topics:

-   [Introducing Datatype Tables](#introducing-datatype-tables)
-   [Inheritance in Data Types](#inheritance-in-data-types)
-   [Vocabulary Data Types](#vocabulary-data-types)

##### Introducing Datatype Tables

A **Datatype table** defines an OpenL Tablets data structure. A Datatype table is used for the following purposes:

-   create a hierarchical data structure combining multiple data elements and their associated data types in hierarchy
-   define the default values
-   create vocabulary for data elements

A compound data type defined by Datatype table is called a **custom data type**. Datatype tables enable users to create their own data model which is logically suited for usage in a particular business domain.

For more information on creating vocabulary for data elements, see [Vocabulary Data Types](#vocabulary-data-types).

A Datatype table has the following structure:

1.  The first row is the header containing the **Datatype** keyword followed by the name of the data type.
2.  Every row, starting with the second one, represents one attribute of the data type.
    
    The first column contains attribute types, and the second column contains corresponding attribute names.
    
    **Note:** While there are no special restrictions, usually an attribute type starts with a capital letter and attribute name starts with a small letter.
    
1.  The third column is optional and defines default values for fields.

Consider the case when a hierarchical logical data structure must be created. The following example of a Datatype table defines a custom data type called **Person**. The table represents a structure of the **Person** data object and combines **Person’s** data elements, such as name, social security number, date of birth, gender, and address.

![](../../../assets/images/reference/ad45a4a8dffb2ceaaa9bb4bad359eed3.png)

*Datatype table Person*

Note that data attribute, or element, address of **Person** has, by-turn, custom data type **Address** and consists of zip code, city, and street attributes.

![](../../../assets/images/reference/ad05529167018d2d8061c354bdcc3099.png)

*Datatype table Address*

The following example extends the **Person** data type with default values for specific fields.

![](../../../assets/images/reference/6e498e992fff0996f4f0bfdaa41fe293.png)

*Datatype table with default values*

The **Gender** field has the given value **Male** for all newly created instances if other value is not provided. If a value is provided, it has a higher priority over the default value and overrides it.

One attribute type can be used for many attribute names if their data elements are the same. For example, insuredGender and spouseGender attribute names may have Gender attribute type as the same list of values (Male, Female) is defined for them.

**Note for experienced users:** Java beans can be used as custom data types in OpenL Tablets. If a Java bean is used, the package where the Java bean is located must be imported using a configuration table as described in [Configuration Table](#configuration-table).

Consider an example of a Datatype table defining a custom data type called Corporation. The following table represents a structure of the Corporation data object and combines Corporation data elements, such as ID, full name, industry, ownership, and number of employees. If necessary, default values can be defined in the Datatype table for the fields of complex type when combination of fields exists with default values.


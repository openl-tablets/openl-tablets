#### TBasic Table

A **TBasic** table is used for code development in a more convenient and structured way rather than using Java or Business User Language (BUL). It has several clearly defined structural components. Using Excel cells, fonts, and named code column segments provides clearer definition of complex algorithms.

**Important:** As this table type is Java code related, TBasic table must not be used unless there is a critical need for it and no other table type can represent the logic in a simpler way more comprehensive for business users.

In a definite UI, it can be used as a workflow component.

The format of the TBasic table header is as follows:

```
TBasic <ReturnType> <TechnicalName> (ARGUMENTS)
```

The following table describes the TBasic table header syntax:

| Element       | Description                                       |
|---------------|---------------------------------------------------|
| TBasic        | Reserved word that defines the type of the table. |
| ReturnType    | Type of the return value.                         |
| TechnicalName | Algorithm name.                                   |
| ARGUMENTS     | Input arguments as for any executable table.      |

The following table explains the recommended parts of the structured algorithm:

| **Element**                             | **Description**                                                 |
|-----------------------------------------|-----------------------------------------------------------------|
| Algorithm precondition or preprocessing | Executed when the component starts execution.                   |
| Algorithm steps                         | Represents the main logic of the component.                     |
| Postprocess                             | Identifies a part executed when the algorithm part is executed. |
| User functions and subroutines          | Contains user functions definition and subroutines.             |


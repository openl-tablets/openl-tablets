##### Using References to Expressions

References to expressions can be used in decision tables. They can be referenced from table headers and within table body.

-	**$Expr.C1** is used to reference the expression for condition C1. To address action or return expression use RET1 and A1 respectively.
![](../../../ref-guide-images/usingReferencesExpressions.png)
-	**$Expr.$C1.param1** is an expression defined as a value in a column for the **param1** condition parameter. $C1 is optional. For instance, in the example below, parameter **cond** is the condition parameter for condition C2. It's important to use named parameters which is possible in decision tables of [Rules type](decision-table-overview-and-structure.md#decision-table-structure) or when working with [external conditions, actions, or returns](11-external-tables-usage-in-smart-decision-tables.md#external-tables-usage-in-smart-decision-tables) in smart tables.
![](../../../ref-guide-images/usingReferencesExpressions_1.png)

`$Expr.C1, $Expr.$C1.param1` return the expression type that contains following attributes:
- `ast` - returns AST (Abstract Syntax Tree) tree for the expression
- `textValue` - returns a string representing an expression

Note: If a cell, which is expected to contain an expression or formula, is empty, it will return null.


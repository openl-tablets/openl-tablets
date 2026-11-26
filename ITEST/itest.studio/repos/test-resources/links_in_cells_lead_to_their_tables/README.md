# Links in cells lead to their tables

A word in a cell that names a datatype, a data table, a field, a parameter or a variable is reported among the
usages of the cell, with the text of its tooltip and, when it names a table, the table it leads to.

`010-setup` creates a project from the workbook of the test case IPBQA-25751, opens it, and captures the ids of the
tables from the table list of the module.

`020-links` reads each table with its meta info:

- the Spreadsheet header and a step type lead to `testDatatype`; in `= (testDatatype) a_byte` the cast leads to
  `testDatatype` and the parameter `a_byte` tells its type;
- in SimpleRules the constructor `new testDatatype ("3")` tells its arguments, the cast `(testDatatype)CondValue`
  leads to `testDatatype`, and the column titles tell the condition, the expression and the type;
- in TBasic the parameter `n` tells its type and leads to `testDatatype3`, the local variables `i` and `factorial`
  and the subroutine `Calculation` tell their types, the constructor tells its arguments, and the field `Value5`
  tells the datatype that declares it;
- the ColumnMatch, SmartLookup and SmartRules headers lead to `testDatatype`, and the SmartLookup and SmartRules
  column titles tell the condition, the expression and the type;
- in a Test table `>myData2` leads to the data table `myData2`.

`MethodTable` is left out: the type of a declared variable, as in `testDatatype s = new testDatatype();`, is
checked by `EPBDS-17324_unlinked_type_of_a_declared_variable`.

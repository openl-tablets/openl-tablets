# EPBDS-16692: the values of a vocabulary reach every schema of a run

The input of a spreadsheet taking a vocabulary parameter and a datatype with vocabulary fields carries the values
the vocabulary allows: as the `enum` of the parameter, of the datatype field, and of the elements of an array
field. The result of the run describes the spreadsheet result the same way: a cell of the vocabulary type and an
array cell carry the values too.

The project is the one of the Rule Services suite: `Datatype myType <String>` with the values `bla1`, `bla2` and
`bla3`, `Datatype Bean` with the fields `myType kind`, `myType[] kinds` and `String note`, and
`Spreadsheet SpreadsheetResult calc(myType a, Bean b)` whose cells return them.

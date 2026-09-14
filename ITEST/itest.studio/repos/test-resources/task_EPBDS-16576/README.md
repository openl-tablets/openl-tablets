# EPBDS-16576 — comparing two Excel files

Two workbooks that differ in three ways are compared: a table whose cells changed (`Datatype Person`), a table only
the first file holds (`Datatype Contact`) and a table only the second one holds (`Datatype Address`).

The comparison parses both files, so it is started and read in two steps: the start answers with the identifier the
comparison is known by, and the result is read once the comparison answers with one rather than with a `409`.

The scenario then reads the two sides of a changed table with the addresses of the cells that differ, reads a table
only one file holds, asks for a table the comparison does not hold, releases the comparison, and asks for it again
once it is gone. Finally it offers a file that is not a workbook, which is refused before anything is compared.

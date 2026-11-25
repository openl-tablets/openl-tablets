# EPBDS-17324 — the type of a declared variable leads to the table declaring it

The type written in the declaration of a variable, such as `Member` in `Member m = new Member();`, is reported among
the usages of its cell as a `datatype` usage, with the table that declares the type, in every table whose cells
declare variables. A screen draws it as a link to that table, with a hint.

`010-setup` creates a project, opened, holding the datatype `Member` and three tables declaring variables of it,
and captures the ids of the four tables. The project is of its own: the one of `task_EPBDS-12301-type-named-as-value`
holds a vocabulary and a spreadsheet reading it, no table declaring a variable.

`020-meta-info` reads the cells of each table with their meta info:

- the Method table `member`: `Member m = new Member(); return m;`;
- the Spreadsheet `declarations`: an array variable, a statement declaring two variables, which reports the type
  once, a variable of a Java type, which has a hint without a link, and the element an array index operator declares;
- the Decision Table `pick`: the return column declaring a variable.

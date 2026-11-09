# A table takes the look of its compiled roles at its first save

With **Format tables on save**, a save that creates a table, or changes how the compiler reads one, binds that table
alone against the module compiled before and formats it in the roles the compiler gives it. The look in the file
after the first save is the look a compile and **Reformat Excel files** give, with no compile of the module.

The project starts empty, from the **Empty Project** template. Every table compiles with no error or warning once
created and once edited: no read carries a message. An edit writes several cells at one save (`actions/batch`) where
one alone would leave the table broken.

- `020-create-tables` — a table of every kind is created through the API with no regions, in the order of what they
  use: the Datatypes `Car`, `Result`, `Person` and `Claim` and the method `GradeOf` come before the tables using them.
  The raw read with its styles follows each create. `ClaimCode`, a SmartRules table, takes one argument of the type
  `Claim` and matches its four conditions to the fields of the argument. A table whose body the compiler reads either
  way is written turned, a row of its body for each column under the header: the decision tables `GradeOf`, `Turned`,
  `SimpleGrade`, `SmartGrade`, `SimpleRate` and `SmartRate`, the Test table `GradeTest`, the Run table `GradeRun` and
  the Data table `carsTurned`. A Spreadsheet, a TBasic, a ColumnMatch and a Properties table are read one way only.
- `030-edit-tables` — one save edits each table so that it reads in other roles, and the raw read with its styles
  follows it:
    - `Premium`, a `SpreadsheetResult`, is made to return `Double`: its last step becomes the result;
    - `Quote`, a `SpreadsheetResult`, marks its step `Doubled*`, which becomes a bold key;
    - `Payment` names its step `Total` `RETURN`, which becomes the result in place of the last step;
    - `RulesGrade`, of 70 rules, has its condition `C2` and its return `RET1` change places; it is read at the top
      and at the bottom, as a save binds its first rules alone first and the whole table once they take other roles;
    - `SimpleGrade` and `SmartGrade` return the type `Result` and take one parameter less: their line `Level`
      becomes a return;
    - `SimpleRate`, a SimpleLookup table, takes one parameter more: its second line becomes a condition;
    - `SmartRate`, a SmartLookup table, takes one parameter less: its line `Zone` becomes a line of ages;
    - `Calc`, a TBasic table, has its step `SET` return;
    - `GradeTest`, a Test table, has its argument `level` and its expected result `_res_` change places;
    - `people`, a Data table, takes keys of its own, `_PK_`, in its first field.
- `040-reformat` — a Reformat formats every table as its compile reads it and leaves each table as its last save left
  it: each read expects the body of the read after the last create or edit of its table, kept beside that read as a
  `.json` file both share. A body only one read expects stays in its `.resp`.

A save formatting a table in the roles of the compile before it fails every read of `030-edit-tables` and the reads
of the seven decision tables in `020-create-tables`: laid out from its text alone, a decision table is read upright,
and a SmartRules or a SimpleRules table takes a condition for each of its arguments, so `ClaimCode` takes the
conditions after its first for returns. The other created tables read the same either way: their text alone already
tells their roles, and a turned Test, Run or Data table by its field names, as the compiler tells it.

A ColumnMatch table is created, not edited: one save cannot give it the weights and the scores another algorithm
reads with no error. An edit of a Method, Properties, Environment or Constants table changes no look in the file, and
a Datatype or a Vocabulary table is not bound when it is edited.

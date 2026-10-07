# EPBDS-16796 — a call of a rule written in versions leads to the table choosing the version

`Hello` is written twice, for the states CA and NY, and `CallHello` calls it. The compiler builds a table of its
own to choose between the versions, `validateGapOverlap_Hello`. It sits in no workbook, so the module refused it
as a table it does not hold: the word `Hello` in `CallHello` was plain text, and the table could not be opened.

- The table list of the module names the versions, the caller and `validateGapOverlapCheck`, a table its author
  wrote under the name the compiler gives such a table, but not the built table.
- The word `Hello` names the built table and the module it is read through; its id is captured from there, since
  it changes with every compilation of the module.
- The built table opens through the module, says it cannot be run, and the `standard` theme draws it as a Rules
  table.
- Nothing but the compiler writes it, and it is never run: editing, copying, running and tracing it are refused
  with `400`, the message naming the table and saying it is virtual and can only be viewed.
- `validateGapOverlapCheck` is the author's own: written in the workbook, it reads as a table that can be run, is
  edited and runs.

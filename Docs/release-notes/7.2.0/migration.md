---
title: "OpenL Tablets 7.2.0 Migration Notes"
---

Upgrading to OpenL Tablets 7.2.0 requires no database changes and no Java version change. A fix of the rules engine
changes how an existing project runs, as listed for the rules authors.

## Rules Authors

* **An array operator over `SpreadsheetResult[]` reads a step the element does not have from its spreadsheet.** In a
  spreadsheet, the condition or the expression of an operator applied to an array of the general `SpreadsheetResult`
  type, such as `results[select first having $Code == $Wanted]`, reads `$Wanted` of an element without a `Wanted`
  step from the `Wanted` step of the spreadsheet with the operator. That happens when no other spreadsheet has a
  `Wanted` step, and the step and the steps it reads come before the operator. Before, such an element gave an
  empty value, so the operator found no element. A step an element has is read from it as before, the results of the
  same spreadsheet passed to it as `SpreadsheetResult[]` included. Write `this.$Wanted` to read the step of the
  spreadsheet for every element, or name the element, as in
  `results[(result) select first having result.$Code == result.$Wanted]`, to keep the empty value of an element
  without the step.

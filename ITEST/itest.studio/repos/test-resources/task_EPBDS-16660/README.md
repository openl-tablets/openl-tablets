# EPBDS-16660: an array of a vocabulary offers the values for its elements

The input of a rule table taking an array of a vocabulary carries the values the vocabulary allows as the `enum`
of the array's `items`, so every element of the array is offered as a list to pick from. The schema used to be
generated from the Java class of the parameter, which for an array of a vocabulary is an array of its base type.

The project is the workbook attached to the ticket: `Datatype myType <String>` with the values `bla1`, `bla2`
and `bla3`, and rules taking `myType[] my` — the one run here is `SmartRules Double myRule2(String value,
myType[] my)`.

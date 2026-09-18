# EPBDS-16605: a vocabulary parameter offers its values, wherever the vocabulary is declared

The input of a rule table names the values a vocabulary parameter accepts: the schema of the parameter carries
them as its `enum`, so the launcher offers a list to pick from instead of a text box. The schema used to be
generated from the Java class of the parameter, which for a vocabulary is its base type and says nothing about
the values.

The project is the one attached to the ticket: `Main` declares `Rules String Hello (MyList a)` and depends on
`Vocabularies`, which declares `Datatype MyList <String>` with the values `a`, `b` and `c`.

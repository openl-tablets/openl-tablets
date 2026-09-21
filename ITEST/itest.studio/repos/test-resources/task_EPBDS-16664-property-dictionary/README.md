# EPBDS-16664 — the properties a table may be given, and the dictionary they are read against

The properties panel offers a table the properties it may still be given, and the extended search narrows by a
property a table carries. Both used to read the dictionary without naming a table kind, which answered the
properties a *Properties table* declares for the tables of its scope — so `description`, `tags`, `id` and `active`,
which a table alone may carry, could neither be added nor searched by.

`010-setup` creates the project of the ticket, opened: `TestAddDeleteEditProperties.xlsx`, whose `MyRules2` declares
no properties and whose `MyRules1` declares one of nearly every kind.

`020-dictionary` — the dictionary is the same for every project:

- `010-a-table-may-be-given-what-no-properties-table-declares` — asked for a kind of table, the dictionary holds
  what that kind may declare on itself: `description`, `tags`, `id` and `active` among them. This is what the
  panel reads for the table on screen, the way the copy window does.
- `020-a-properties-table-declares-for-the-tables-of-its-scope` — asked for `Properties`, the dictionary holds what
  the contents of a Properties table may name, since such a table declares nothing on itself. This is what the
  Create Table window reads for one.

Asked for no kind, the dictionary holds every property a table may carry however it comes by it — written on it,
inherited from a Properties table or stamped by OpenL Studio — the deprecated ones aside, which is what the extended
search reads; `task_EPBDS-15409/150_ProjectMetadata/050-properties` pins that answer whole.

`030-details` — what the panel reads and writes:

- `010-a-table-with-no-properties-may-be-given-every-one-of-its-kind` — the details of `MyRules2` name its kind and
  list every property a Rules table may declare as one it may still be given.
- `020-a-table-says-the-properties-it-carries` — the details of `MyRules1` list what it declares, grouped the way
  the dictionary groups them, and nothing is left to give it.
- `030-a-property-is-written-from-the-panel` — `description` and `tags` are written onto `MyRules2`, the way the
  panel writes what the reader touched.
- `040-the-details-say-what-was-written` — read again, the details show the two under **Info** and no longer offer
  them.

`999-tierdown` closes the project, discarding the write, and deletes it.

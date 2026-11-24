# EPBDS-17273: the input schema requires the type of a nested datatype that has a subtype

The object mapper of a project writes the type of a datatype that has a parent or children into `@class` and reads
it back from there. The input schema of a table left the property out, so the Run and Trace forms created a nested
object of such a datatype without it, and the run refused the input with
`missing type id property '@class'`.

The project is the workbook attached to the ticket: `Base`, `Child extends Base`, `Holder` with the field
`Base base`, and `checkHolder(Holder h)` returning `ok`.

- `015-input-schema` — the schema of `Holder` describes its field `base` with `@class` required, the one value it
  takes as its `const`. `Holder` has no parent or children, so it has no `@class` itself.
- `020-run` — the input the form builds from that schema, `{"h": {"base": {"@class": "Base"}}}`, runs and answers
  `ok`.

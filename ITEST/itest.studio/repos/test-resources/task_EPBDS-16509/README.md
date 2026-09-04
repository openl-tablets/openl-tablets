# EPBDS-16509: Array-valued raw cells

This test verifies that the raw table API:

- Creates a new module from a Datatype table containing a `String[]` default with a trailing backslash and a comma
  and a `Date[]` default in raw API ISO form, then reads both unchanged arrays back.
- Round-trips string, context-parsed date-time, and enum arrays containing a null slot through a complete PUT while
  another cell changes.
- Updates a Test table's multi-valued `attributes` cell through cell, row, column, and range actions.
- Returns the same semantic arrays after each write, proving enum names and null positions are preserved,
  General-formatted dates use lossless ISO text, dates with an actual Excel format use that format, and strings use
  reversible OpenL array text.
- Rejects empty arrays, singleton-null arrays, blank string elements, nested arrays, and JSON objects with an
  actionable validation response.

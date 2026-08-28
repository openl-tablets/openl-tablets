# EPBDS-16514: Raw Datatype rows outside the bound model

This test verifies that the raw table API:

- Compiles a Datatype table before it is edited.
- Appends a two-row block containing a cross-row `rowspan`.
- Reads the expanded raw table with the new span and covered cell intact.
- Safely omits Datatype metadata for appended cells outside the previously bound logical body.

# EPBDS-16501: Batch append and insert spans

This test verifies that the raw table actions API:

- Rejects a span outside the projected append block without changing the table.
- Appends a `rowspan` that covers a later row from the same request.
- Appends a `colspan` that covers a later column from the same request.
- Inserts a `rowspan` block inside the table without losing the rows after it.
- Inserts a `colspan` block without changing the existing merge.
- Rejects spans from different batch lines when their merge regions overlap.
- Appends a line fully covered by a span declared on an earlier batch line.

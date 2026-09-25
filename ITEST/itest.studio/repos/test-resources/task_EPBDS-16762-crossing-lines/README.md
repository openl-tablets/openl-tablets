# EPBDS-16762 — a row and a column laid down together, holding their only value where they cross

The reader lays a column down between two others and a row down under the last one, and writes a value in the
one cell where the two meet. Neither line holds anything else. The write was refused:

```
A row or column cannot be entirely empty. A blank line would split the table and drop the data beyond it,
so leave at least one cell with a value.
```

There was no way round it. The value belongs to whichever of the two lines is written second, so the one
written first is blank at that moment whichever way round the pair is sent — both orders were refused.

The rule itself is right: a blank line inside a table ends it where it stands. What was wrong is what the rule
was asked of. `RawTableWriter` asked it of each line's own cells, so a line held by the table's own merge —
the header of a Data table, banked across every column — was refused for holding nothing of its own, even
though the merge grows over it and keeps it part of the table. The rule now asks whether anything holds the
line: a value of its own, a span declared earlier in the same request, or a merge the table already banks
across the place the line is laid down in. A blank line nothing reaches over is refused as before, and the
table the sequence ends with is checked for a blank line at save as it always was.

The project is created from the bundled **Tutorial 3** template, whose `addresses` table is the one the report
came from: a Data table whose header is banked across all its columns.

`020-crossing`

- `010-a-row-and-a-column-laid-down-together` — the reported request, verbatim: a column laid down at
  position 2 carrying nothing but the covered cell the header reaches over, and a row laid down under the last
  one whose only value stands in that column. `204` against the fixed code, `400` against the unfixed.
- `020-the-table-is-whole` — the table read back: seven columns with the header banked across all of them, the
  column laid down between `street2` and `city`, and the value standing where the two lines cross.

`999-tierdown` closes the project and deletes it.

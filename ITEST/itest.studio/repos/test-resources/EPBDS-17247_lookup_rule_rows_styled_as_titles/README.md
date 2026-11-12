# A lookup compiled with an error lays out its rule rows as rules

`StyleBroken.xlsx` is the workbook of the ticket. Its SmartLookup `CorporateRatingNewElement` returns an `Integer`, and
the cell `G16` holds `3.2`. Neither orientation compiles. The compiler also reads the table transposed, then drops that
reading and keeps the errors of the first one. The table used to keep the rows of the dropped reading, so the rows
`1`, `2` and `33` were laid out and saved as titles.

The raw read of the table with its styles carries what the Studio look draws (`regions`) and what the Excel file holds
(`styles`). Each scenario follows a step of the ticket. Format tables on save is on, as by default.

- **`020-open-with-the-error`** — the table is opened with the error. The title `financialRating` is an input title,
  and `LOW`, `HIGH` are output titles. The ratings `1`, `2`, `33`, `345` are values, and the grid under the risks is
  the output.
- **`030-save-the-fixed-value`** — `3.2` becomes `4` and is saved. The rows `1`, `2`, `33` are written as rules, not
  titles, and the cell `4` of `LOW` keeps its fill.
- **`040-save-the-error-and-copy`** — `4` becomes `3.2` again and is saved, and the table is copied under another
  name. The table and its copy are laid out and written with their rule rows as rules, the error in place.

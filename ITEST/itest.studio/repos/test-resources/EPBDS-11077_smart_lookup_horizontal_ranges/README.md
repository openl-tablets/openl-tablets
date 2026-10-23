# Smart Lookup horizontal range editors

The project contains the original `Range_cell (1).xlsx` workbook from EPBDS-11077 without modifying its bytes.
The archive stores it as `Ranges.xlsx` and adds a minimal `rules.xml` descriptor. The table has two horizontal
numeric range conditions and one vertical numeric range condition.

The Editors API must declare range editors for all three conditions. The raw table API must also report
`editor: range` for cells containing the horizontal range expressions, even when those cells have usage metadata.

Setup captures the project ID from the upload response and the table ID from the table list response.

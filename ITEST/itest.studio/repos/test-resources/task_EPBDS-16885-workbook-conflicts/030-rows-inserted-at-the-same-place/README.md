# Different rows inserted at the same place

Both branches insert a row between US and DE in `rate`: FR on the side branch, IT on master. The table is a
`rows` conflict, and each version marks the row its branch inserted. Discarding the changes of both branches
answers the merged workbook.

This is the expected behaviour, not a defect.

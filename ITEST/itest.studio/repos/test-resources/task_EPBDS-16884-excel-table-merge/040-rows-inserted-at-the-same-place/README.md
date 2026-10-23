# Different rows inserted at the same place

Both branches insert a row between US and DE in `rate`: FR on the side branch, IT on master. Each branch grows the
table into the empty rows under it. Sync keeps both rows, IT of master first since the side branch is merged into
master, and the merge succeeds. Holding both rows, `rate` would touch `minAge`, so `minAge` moves a row down.

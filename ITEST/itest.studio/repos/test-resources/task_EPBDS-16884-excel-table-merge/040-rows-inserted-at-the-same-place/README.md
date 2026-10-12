# Different rows inserted at the same place

Both branches insert a row between US and DE in `rate`: FR on the side branch, IT on master. Only a person
can tell their order, so the merge answers `conflicts`. Cancelling the merge leaves master as it was.

This is the expected behaviour, not a defect.

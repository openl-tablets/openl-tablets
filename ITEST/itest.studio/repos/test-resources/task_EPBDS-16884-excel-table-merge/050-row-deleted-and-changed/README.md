# Row deleted in one branch and changed in the other

The side branch deletes DE from `rate`, master changes the rate of DE. Keeping either change loses the
other, so the merge answers `conflicts`. Cancelling the merge leaves master as it was.

This is the expected behaviour, not a defect.

# Row inserted next to a row the other branch deletes

Master inserts IT between US and DE in `rate`, and the side branch deletes DE. IT was meant to stand before DE, so
only a person can tell where it goes: the table is a `rows` conflict, and your version marks the row master
inserted. Such a table has no cell of one version facing a cell of the other, so it is not merged cell by cell, and
a decision to merge it so is refused. Discarding the changes of both branches answers the merged workbook.

This is the expected behaviour, not a defect.

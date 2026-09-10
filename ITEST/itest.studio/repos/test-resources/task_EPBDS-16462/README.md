# EPBDS-16462 — compare a merge conflict with a deleted file

The scenario creates a two-module project and branches it. The side branch modifies `Main.xlsx`, while the main
branch deletes that file. Receiving the side branch therefore creates a modify/delete merge conflict.

The conflicted-file REST endpoint returns the existing side as a workbook download and reports the deleted side as
`404 Not Found`. The conflict-details response reports availability for each file and side, so the UI does not request
the missing download and shows a deleted status even when one conflict contains files with different availability.

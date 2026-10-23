# Changes Made Outside OpenL Studio

OpenL Studio is not the only writer of the files of a project. An IDE, Excel, a build or another client of OpenL
Studio can change a file that OpenL Studio holds compiled. This page tells how OpenL Studio keeps from overwriting
such a change.

## A Workbook Is Never Saved Over a Newer File

A compiled module holds its workbooks in memory, and a save writes the whole workbook from memory. Saving over a file
changed on disk since the workbook was read would lose that change, so the save is refused instead.

- **What the workbook remembers.** `XlsWorkbookSourceCodeModule` remembers the size and the modification time of its
  file as the compilation is about to read it. Its loader reads the workbook from one snapshot of the file: the whole
  content read at once, and read again when the size or the time changes under the read. The workbook remembers a
  digest of that content. A workbook read from another file than the compiled one, because another program wrote
  the file in between, remembers that instead. A workbook unloaded from memory and read again is read from another
  file too when its content differs from the digest remembered, even when another program kept the size and the time
  of the file. After every save it remembers the file it wrote.
- **What a save compares.** Before it writes anything, a save compares the size and the time of the file. It then
  writes the whole workbook into a hidden file beside it, and compares the file once more right before replacing it:
  by the digest of its content, then by its size and time. A file changed since, and a workbook read from another
  file, are not written: the save throws `WorkbookChangedOutsideException`, and the file stays as the other program
  left it. A file deleted since the read is written again.
- **Whole or nothing.** The written file takes the place of the workbook's file in one step (`FileReplacement` in
  `org.openl.commons`). Excel, an IDE or OpenL Rule Services reading the file meet it as it was or as it is saved,
  never half written, and a save that fails, out of memory or out of disk space, leaves the file as it was. The file
  keeps its POSIX permissions, but it belongs to the user OpenL Studio runs as. A file reached through a symbolic link
  is replaced where the link points. A file that cannot be replaced by another file — in a folder OpenL Studio cannot
  create a file in, or mounted into a container on its own — is written over in place instead, and may be met half
  written. The hidden files a stopped save left behind are removed by a later save of the same file. A hidden file
  that cannot be removed once the save is over is only logged: the outcome of the save stands.
- **What the client gets.** The REST API answers `409` `workbook.changed.outside.message`, naming the file. A refused
  write has the session build the module from its workbooks again (`WorkspaceProjectService.writing`), so the next
  read shows the file as it is now, and the edit is repeated on it.
- **What it costs.** A compilation reads the size and the time of each workbook, nothing more. A workbook loaded for
  a write is read once, and its digest comes from that read. A save hashes the workbook as it writes it, and reads the
  workbook's file once, a part at a time, to compare it. OpenL Rule Services never writes a workbook, so it never
  reads a digest.
- **To improve.** A workbook is loaded from a copy of its whole file held in memory, the snapshot its digest comes
  from, so loading a workbook takes about twice the memory POI needs for it. A way to tell that the file stayed as it
  was while POI read it, without holding a copy of it, is still to be found.
- **What it does not tell.** A change made between the last comparison and the replacement, a matter of
  microseconds, is overwritten. So is a change made between the compilation and the read of the workbook that keeps
  both the size and the modification time of the file.

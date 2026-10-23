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
  write has the session build the module from its workbooks again and compile it at once
  (`WorkspaceProjectService.writing`), so the next read shows the file as it is now, the edit is repeated on it, and
  the status goes on telling how the module compiles and what changes on disk. A write refused by the version of a
  table (`412`) has changed nothing, and builds nothing again.
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
- **A file held open.** Excel on Windows keeps the workbooks it shows from being replaced, and a read-only file is not
  replaced either. A program that opens the file for a moment, such as an indexer or an antivirus, is waited out. The
  save throws `FileLockedException` (`org.openl.commons`), the REST API answers `423` `file.locked.message`, naming the file, and the
  session builds the module again as for any refused write.
- **Any file held open.** A write, a copy or a deletion of a file in a repository on the file system
  (`FileSystemRepository`, the workspace included) that the file system refuses for an existing file throws
  `FileLockedException`. The files API and every other endpoint answer `423` `file.locked.message`, naming the file,
  instead of a failure that does not tell why.
- **Repository files are written whole.** `FileSystemRepository` writes and copies a file through `FileReplacement`
  as well, so a file written through the files API, a project saved into a repository on the file system and a file
  of the workspace are never half written either. A copy keeps the time its source was modified. A read-only file is
  not replaced. The hidden files of the writes are not listed as files of a project.

## A Write Names the Version It Is Based On

A client sends a table or a file it built from an earlier read. When the table or the file changed since, on disk or
through another client, the write would overwrite that change even when OpenL Studio holds the newest content. So a
write names the version it is based on, and a write over another version is refused.

- **Tables.** The version of a table is a digest of the text of its cells (`TableSyntaxNode.getVersion()`, read through
  `IOpenLTable.getVersion()`). A compiled table reads its cells from the workbook as the module was compiled, so the
  version is computed once and changes with a new compilation. A written table takes a new version at once
  (`TableSyntaxNode.markWritten()`), as a module is not always compiled again after a write. A read answers the version
  in the `ETag` header, and the body carries none. The table writes — a replacement, an append, an edit of the source, a
  write of the properties, a deletion — take it in `If-Match`, and `WorkspaceProjectService` compares it once the
  write's turn in the queue of the session has come, before anything is written. So of two writes based on one read, the
  second is compared with what the first left. Another version answers `412 Precondition Failed`
  `table.changed.since.read.message`.
- **Files.** The version of a file is a digest of its content (`ProjectFilesService.getVersion`). A read of a file and
  of its metadata answer it in `ETag`, and a replacement and a deletion through the files API take it in `If-Match`;
  so does an upload link that replaces a file (`expectedVersion` of the link). The writes based on a version run one
  at a time, from the comparison to the end of the write, so two of them based on one read never both pass. Another
  version answers `412 Precondition Failed` `file.changed.since.read.message`.
- **HTTP semantics.** The versions follow [`ETag`](https://www.rfc-editor.org/rfc/rfc9110#section-8.8.3) and
  [`If-Match`](https://www.rfc-editor.org/rfc/rfc9110#section-13.1.1) of RFC 9110 (see also MDN on
  [`ETag`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/ETag) and
  [`If-Match`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/If-Match)). A tag is strong and
  quoted, and a write is made only when the tag it names equals the current one, as the [strong
  comparison](https://www.rfc-editor.org/rfc/rfc9110#section-8.8.3.2) defines it; a weak tag matches none. A list of
  several tags matches none either, while RFC 9110 lets it match when one of them is current: no client of OpenL Studio
  sends one. Another version answers [`412 Precondition
  Failed`](https://www.rfc-editor.org/rfc/rfc9110#section-15.5.13). `*` matches any version: a write of a table or a
  file that is not there answers `404`, as the conditions are [left
  out](https://www.rfc-editor.org/rfc/rfc9110#section-13.2.1) of a request answered otherwise without them. A write
  whose version is current answers as without the header, and its answer names no new tag: what is written is formatted
  on the way, so the client reads it again for the next version. The tag of a table is the version of the table, not of
  one view of it: every read of the table, whole, in a window or by fields, answers the same tag. The MCP server answers
  the tag to the agent as the `version` of its table tools, beside the table, and names it back in `If-Match`. `409`
  stays for a state that refuses the write whatever the client read, such as a workbook another program changed since
  OpenL Studio read it.
- **Optional for REST clients.** A write without `If-Match` is made over any version, so the existing integrations keep
  working; `428 Precondition Required` is not answered. The editor of OpenL Studio names the version of the table it
  read in every write it makes, the properties included unless the cells were written first in the same save; it changes
  no file by its content. The MCP server always names one, as an agent may write from a read long past: its table tools
  require the version `openl_get_table` or the previous edit answered, and its file tools the version
  `openl_read_project_file` answered to replace or delete a file. Only a table or a file it creates names none.
- **What it does not tell.** Another program that writes a file between the comparison and the write is not seen by
  the comparison; a workbook saved by OpenL Studio is still checked by the save itself. While automatic compilation is
  off, a written module is not compiled again, so what a read shows stays as it was compiled until the module is
  verified, while a written table answers its new version.
- **To improve.** The version of a file is a digest of its whole content, so a read of a file reads it twice, once
  for `ETag` and once to send it, and a read of the metadata of a file reads the whole file. A version that costs no
  read of the content, such as one kept with the file or the revision of a repository that has one, is still to be
  found.

## Projects Edited in Place

In single-user mode the one user works on the folder of their projects, often in an IDE and in Excel at the same
time. A copy of each project in the workspace would have to be opened, saved and closed, and kept in step with the
folder. So the projects of a design repository on the file system are edited where they are instead.

- **When.** `LocalWorkspaceManagerImpl` turns it on for `user.mode = single`. A design repository qualifies when it
  unwraps — through `PathCheckedRepository`, the secured and the mapped wrappers — to a `FileSystemRepository` that
  says it is local (`repo-file`). Every other repository is copied into the workspace as before.
- **Listing.** `LocalWorkspaceImpl` lists every project of such a repository as a project of the workspace, read
  through an `InPlaceRepository`: a `LocalRepository` rooted at the folder of the design repository, whose paths are
  the paths there. `getRepository(id)` still answers the repository of the workspace, so a copy opened later goes
  there. A copy opened before stays while the registry marks it as changed, so that it can be saved; a copy without
  changes gives way to the folder.
- **State.** `RulesProject.isInPlace()` tells such a project. It is opened, never modified and has no local changes;
  `open`, `openVersion`, `save` and `close` change nothing on disk, and closing it through the API answers `409`
  `project.close.in-place.message`. Nothing is recorded about it in the metainfo registry of the user: the hooks of
  `LocalRepository` (`noteModified`, `forgetProject`, `uniqueIdOf`, `baselineOf`) are left out.
- **Name and folder.** The project is named by its `rules.xml`, as a closed project is, while its folder keeps the
  name it has. `WebStudio` finds the project of a descriptor by the folder and then by the name the descriptor
  declares, takes the project found only when its path ends the folder of the descriptor (in a repository of nested
  folders, the folder name of one project may be the name of another), and resolves the descriptor in the folder of
  the project rather than in the workspace.
- **History.** The local edit history of a module stays in the workspace
  (`{userId}/.history/.in-place/{repository id}/{folder}`, apart from the history of a copy in the workspace with the
  same folder), and a save of the project keeps it, so an edit is rolled back from it. Before a workbook is written
  and before a version is restored, `ProjectHistoryService.save` keeps the file as it is on disk when it differs
  from the version kept last, so the version another program wrote is never lost to the session. This holds for a
  copy in the workspace too. The file and the version are compared as they are read, so a big workbook is never held in memory for it, and a file
  that kept its size and modification time since the history took it is not compared again. A restore
  writes the file whole, as a save does, and has the session compile the project again when the project was
  compiled from the restored workbook (`ProjectModel.isCompiledFrom`), so a restore is never taken for a change
  another program made.
- **Telling the reader.** `XlsWorkbookSourceCodeModule.isChangedOnDisk()` compares the size and the modification time
  of the file with the ones it remembers, and tells a workbook read from another file than the compiled one, such as
  one read again from content another program changed keeping the size and the time. `ProjectModel.isChangedOnDisk()`
  asks every workbook compiled for the open project. A workbook added to the modules of the project, one taken away,
  and a changed `rules.xml` count too:
  the model remembers the descriptor and the workbooks of the modules it compiled, and `checkProjectFiles()` reads the
  descriptor again when the repository watcher reports a change, since that costs a read of the folder. The status of
  the project answers it as `changedOnDisk`, and a change the watcher reports publishes the status on the channel of
  the project. The status in short,
  `GET /projects/{projectId}?include=status&fields=compileStatus(compileState,changedOnDisk)`, is cheap enough to ask
  before every call: the project builds no field `fields` leaves out, neither the projects using it nor the messages
  resolved to their tables.
- **Refreshing.** The editor never reads the files again by itself, as the reader may be in the middle of an edit: it
  shows a warning with **Refresh**, which compiles the module again with `reset=true`. The MCP server needs no call of
  the agent: before every tool that works with what Studio compiled (`compiled` in `registerTool`) it asks the short
  status, and compiles the project again when the files changed, waiting until the compilation ends: Studio drops
  what it compiled as the compilation starts, and the tables of a module a workbook added come with its end. The
  tool then goes on with the files as they are: an edit names the version of the table it is based on, so an edit of
  a table changed since is refused, and a table created or an edit of another table of the workbook is written beside
  the change. A write the workbook refuses as
  changed outside (`409`) asks a person to refresh the project; the MCP server tells the agent to read the table again
  instead, as the session builds the module from its workbooks again after the refusal and no refresh call is needed.
- **Safety.** A write of a workbook is refused when another program changed the file since it was read (above), and
  a write of a table or a file based on an outdated read is refused by its version.

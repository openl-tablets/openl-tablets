# Projects Edited in Place

In single-user mode the projects of a design repository on the file system are edited where they are. The suite
puts a project into the folder of the `design-file` repository through the repository files API, as an IDE would,
and works with it through the projects API.

- The project is open at once and is never closed: closing it is refused.
- A table write names the version of the table it read: another version is refused, and the edit lands in the file.
- Another program changes the workbook: the project status tells it, and a write over the change is refused. The
  module is compiled from the files at once after the refusal, so the status goes on telling what the files hold.
- The project declares no module in `rules.xml`, so every workbook under `rules/` and `tests/` is a module of its
  own. A workbook another program adds there shows in the status in short, the compile state and the change on disk
  alone (`GET /rest/projects/{projectId}?include=status&fields=compileStatus(compileState,changedOnDisk)`), and its
  tables are read once the project is compiled again.
- A test written in another module of the project names the project it is written in, so the editor can open it:
  the folder of the project, `greetings`, is not its name.

# Projects Edited in Place

In single-user mode the projects of a design repository on the file system are edited where they are. The suite
puts a project into the folder of the `design-file` repository through the repository files API, as an IDE would,
and works with it through the projects API.

- The project is open at once and is never closed: closing it is refused.
- A table write names the version of the table it read: another version is refused, and the edit lands in the file.

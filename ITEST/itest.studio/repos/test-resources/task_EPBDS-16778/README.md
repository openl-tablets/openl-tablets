# EPBDS-16778 — single-file writes into a closed project

A project created from a ZIP archive stays closed, so the project files API commits every change to it straight to
the design repository. The admin commits the project; a contributor then writes it, file by file.

Every commit of the contributor must carry the contributor as its author and a message naming the write, whoever
changed the file before:

- `020-create-file` — a new file (`POST`) is committed as `Save readme.txt`, not refused with 409;
- `030-overwrite` — an overwritten file (`PUT`) is committed as `Save data.txt`, not as the admin with the message of
  the admin's commit;
- `040-copy-and-move` — a copy, and a move (a copy and a deletion), are committed the same way;
- `050-delete-module` — deleting a module is committed the same way, and so is the `rules.xml` that no longer
  declares it;
- `060-project` — the project stays closed and nobody holds it.

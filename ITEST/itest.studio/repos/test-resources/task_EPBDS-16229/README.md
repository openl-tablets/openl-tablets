# EPBDS-16229 — a project renamed in `rules.xml` but not saved stays addressable by its id

A project opened in the workspace is renamed by editing `rules.xml`, without saving the project. The workspace copy
moves to a folder named after the new name, while the design repository still holds the old folder — the folder the
project id names.

The test requires the id the listing hands out to keep working:

- `GET /projects` returns the new business name under the unchanged id;
- `GET /projects/{id}` and `GET /projects/{id}/status` answer for that id;
- `DELETE /projects/{id}` removes the project instead of answering
  `openl.error.404.project.identifier.message`.

The delete runs in `999-tierdown`, so the assertion and the cleanup of the shared `design-flat` repository belong to
the same teardown phase: `010-delete-project` removes the project, `020-verify-deleted` requires the id to answer
404 afterwards, and `030-verify-repository-clean` requires the repository to hold nothing under that name. That
closing listing asks for deleted projects too, so an archived leftover is not read as a clean repository.

# EPBDS-16432 — the history of a project renamed in `rules.xml` but not saved

A project opened in the workspace is renamed by editing `rules.xml`, without saving the project. From then on the
user sees it under the new name, while the repository still holds it under the name it was published with.

The Revisions tab used to ask the design repository for the history of the displayed name and was answered 404,
because that name exists nowhere but in the user's workspace. It now asks by project id, which the rename does not
move:

- `010-history-by-id` — `GET /projects/{id}/history` returns the history of the renamed project;
- `020-history-by-published-name` — the design repository still answers for the name the project was published
  under, with the same revisions. This step is the control for the one below, so it asserts the revisions rather
  than the status alone: otherwise a repository that answered with nothing would keep both steps green;
- `030-history-by-displayed-name` — that same repository knows nothing of the new name. This is why the name is not
  what a workspace screen may address the history by, and it is the request that used to back the Revisions tab.

`999-tierdown` deletes the project and requires the shared `design-flat` repository to hold nothing under that name
afterwards. The closing listing asks for deleted projects too, so an archived leftover is not read as a clean
repository.

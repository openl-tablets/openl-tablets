# EPBDS-16380 — a project outside the base branch is represented by a protected branch

`design-flat` protects `release-*`. The project is created in the ticket branch `EPBDS-16380-fix` and the release
branch is then created from it, so both branches hold the project, neither is the repository base branch, and the
two share one tip commit. Under the previous rule — newest tip, then branch name — the ticket branch won both keys;
the protected branch must win now.

- `020-home-branch/010-another-user-sees-the-protected-branch` — the project list shows the project on
  `release-EPBDS-16380`;
- `020-home-branch/020-both-branches-keep-the-project` — ranking decides only which branch represents the project:
  both branches still hold it, and the release branch is reported as protected.

The listing is read by a viewer rather than by the admin who created the project, and with
`Cookie: NO_JSESSIONID=noAuth` so that the request authenticates as that viewer instead of reusing the session of
the preceding setup steps. The branch shown for a project is the *effective* branch, and the workspace keeps the
branch a user last saw as a durable preference: the creator therefore stays on the creation branch, which would
hide the home branch this suite is about.

The project is created in the ticket branch, not in the release branch, because writing to a protected branch is
refused. Teardown deletes the ticket branch, then the release branch with the protected-branch bypass enabled —
that branch is the last one holding the project, so deleting it removes the project, which `060-project-is-gone`
verifies.

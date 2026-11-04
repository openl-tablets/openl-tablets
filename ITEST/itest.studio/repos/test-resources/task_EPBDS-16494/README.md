# Project local history API

This suite verifies that the project-scoped local history endpoint:

- works in a fresh HTTP session for an opened project;
- defaults to the project's first module and honors an explicit module name;
- does not leak the current project or module from another request;
- restores only the project and module named in the request;
- leaves a project not modified once a restore brings its module back to the revision it was saved at, so that it
  closes without discarding changes, and modified once a restore makes its module differ from that revision
  (EPBDS-17086);
- clears only the project named in the request while the administrator operation clears every workspace;
- rejects an unknown module;
- replaces the removed `/history/project`, `/history/restore`, and `/history` endpoints.

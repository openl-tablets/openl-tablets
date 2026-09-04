# EPBDS-14589 — a deployment keeps the content of the project it deploys

A project of a database repository is stored as an archive, and it is deployed into a repository of folders —
git here, a local folder next to it — by streaming that archive. The deployment must hold the files of the
project, not empty ones.

The revision in the design repository is what proves it: a deployed project is recognized by the content it
was built from, so the revision is found only while the deployed files are the files of the design revision.

# EPBDS-16269 — resolve conflicts after renaming a historical project revision

The declarative scenario creates a project, renames and saves it, reads the initial revision from the project history,
then opens and renames that revision. Saving the historical revision conflicts with the already published rename.

The project ID obtained after the first rename must remain usable while the second rename is unsaved. The save returns
the expected conflict response, and three consecutive `GET /projects/{id}/merge/conflicts` requests expose the
`rules.xml` conflict. These are the probe, modal initialization and conflict-step loading requests OpenL Studio makes
while opening the **Resolve Conflicts** dialog.

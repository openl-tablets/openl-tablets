# Workspace Copy Reconciliation

These scenarios verify the final workspace state for opened and in-editing project copies after their
source changes outside OpenL Studio:

- When OpenL Studio no longer identifies a project as existing, its copy is removed together with local
  changes and its edit lock.
- Restoring the project does not restore the removed copy; the project is listed as closed.
- When the project still exists but the branch used by its copy no longer contains it, the copy is closed
  and the project remains listed.

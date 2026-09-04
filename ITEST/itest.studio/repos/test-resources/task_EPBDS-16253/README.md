# Workspace Copy Reconciliation

These scenarios verify workspace state for opened and editing project copies after their source changes
outside OpenL Studio:

- Deleting a project from the main branch keeps it listed while the copy branch still contains it.
- The opened and editing states stay attached to that surviving branch, including unsaved local changes.
- Deleting the last containing Git branch removes the logical project and its workspace copy.

# EPBDS-16258: Preserve the branch selected for a closed project

This test verifies that a repository refresh does not reset the branch selected for a closed project.

Creating another branch triggers a project index refresh and waits for its completion. The closed project must remain
on the branch selected before that refresh.

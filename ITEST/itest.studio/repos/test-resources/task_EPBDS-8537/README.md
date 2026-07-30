# EPBDS-8537 Cross-Branch Project Creation

This suite verifies that:

- a contributor can create a project in a valid branch that does not exist yet;
- creation returns only after the cross-branch index publishes the new project;
- a different user with repository read access lists the project without selecting its branch first;
- the created branch is both the project's only membership and its home branch;
- a project stored under a custom mapped path can be copied by business name into a new branch;
- the copied project is published to another user with its source content intact.

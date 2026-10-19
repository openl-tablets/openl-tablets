# EPBDS-16328 Copying a Project Whose Name Another Folder Carries

A non-flat repository tells its projects apart by the folder they live in, so more than one folder may
carry the same project name. This suite verifies that:

- a copy whose source is named by its name, which the repository carries in more than one folder, is refused:
  a name names no project, so the source is not found;
- a copy that still names the repository of its source in `sourceRepositoryId` is refused as an unknown field:
  the identifier names the repository too;
- a copy whose source is addressed by its identifier is made from that folder — the copied descriptor is
  the source one, not the twin one that declares its own rules root.

> [!Note]
> The suite addresses its projects by identifier. An identifier is a hash of the repository and the folder
> the project lives in, so changing a fixture path changes it. `itest.env` holds the ones a URL carries;
> `020-copy/015-copy-naming-the-source-repository-is-refused.req` and `020-copy/020-copy-by-id.req` repeat
> `SOURCE_ID` in their bodies, because the harness substitutes variables in the URL path and the headers only.
> Regenerate them together.

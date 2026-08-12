# EPBDS-16328 Copying a Project Whose Name Another Folder Carries

A non-flat repository tells its projects apart by the folder they live in, so more than one folder may
carry the same project name. This suite verifies that:

- a copy whose source is named, and whose name the repository carries in more than one folder, is refused
  and the refusal names the identifiers to choose from;
- a copy whose source is addressed by its identifier is made from that folder — the copied descriptor is
  the source one, not the twin one that declares its own rules root.

> [!Note]
> The suite addresses its projects by identifier. An identifier is the repository, the project name and
> the hash of the folder the project lives in, so changing a fixture name or path changes it. `itest.env`
> holds the ones a URL carries; `020-copy/020-copy-by-id.req` repeats `SOURCE_ID` in its body, because the
> harness substitutes variables in the URL path and the headers only. Regenerate both together.

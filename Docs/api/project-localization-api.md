# Project Localization API

The Project Localization API works with the localization messages of a project: the `message*.properties` files of its
`i18n` folder, which the `msg` function of the rules reads. The checks of the files are described in the Reference
Guide, in
[Checking Localization](../user-guides/reference-guide/04-working-with-projects/04-project-localization.md#checking-localization).

## Problems of the Files

```http
GET /rest/projects/{projectId}/localization/problems?positions=file
```

Checks the localization files without compiling the project and lists the warnings as the compilation status lists
them, each with a `location` of the `type` `localization`: the `path` of the file, the `line`, the `key` and the
`language`. A warning about the whole file, such as the keys a language leaves untranslated, has no key.

- **positions=file** (default) — adds `places`, the places of the file each warning is in, as `line` and `endLine`
  from 1 and `column` and `endColumn` from 0, so an editor of the file marks them.
- **positions=text** — adds `spots`, the pieces of the text of the key, counted from its first character.

A project nobody opened has no files in the workspace to read: the answer is `409` with
`openl.error.409.project.not.opened.message`, not an empty list, which would read as files without problems.

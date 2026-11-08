# Project Localization API

The Project Localization API works with the localization messages of a project: the `message*.properties` files of its
`i18n` folder, which the `msg` function of the rules reads. The checks of the files are described in the Reference
Guide, in
[Checking Localization](../user-guides/reference-guide/04-working-with-projects/04-project-localization.md#checking-localization).

It reads the messages by pages of keys and changes them in one write: the **Localization** screen of OpenL Studio works
through it.

## Rules for Every Endpoint

- **Opened project** — the project is opened first with `PATCH /rest/projects/{projectId}` and `{"status": "OPENED"}`.
  Otherwise the answer is `409` with `openl.error.409.project.not.opened.message`.
- **Language** — a language is named as the file name names it: a language of two or three letters, then the country
  and the variant, each after an underscore, such as `de` for `message_de.properties` or `de_LU` for
  `message_de_LU.properties`, and an empty name for the default file `message.properties`. A change writes a language
  only as the engine opens its file: the language in lower case and the country in upper case or as three digits, so a
  file is read on a file system that tells the cases apart. A name that is not such a tag answers `400` with
  `openl.error.400.localization.language.invalid.message`, and a file whose name holds no such tag, such as
  `message_backup.properties`, is no localization file.
- **Read once** — the session reads the files once and keeps them. A request reads a file again only when its size or
  the time of its last change differs from the kept ones, so a file written by anything, a sync or a revision opened
  included, is read anew. A write through the files API of the project, and a change by this API, drop the kept file.
  Closing the project drops all of them.

## Keys of the Tree

```http
GET /rest/projects/{projectId}/localization/keys
```

Every key of the project in the order the files write them: the keys of the default file first, then the keys the
other files add. Each key is an object, so a node can carry more facts later.

```json
[
  { "key": "greeting" },
  { "key": "total" }
]
```

## Pages of Messages

```http
GET /rest/projects/{projectId}/localization?search={text}&problems=true&untranslated={tag}&sort=asc&page=0&size=50
```

A page of the keys with their texts in every language, in the shape of [Pages of Results](README.md#pages-of-results),
50 keys by default. Every page carries the languages of the project.

- **search** — keeps the keys whose name or text in any language contains the text, in any case.
- **problems** — `true` keeps the keys the check of the files warns about.
- **untranslated** — keeps the keys this language shows the default text for, having no text of its own, of its parent
  language, or of a project the project depends on. Repeated, it keeps the keys each of the languages shows the default
  text for.
- **sort** — `asc` or `desc` orders the keys by name, regardless of case. Without it the keys come in the order the
  files write them.
- **key** — answers this key alone, whatever the other filters. A key only a project the project depends on has is
  answered with the text of that project, and a key no project has answers an empty page.

```json
{
  "content": [
    {
      "key": "greeting",
      "values": { "": "Hello, {0}!", "fr": "Bonjour, {0} !" },
      "shared": { "de": { "value": "Hallo, {0}!", "project": "Base", "projectId": "7H2KQ0M4ZT9RB" } },
      "warnings": [
        {
          "language": "fr",
          "summary": "Placeholders [] differ from [0] of the default language.",
          "spots": [ { "start": 0, "end": 7 } ]
        }
      ]
    }
  ],
  "pageNumber": 0,
  "pageSize": 50,
  "numberOfElements": 1,
  "total": 1,
  "languages": [ { "tag": "" }, { "tag": "fr", "legacy": true } ]
}
```

- **values** — the text of the key by the language that writes it. A language missing here does not write the key.
  `null` stands for a key written on a line without a separator, which has no text: a rule gets the key itself.
- **shared** — the texts the projects the project depends on give the key, by the language, with the name of the
  project and its id when the workspace holds the project. Absent when none of them has the key.
- **warnings** — what the check of the files warns about the texts of the key, by the language, with the pieces of the
  text a warning is about in `spots`, counted from the first character of the text. Absent when there are none.
- **languages** — the languages of the project, the default one first. `legacy: true` marks a file not written in
  UTF-8: it is read in Windows-1252, and its first change writes it whole in UTF-8. Absent for a project without
  localization files.

## Changing Messages

```http
POST /rest/projects/{projectId}/localization/actions
Content-Type: application/json

{
  "actions": [
    { "operation": "setValue", "key": "greeting", "language": "fr", "value": "Salut, {0} !" },
    { "operation": "removeValue", "key": "greeting", "language": "de" },
    { "operation": "addKey", "key": "note", "values": { "": "A note", "fr": "Une note" } },
    { "operation": "removeKey", "key": "farewell" },
    { "operation": "addLanguage", "language": "it", "values": { "greeting": "Ciao, {0}!" } }
  ]
}
```

Makes the changes in the order they are given, then writes every file they changed at once, and answers
`204 No Content`.

- **setValue** — writes the text of a key in a language: over the key where the file holds it, or at the end of the
  file.
- **removeValue** — takes the text of the key out of the file of the language, so the language takes the text it falls
  back to.
- **addKey** — adds a key no language has, at the end of the file of each language given.
- **removeKey** — takes the key out of the file of every language.
- **addLanguage** — creates the file of a language with the texts given, in their order, or empty.

**How the files are written:**

- **Only what changed** — the lines of a changed key are written anew, a new key is added at the end of the file with
  the line break of the file, and a key taken out leaves no line behind, its continued lines included. Comments, blank
  lines and the other keys stay as they are written.
- **As the file writes them** — a key written anew keeps the separator it is written with, such as ` = ` or `: `. A
  new key, and a key written without a separator, take the separator of the last key of the file written with one; a
  new file takes `=`.
- **All or nothing** — a change that is refused refuses them all, and no file is written. Each file is written beside
  itself first and takes its place only once every file is written, so a failure leaves the files as they were.
- **One at a time** — the changes of a project are written one at a time, whichever session sends them. A file another
  program or session changed since the session read it refuses the changes with `409`; the next request reads it anew.
- **After the write** — the project is marked as modified and stays in the workspace until it is saved, and the
  compiled project reads the new texts at once and checks the files again, without compiling anything.

**Refusals:**

- **`400`** — `openl.error.400.localization.language.invalid.message`: a language that is not a tag.
- **`404`** — `openl.error.404.localization.key.not-found.message` or
  `openl.error.404.localization.language.not-found.message`: no file of the project has the key, or the project has no
  file of the language.
- **`409`** — `openl.error.409.localization.key.exists.message` for a key added that a language has,
  `openl.error.409.localization.language.exists.message` for a language added that the project has,
  `openl.error.409.localization.file.changed.message` for a file changed since it was read, and
  `openl.error.409.project.locked.message` for a project another user edits.

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

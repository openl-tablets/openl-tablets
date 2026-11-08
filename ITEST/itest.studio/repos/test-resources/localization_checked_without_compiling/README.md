# The localization files are checked without compiling the project

OpenL Studio checks the `message*.properties` files of the `i18n` folder of a project as the first step of its
compilation, before any module, and again in place whenever they are written: a module does not read them when it is
compiled. The calls of `msg` in the rules are checked against them as each module compiles, and their warnings belong
to the tables the calls are written in.

`localization.zip` is a project written for this: an empty `Main.xlsx` and three localization files.

- `message.properties` writes `greeting` twice, leaves the apostrophe of `note` open, and has a line without a
  separator.
- `message_fr.properties` translates `greeting` with another placeholder, and writes `extra`, which the default
  language lacks.
- `message_zh_TW.properties` writes two full-width commas in the placeholder of `total`.

`010-setup` creates the project and opens it.

`020-the-files-are-checked-without-compiling`

- `010-the-problems-of-the-files` — each problem of the files, with the file, the line, the key, the language and the
  pieces of the text it is about; two full-width commas are one warning with both pieces.
- `015-the-key-written-twice` — `greeting` with its warnings by the language: the default language, an empty name,
  warns that the key is written twice, and French that its placeholders differ.
- `020-nothing-is-compiled` — the session compiled nothing for it.
- `025-the-status-lists-the-warnings-of-the-files` — the status lists the warnings about the files all the same, the
  way the panel of the problems shows them before any compilation.
- `030-a-write-of-the-files-compiles-nothing` — writes `message.properties` of the folder of the scenario, the text
  the project already holds.
- `040-still-nothing-is-compiled` — the write compiled nothing either.

`030-the-compilation-checks-them-first-and-the-calls-of-msg`

- `010-a-table-calling-msg` — writes the Spreadsheet `Greet`, whose steps call `msg`: with as many values as the text
  takes, with one too many, with a key no file has, and with a number for `total`.
- `020-compile` — compiles `Main`.
- `030-the-status` — the files count as a step of their own, before the module: 2 of 2. The warnings about the files
  come first, then those about the calls.
- `040-the-table-tells-its-calls` — the table lists the warnings about its calls, each on the cell of the call.
- `050-its-cells-lead-to-the-keys` — each key a call writes is marked in its cell, with its default text, or with the
  fact that no default file has it.

`040-a-write-of-the-files-is-checked-in-place`

- `010-write-the-default-file` — writes `message.properties` again, `message.properties` of the folder of the
  scenario: `greeting` takes two values now, `farewell` is added, and the apostrophe of `note` is doubled.
- `020-the-status-without-compiling` — the warnings follow the files at once, nothing compiled again: the calls of
  `greeting` are checked against its new text, and `farewell` is found.
- `030-the-problems-of-the-files` — the problems of the files as they are now.

`050-the-messages-are-served-by-pages-and-changed-by-actions` — the messages of the open project, read once by the
model of the session, served by pages and written by actions.

- `010-the-tree-of-the-keys` — every key as the nodes of their tree, those of the default file first.
- `020-the-second-page-of-two-keys` — a page of two keys with their texts by the language, the default one as an empty
  name, and the languages of the project.
- `030-only-the-keys-with-problems` — a page of the keys the check of the files warns about.
- `040-the-keys-french-does-not-translate` — a page of the keys French shows the default text for.
- `045-the-keys-by-name-backwards` — a page of three keys ordered by their names, backwards; the total counts all five.
- `050-one-key-alone` — a key asked for is answered alone, whatever the search, with the warnings about its texts.
- `060-change-by-actions` — writes a text over `greeting`, adds `welcome` in two languages, removes `extra` and adds
  German with a text, all at once.
- `070-the-default-file-is-written-in-place` — only the line of `greeting` changed, and `welcome` is added at the end;
  the other lines are as they were written. Both lines are written with ` = `, as the file writes its keys.
- `080-the-french-file` — `extra` is gone, `welcome` is added at the end.
- `090-the-new-german-file` — the new file holds the text it was created with.
- `100-a-key-added-twice-is-refused` — a change refused refuses the others sent with it: `note` is not written.
- `110-the-keys-after-the-changes` — the keys as the changes left them.

`060-the-comments-stay-where-they-are-written` — the actions write only the lines of the keys they change, so the
comments of a file and its blank lines stay as they are written, and a key taken out leaves no blank line behind.

- `010-write-an-italian-file-with-comments` — writes `message_it.properties` of the folder of the group: comments
  starting with `#` and `!`, blank lines, a comment over `greeting`, over `note` and before `last`, `address` and
  `farewell` continued on the next lines, and `last` on the last line, with no line break after it.
- `020-change-the-italian-texts` — writes a text over `greeting` and over `farewell`, takes `note`, `address` and
  `last` out of Italian and adds `goodbye` in the default language and in Italian.
- `030-the-comments-stay` — every comment and blank line is where it was, the comment over `note` included, and no
  other blank line is left: the lines of `note`, `address` and `last` are gone with their line breaks, `greeting` and
  `farewell` are written anew on their lines, `farewell` on one line, and `goodbye` is added after the last comment.

`999-tierdown` closes the project, finds that the files of a closed project are neither checked, read nor changed,
and deletes it.

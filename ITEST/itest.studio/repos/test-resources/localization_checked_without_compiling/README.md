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
- `020-nothing-is-compiled` — the session compiled nothing for it.
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

`040-a-write-of-the-files-is-checked-in-place`

- `010-write-the-default-file` — writes `message.properties` again, `message.properties` of the folder of the
  scenario: `greeting` takes two values now, `farewell` is added, and the apostrophe of `note` is doubled.
- `020-the-status-without-compiling` — the warnings follow the files at once, nothing compiled again: the calls of
  `greeting` are checked against its new text, and `farewell` is found.
- `030-the-problems-of-the-files` — the problems of the files as they are now.

`999-tierdown` closes the project, finds that the files of a closed project are not checked, and deletes it.

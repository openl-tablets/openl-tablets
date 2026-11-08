# The key of a msg call leads to the project its texts are written in

A cell that calls `msg` marks its key, and the editor opens the key on the **Localization** screen of the project its
texts are written in. A key the project writes in any language opens there. A key only a project it depends on gives
opens in that project. A key no localization file of the workspace writes, such as one a library of the project gives,
is told with its text in the tooltip and opens nothing.

- `base.zip` is the project `EPBDS-16933-base-messages`: an empty `Base.xlsx` and a default file that gives
  `shared.greeting` and `overridden`.
- `project.zip` is the project `EPBDS-16933-messages-of-a-base`, which depends on it: an empty `Main.xlsx`, a default
  file that writes `overridden` and `own`, and `lib/messages.jar`, on its classpath, whose `i18n/message.properties`
  gives `library.greeting` and `own`.

`010-setup` creates both projects and opens them.

`020-the-keys-lead-to-their-project`

- `010-a-table-calling-msg` — writes the Spreadsheet `Greet`, whose steps call `msg` with each key, and with `absent`,
  which nothing gives.
- `020-compile` — compiles `Main`.
- `030-its-cells-lead-to-the-projects-of-the-keys` — `shared.greeting` leads to the base by its id; `overridden` and
  `own`, which the project writes, lead to the project; `library.greeting` has no key to lead by and tells its text as
  that of an external resource; `absent` leads to the project, where it can be added.
- `040-the-base-has-the-key-it-gives` — the base answers `shared.greeting`, the key its link opens.
- `050-the-project-has-the-key-it-overrides` — the project answers `overridden` with its own text, and the text of the
  base beside it.

`999-tierdown` closes and erases both projects.

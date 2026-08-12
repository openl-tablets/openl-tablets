# EPBDS-16433 — the properties file name settings of a written `rules.xml`

The project overview writes `rules.xml` through the files API, which used to accept any descriptor: a processor
class that does not exist was stored and only failed later, while the legacy Editor rejected it on save. The API
now runs the engine's own check on the settings a descriptor write changes.

`020-descriptor-validation` writes the descriptor the way the overview does — a raw `PUT` of the file — and
asserts the wiring and the payload of each rejection, one per error code (the engine rules themselves are covered
by `ProjectDescriptorValidatorTest`):

- `010-unknown-processor-rejected` — a processor class the project cannot provide;
- `020-unknown-property-in-pattern-rejected` — a pattern naming a property that does not exist;
- `030-descriptor-is-untouched-by-the-rejections` — the file still holds what the project was created with, so a
  rejected write reached the repository in no form at all;
- `040-literal-pattern-accepted` — a pattern without a `%property%` is a plain file name mask and stays valid;
- `050-valid-settings-accepted` — valid settings uploaded as a file, the API's other write path;
- `060-accepted-settings-are-stored` — that write is the one the project keeps.

`999-tierdown` deletes the project and requires the shared `design-flat` repository to hold nothing under that
name afterwards. The closing listing asks for deleted projects too, so an archived leftover is not read as a
clean repository.

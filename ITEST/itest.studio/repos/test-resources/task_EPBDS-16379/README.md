# EPBDS-16379 — an upload that did not arrive in full is refused

An agent saving a rule module through the API reported success while only a third of the workbook reached the
repository: the API checked the first four bytes of an upload and nothing else, so anything that starts like a
workbook was stored as one. The API now reads the structure each format records about itself, and refuses
content that cannot be read to its end.

The fixtures carry the two shapes an interrupted upload takes, plus a complete file of each format:

- `Rating-cut.xlsx` — cut in the middle of an entry, a third of the workbook;
- `EPBDS-16379-broken-module.zip` — an intact project archive carrying `Rating-tailless.xlsx` as its module;
- `Rating-tailless.xlsx` — cut where the ZIP directory starts, so 82% of the bytes and **every entry** arrived,
  and reading the archive entry by entry finds no damage at all;
- `Legacy-cut.xls` — an OLE2 workbook cut to a third;
- `upload-tailless.zip`, `EPBDS-16379-tailless.zip` — archives that lost only their directory;
- `Rating.xlsx`, `Legacy.xls`, `upload.zip`, `EPBDS-16379.zip` — the complete originals.

Scenarios:

- `020-damaged-module` — the module write paths of the files API: a raw `POST`, a multipart `POST` and a raw
  `PUT`. Each damaged upload is refused, the rejected module is not created, a complete workbook of either
  format is still accepted, and a module survives a rejected update with the content it had.
- `030-damaged-archive` — the archive upload of the files API. A damaged archive expands into nothing, while a
  complete one expands as before.
- `040-damaged-project` — project creation, from an archive and from module files. Neither damaged upload
  creates a project, and neither does an archive that arrived in full but carries a module that did not:
  `EPBDS-16379-broken-module.zip` records the right checksum for a `Rating.xlsx` that lost its own tail.
- `999-tierdown` — closes and deletes the project, and requires the shared `design-flat` repository to hold
  nothing under that name afterward.

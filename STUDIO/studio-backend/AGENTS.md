# studio-backend — Backend REST API Conventions

Rules for Studio REST controllers (`org.openl.studio.**.rest.controller`, `org.openl.rules.rest.**`) and their
request/response models. They keep the generated `/rest/openapi.json` spec consistent, localizable, and correctly
validated. Follow them for **every** new or changed endpoint, DTO, and enum.

## OpenAPI Descriptions

Descriptions are resolved as **message keys**, not literal prose. `OpenApiPropertyResolverImpl.resolve(...)` looks each
string up in the `openApiMessageSource` bundle (`ApiConfig.openApiMessageSource` → `resources/i18n/openapi.properties`),
falling back to the string itself when no key matches. New APIs **MUST** use keys — never leave English prose in the
annotation.

- **Operation/endpoint docs → keys in `openapi.properties`.** `@Operation(summary=…, description=…)`,
  `@ApiResponse(description=…)`, and method-level `@Parameter(description=…)` (path/query/header params) all carry keys.
- **Key namespace** mirrors the area: `<area>.<operation>.summary` / `.desc`, `.param.<param-name>.desc`,
  `.<statusCode>.desc`. Existing prefixes: `repos.`, `projects.`, `deployments.`, `tags.`, `acls.`, `mgmt.`, `users.`,
  `trace.`, `test.`, `diff.`. Reuse a shared param key across sibling endpoints (e.g. `project.table.id.desc`,
  `repo.param.branch-name.desc`, `header.location.desc`) instead of duplicating.
- **Descriptions use CommonMark.** Backticks, lists, and `\n` line breaks are allowed (see the file header).
- **`@Tag(description=…)` stays literal** — tag descriptions are not externalized anywhere in this module.
- **An `@ApiResponse` carries its code and description key only.** The generator takes the response schema from the
  return type of the method and the media type from the `produces` of the class-level `@RequestMapping`, so a
  `content = @Content(mediaType = …, schema = @Schema(implementation = …))` repeating them is noise. Declare the media
  type once, on the class; name `content` only where neither the return type nor the class tells it.
- **Model field descriptions are literal**, NOT keys — see below. The key convention is for the endpoint surface only.

## `@Parameter` vs `@Schema` on Model Fields

- **Every exposed field / record component MUST have a description** via
  `io.swagger.v3.oas.annotations.Parameter` → `@Parameter(description = "literal text")`. Model field descriptions are
  literal English (the resolver's fallback returns them verbatim).
- **A field whose type is another component MUST use `@Parameter`, never a field-level `@Schema(description=…)`.** This
  covers a DTO-typed field or a `List`/`Collection`/`Map` of one. The stock swagger `ModelResolver` copies a field-level
  `@Schema` description onto the **referenced component schema**, whereas
  `PropertySchemaCustomizingConverter` (package `org.openl.rules.spring.openapi.converter`) reads `@Parameter` and
  sets the description on the **property only**.
- **A class-level `@Schema(description=…)` on the component itself is correct** and expected — keep it. Simple scalar
  fields (`String`, `boolean`, `long`, enum, `Map<String,String>`) do not leak, but use `@Parameter` for them too so the
  whole model is uniform.
- `@JsonProperty(access = READ_ONLY)`, `@Parameter(required=…)`, `example`, and `allowableValues` are all honored by the
  converter — use them rather than a field-level `@Schema`.

## Enums on the Wire

Every enum reachable through the REST/OpenAPI surface — a response field type **or** a query/path parameter — **MUST**
carry `@JsonProperty("…")` on each constant, with a lowercase (camelCase for multi-word) wire code. Never expose the
Java `UPPER_SNAKE` `.name()`.

- The code drives both serialization and request binding: `JacksonEnumConverterFactory`
  (`org.openl.studio.common.web`) binds query params through Jackson, so `@JsonProperty` controls the accepted value and
  the generated schema `enum` array alike.
- Changing an enum's wire values is a **breaking change**. Update the frontend union/type in `studio-ui/`, the affected
  `.req`/`.resp` fixtures, and regenerate the OpenAPI goldens in lockstep.
- A shared core enum with its own dedicated converter (e.g. `ProjectStatus` via `ProjectStatusConverter`) is out of
  scope — do not add `@JsonProperty` to it here.

## Ids in a URL Path

A project is addressed by `ProjectId`, a deployment by `DeploymentId`. Both travel as a **path segment**, so both
**MUST** stay within one.

- **A project id is a hash of where the project is stored** — 13 upper-case characters of Crockford's Base32 of the
  SHA-256 of the repository id and the project folder (`ProjectIds`; the design is
  `Docs/architecture/project-ids.md`). The alphabet keeps it within one segment as it is, so it needs no encoding.
  - **Take the id from the project** — `AProject.getProjectId()`, which `RulesProject` computes once. Never build one
    from a name, a `FileData` or a request value: a create or a copy answers `ProjectCreationService.idOfWritten`, the
    id of the folder it wrote. A client never builds or decodes an id either.
  - **The folder, not the name.** A design project's folder is its internal path in a repository with folders, or the
    key it is stored under in JDBC and AWS S3; a local-only project's folder is its workspace folder. The id survives a
    rename in `rules.xml`, saved or not, a branch switch, an open and a close.
  - **An id only.** A project path variable (`@ProjectId`), `{project-id}` of the ACL endpoints, `sourceProject` of a
    copy, `projectId` of the deploy requests and `dependsOn` take an id. `ProjectIdConverter` resolves it in the
    user's workspace, which holds only the projects the user may read; `AProjectConverter` resolves it in the design
    repositories, for the ACL endpoints. A project name and a Base64 id of OpenL Studio 6.x answer `404`
    `project.identifier.message`.
  - **An old id leads on.** While the server runs, the design repositories remember the ids that stopped naming a
    project (`StaleProjectIds`, learned from the listings read whole). Both converters ask `StaleProjects.find` only
    once the live lookup misses, so a live id always wins: a moved project is served under its current id, which
    `ContentLocation.servedAs` names, and a deleted one answers `410` `project.deleted.message`.
  - **A body names a project as a path does.** A design project named in a request body goes through
    `ProjectIdConverter` too (EPBDS-16328), never `UserWorkspace.getProjectsByName`; leave reading the project to the
    endpoint, so its own refusal message survives.
  - **Dependencies match by name.** A local-only dependency matches the logical name its `rules.xml` declares,
    because it has no design identity, and the resolved dependency carries the folder-based id, so following its link
    addresses the actual project (EPBDS-16518). A repository-backed project keeps its stable business name for
    dependency lookup; do not index it by the logical name read from the currently selected branch, as described in
    `Docs/architecture/cross-branch-projects.md`.
- **A deployment id is `repositoryId:name` in Base64.**
  - `encode()` (the `@JsonValue`) uses the **URL-safe** alphabet and reads the name as UTF-8. The standard alphabet is
    forbidden here: its `/` is read as a path separator (404), and percent-encoding it to `%2F` is rejected as an
    ambiguous separator (400). A name outside US-ASCII makes that `/` likely — Cyrillic names hit it about a quarter
    of the time.
  - `decode()` accepts both alphabets, so an id kept in a bookmark or a script keeps working. Never tighten it to one
    alphabet. Never hand a caller an id from anything but `encode()`: a hand-rolled `Base64.getEncoder()`
    reintroduces the slash. A browser normalizes one with `toUrlSafeDeploymentId` of `studio-ui`'s
    `services/deployments.ts`.

## State Kept Between Requests

- **State of one client is `@ClientSessionScope`, never `@SessionScope`.** A browser keeps it in its HTTP session; a
  request with its own credentials (bearer token, personal access token, Basic authentication) opens no session and
  finds it kept for the credential. A `@SessionScope` bean would open an HTTP session for every such request and
  lose its state between them. See [`Docs/architecture/client-sessions.md`](../../Docs/architecture/client-sessions.md).
- **Never open an HTTP session in a REST or WebSocket path.** No `request.getSession()` / `getSession(true)` and no
  saving of the security context: the `/rest/**` and `/ws` chains only read a browser's sign-in
  (`restSecurityContextFilter`).
- **A holder of the user workspace hands it back.** Take it with `MultiUserWorkspaceManager.acquireUserWorkspace` and
  return it with `releaseUserWorkspace`; never call `UserWorkspace.release()` directly — other clients of the user
  still work in it.
- **A project is read-only for a client while the client merges it.** `MergeInProgressInterceptor` refuses every
  request with a `projectId` path variable but a read with `409` `project.merge.running.message` while the client's
  merge of that project runs in the background, and brings an ended merge to the editor before any request about the
  project. A new endpoint of a project needs nothing for it, unless it changes nothing though it is no `GET` — a
  compilation, a run, a trace, a comparison: it carries `@ChangesNothing`, or the editor can neither show nor run
  the project while it is merged. See
  [`Docs/api/projects-merge-architecture.md`](../../Docs/api/projects-merge-architecture.md).

## User Settings

- **User administration is administrator-only.** `PUT /rest/users/{username}` can change groups and set a password
  without the current one, so only `@AdminPrivilege` may guard it. A user changes their own details through
  `PUT /rest/users/info` and their own password through `PUT /rest/users/profile`, which validates the current password.

- **The profile carries no setting.** `GET` and `PUT /rest/users/profile` hold the details of the user alone: the
  name, the e-mail, the display name and, on `PUT`, the change of the password. **My Settings** live in the browser
  (see `STUDIO/studio-ui/AGENTS.md`), so a setting the profile once had is refused as an unknown field
  (`removed-user-settings` of `itest.studio/users`). `UserSettingManagementService` keeps only the e-mail
  verification token of a user (`MailController`, `MailSender`).

## Request Validation

- **A `@RequestBody` needs `@Valid`** for its bean constraints to run. Without it, `@ProjectNameConstraint`, `@NotBlank`,
  nested `@Valid`, etc. are **silently skipped**. Always write `@Valid @RequestBody Xxx request`.
- **Constrain request params and request-model fields** where a value is required or bounded: `@NotNull`, `@NotBlank`,
  `@Size`, `@Pattern`, `@Min`/`@Max`, and custom constraints. These also surface as `required` / `minLength` / etc. in
  the schema.
- **Box a required numeric param.** A primitive `int`/`boolean` param silently defaults (a missing `int` becomes `0`);
  use `@NotNull Integer` when the value must be supplied.
- Cross-field or service-level rules use Spring `Validator`s run through `BeanValidationProvider`
  (`org.openl.studio.common.validation`). Message keys live in `ValidationMessages.properties`; see the
  `localized-exceptions-and-validation-skill`.

## Uploaded Content

**An endpoint that stores uploaded content MUST verify it with `FileIntegrityValidator`**
(`org.openl.studio.common.validation`) before it reaches a project or a repository — see
[`Docs/architecture/upload-integrity.md`](../../Docs/architecture/upload-integrity.md).

- It reads the structure the format records about itself: the **central directory** of a `.xlsx`/`.xlsm`/`.zip`
  and the checksum of every entry, the workbook stream of a `.xls`. Content of any other type passes untouched.
- **A file signature, Apache POI, and any streaming ZIP reader all accept an upload that lost its tail** — the
  most likely shape of an interrupted upload. None of them is a substitute (EPBDS-16379).
- **The check is bounded, and every bound narrows what it promises** — an upload above 1000 MB is refused rather
  than checked, an archive that unpacks to more than 2 GB keeps only the structural check, and a workbook above
  100 MB carried by an archive keeps only the checksum recorded for it. Keep
  [`upload-integrity.md`](../../Docs/architecture/upload-integrity.md) and the user guide in step with them.
- The stream overload returns a stream over a temporary copy that deletes itself on close, so its call site
  **MUST** consume it inside a try-with-resources. The `Path` and `byte[]` overloads leave nothing to clean up,
  and `verifyContent` is the one to call when the content is read only to be checked.
- A caller that expands an archive reads it through `openArchive` and checks each entry it reads with
  `verifyEntry`, so the archive is walked once instead of being verified and then read again.
- A rejection is a `BadRequestException` carrying the file name and the reason: `file.content.damaged.message`
  for a file, `file.archive.invalid.message` for an expanded archive.

## Table Formatting

The design is [`table-formatting.md`](../../Docs/architecture/table-formatting.md). Every table but a table of the type
Other (`XLS_OTHER`, and `XLS_TABLEPART`, which the screen shows as Other) is laid out in the roles of its cells
(`RawTableRole`). A read reports the roles (`RawTableView.regions`), which `studio-ui` draws the Studio look by, and
`ExcelTableFormatter` writes the OpenL table style from the same roles into `.xlsx` workbooks. The code lives in
`projects/service/tables/format`.

- **One layout for both uses.** `TableRegionLayout` lays a table out in regions (`TableRegions`): rectangles of its
  cells, in table coordinates, that take one role and one type of value, the lines of its body that start a group
  (`RawTableGroups`), and the pieces of its header text (`SignatureSplitter`). It lays out the header and the
  properties for every kind and hands the body (`TableBody`) to the `BodyLayout` its `LayoutKind` names: a method of
  `DatatypeLayout`, `SpreadsheetLayout`, `TBasicLayout`, `DataLayout`, `DecisionLayout`, `ColumnMatchLayout` or
  `NamedValuesLayout`. A layout tells what each place of the body is (`Place`: a role and how its values are typed);
  `TableRegionLayout` alone gives it to every cell of the sheet the place takes, a merged region once, by the cell
  holding it, and drops a cell past the edge of the table (`GridSplitter` does not widen a table for a region of
  empty cells). A new kind of table is one constant of `LayoutKind`.
- **A role is a meaning, read from the compiled table, or from the text of one the compiler cannot read.** A layout
  names the role of each place it reads, never a position it assumes. How the compiler read a table is asked once, in
  `TableRegionLayout.of`: each `LayoutKind` names a function of `CompiledReads`, which tells whether the compiler read
  the table transposed, or nothing — a Datatype whose parent type is not found, a Vocabulary of a type that does not
  exist, a Data, a Test or a Run table whose type or tested table is missing, a Spreadsheet, a TBasic or a ColumnMatch
  table whose body could not be built, a ColumnMatch table of an unknown algorithm. Such a body is read from its text
  instead: each `LayoutKind` names a `TextRead` beside its compiled layout — a layout that finds the parts where the
  kind writes them, and a `GuessedReads` orientation test (field names, types, column kinds) — and
  `TableRegionLayout.of` picks one of the two once; only a body whose text tells none of its parts holds values
  (`BodyLayout.Placed.plain`). The type of a value (`ValueTypes`) is the data type the compiler gives the cell
  (`CellMetaInfo.getDataType`), a formula, or the body of a Method table being `code`; a region takes the type most of
  its values have on each of its lines, and a cell of another type names its own (`TableRegions.typeAt`). The
  properties take no type, but the value of a date property names the type `date` itself
  (`TableRegionLayout.withPropertyDates`), so the screen writes it in the formats of the reader.
- **A table is laid out once for each compilation.** `TableRegionLayout.regionsOf` keeps the regions in a weak map
  keyed by the compiled `TableSyntaxNode`, so every page of a large table reuses one layout. It is reused only while
  the workbook was not saved since the compilation read it (`XlsWorkbookSourceCodeModule.isSavedSinceRead`), and
  only for the grid the table was compiled from; otherwise the table is laid out as it stands.
- **Each kind is read as the compiler reads it.**
    - *Datatype, Vocabulary, Constants* (`DatatypeLayout`): the orientation, the columns and the row of titles of the
      compiled body (`DatatypeTableBoundNode.getColumnTitlesOrder()`, `hasColumnTitles()`); a type is `code`, the name
      of a field or a constant `output` (untyped: a name, not a value). A Datatype bound as the type of the values it
      lists (`AliasDatatypeMetaInfoReader`) is a Vocabulary. A Constants table is read by place, as
      `ConstantsTableBoundNode` reads it.
    - *Data, Test, Run* (`DataLayout`): the fields across, as `DataTableBindHelper` reads them — the field names
      (`code`), the row of references when `hasForeignKeysRow` finds one, the titles, then the values. The keys
      (`key`) are the column a reference reads the rows of a Data table by (`ITableModel.getKeyColumnIndex()`) unless
      it is a `_PK_` column, and in a Test and a Run table every column taken from a Data table
      (`ColumnDescriptor.isReference()`). A Data table gives its rows: its titles and values are
      `outputTitle`/`output`, as is a Test column whose field starts with `_res_` or `_error_`.
    - *Decision tables* (`DecisionLayout`): the places of the compiled `FunctionalRow`s — the code of a Rules table
      (`getInfoTable`, `getCodeTable`, `getParamsTable`), the titles (`getPresentationTable`) and the values. The column
      naming the rules is `key`. The titles of the vertical conditions of a lookup, in its corner, are `inputTitle`; its
      horizontal values over the grid it returns `outputTitle`. A condition value merged over several rules starts a
      group, and so does a horizontal value of a lookup merged over several columns. Conditions, Actions and Returns
      tables take the titles the compiler found (`ADtColumnsDefinitionTableBoundNode.getTitles`).
    - *Spreadsheet* (`SpreadsheetLayout`): the steps, the columns and the result the compiler read
      (`getStructureBuilder()`); a step name is a `value` (untyped: a name, not a value), one merged over its row a
      `section`.
    - *TBasic and ColumnMatch* (`TBasicLayout`, `ColumnMatchLayout`): the ids of the columns read as `AlgorithmBuilder`
      reads them (`TableRegionLayout.idsOf`), the rows the compiled table holds; a TBasic step starting a subroutine
      is a `section` and one that returns the `result`; a ColumnMatch condition with the conditions indented under it
      makes a group. Their indent carries the structure and is never changed.
    - *Environment, Properties* (`NamedValuesLayout`): the first column is the `key`, read as written.
- **The style is code, not a file.** `TableStyle` holds the look of each role: its built-in Excel cell style (name and
  `builtinId`), the heading or body font of the theme, bold and italic, the text colour (`dk1`, or `dk1` lighter by
  half), the fill, the alignment and the lines of the style. Colours are theme slots (`TableStyle.ThemeSlot`, the
  SpreadsheetML numbering with the first pairs swapped) with a tint (`PoiExcelHelper.ThemedColor`); no RGB is written.
- **`WorkbookStyles` owns the style in one workbook.** It redefines the nine built-in styles in `cellStyleXfs` and
  `cellStyles` (`customBuiltin`), in place when the workbook names them, and never touches Normal, whose font size every
  role takes. It reads the faces of the theme fonts from the theme part; a workbook without a theme, or with an empty
  one, gets the Office theme (`office-theme.xml` beside the class): `StylesTable.ensureThemesTable` alone makes an
  empty, Excel-invalid part, so `ThemesTable.readFrom` fills it. It indexes the fonts, fills, borders and cell formats
  of the workbook by what they hold, so formatting again adds nothing. POI's `putCellXf` and `putCellStyleXf` answer
  the new size, not the index. POI reads a border colour through the theme (`XSSFCellBorder.getBorderColor`, and so
  `equals`, which `putBorder` asks) and writes the RGB of the theme colour beside it, three bytes the schema refuses:
  the borders are read from their XML, and a border is added with the theme set aside.
- **`ExcelTableFormatter` writes cells, not styles.** Each cell gets the index of a cell format keyed by its role, the
  lines at the edges of its region and what is kept from the cell (number format, indent, wrap, rotation); no
  `CellStyle` object is made per cell (`CTCell.setS`). An indented cell is aligned left. The lines follow `roleLinesOf`
  of `studio-ui`, all in `dk1` (`Line.ROLE`): a line on the top edge of a region, never below the region above — above
  the signature, the properties, the code, the titles and a section, and below the last row of the signature, the
  properties and the code; a lookup lays out each row of its horizontal values as a region of its own (`Place.byRows`),
  so each row is set apart — the result between its own lines, a hair line above a group and on the left of a group
  column from the titles down (`TableStyle.isHeaderPart`), and a line closing the table. The header row is merged into
  one cell across the table unless another cell of it holds a value or a merge reaches out of it
  (`TableWrite.mergeSignature`, through the grid model when there is one, so its region pool stays in sync). The
  signature is the only rich text, in its pieces; rich text in any other cell keeps only its marks (`TextMarks`: a
  piece bold in Red, Orange, Green or Light Blue), each rebuilt on the font of the cell's role, and is plain otherwise.
  A table of an `.xls` workbook and a table of the type Other are not formatted.
- **A mark is a run, never a style.** `UpdateTarget.Cell.marks` writes a text with its marks
  (`RawTableWriter.writeMarks`): the run fonts are `XSSFFont` objects never registered in the workbook, so the font
  lives in the shared string and no cell format, named style or font is added. A font copied from a cell or a role
  keeps its theme colour beside an RGB set through `XSSFFont.setColor`, and the theme wins, so `TextMarks.fontOf`
  replaces the colour element.
- **Unused styles are removed only by Reformat.** `ExcelTableFormatter.removeUnusedStyles` (`UnusedStyles`) drops the
  cell formats no cell, row or column names and the named styles no cell format links, renumbering the references; it
  reads every cell, so a save of one table never runs it. It reads POI's private `xfs` and `styleXfs` lists, as POI
  writes the stylesheet from them, and keeps a copy of each entry: POI writes a shorter list into the stylesheet entry
  by entry and drops the entries left over, so a list must not hold one of them when a sheet reads its cell formats
  during the same write (`XmlValueDisconnectedException`).
- **Format on save formats with no compile.** While `table.format-on-save` is on (`TableFormatting`, read on each
  save), every save formats the table it writes before the one save of the workbook, so an edit and its formatting are
  one history revision. `TableWritersFactory.getTableWriter` arms each update writer
  (`TableWriter.formatOnSave(boolean)`), which takes the layout of the last compile before it writes
  (`RegionShift.ofCompiled`): `null` for a table of the type Other, and for one whose workbook was saved since the
  compile read it (`TableRegionLayout.compiledRegionsOf`), which is then saved unformatted. The writer tells the layout
  every line it inserts or removes (`createOrUpdateCell` growing the table, `removeRows`/`removeColumns` measuring how
  many lines the table really lost, the insert loops of `RawTableWriter`); `RegionShift.fittedTo` lays the header and
  the properties out again from the grid (`TableRegionLayout.headOf`), so a property row the edit wrote needs no
  tracking, and stretches or trims the regions to the size the table ends with. A property write
  (`TablePropertiesServiceImpl`) and a copy (`TableCopyServiceImpl`, the source's layout on the copy) take the layout
  the same way. A created table is formatted in the `regions` its create request carries, which the create dialog's
  builder emits (`buildTableRegions` in `tableSkeletons.ts`); a table written into a new module is formatted on its POI
  sheet (`ExcelTableFormatter.format(Sheet, IGridRegion, TableRegions)`). A formatting that fails is logged and the
  table saved unformatted.
- **A formatting pass costs every cell of the table.** Reading a cell's format through XMLBeans creates an object per
  attribute, about 0.5 µs a cell on a freshly loaded workbook, so `ExcelTableFormatter` reads `getS()` (never
  `isSetS()`), writes `setS()` only when the format changes, looks a row up once, and `WorkbookStyles` keeps the last
  format asked for. The header is built one run per piece (`XSSFRichTextString.append`): `applyFont` rebuilds every run
  of the text at each call, which cost a formatted upload of 1,000 tables a tenth of its time. A list XMLBeans hands
  out (`getCellStyleList()`, `getRowList()`, `getCList()`) reads the XML from its first element at each `get` and
  `size`, so a walk over it is quadratic: `WorkbookStyles` and `UnusedStyles` walk the arrays (`getCellStyleArray()`,
  `getRowArray()`, `getCArray()`), each read in one pass. A workbook whose sheets were copied in Excel names tens of
  thousands of styles, and the lookups of the role styles over the list made a save of it take minutes
  (EPBDS-16931). A table its previous save
  left formatted in the same regions (`FormattedTables`: the table's workbook file, sheet and corner, the regions, and
  the file's timestamp and length right after that save; any other write of the file formats the table in full again)
  has only the lines an edit touched formatted: `RegionShift.touchedRows`/`touchedColumns`, the lines inserted or
  restyled and the lines on either side of where lines were inserted or removed, and the header, written in its pieces
  at every save, as a value written into it is plain text — a value edit formats no other cell. A value write and a
  merge change no cell format but a number format, which the look keeps; a style edit reports the rows it restyles
  (`TableWriter.rowsRestyled`), and a header or properties block that changed height (`RegionShift.bodyMoved`) formats
  the table in full. `TableWriter.save` reads the position while the table is open for writing, as the save closes it,
  and tells `ExcelTableFormatter.formatOnSave` whether the table stands formatted.
- **Reformat Excel files is the project action** (`POST /projects/{id}/format`, `WorkspaceProjectService.formatProject`).
  It compiles what it needs instead of refusing: a project compiled only in part is compiled whole, and a project
  written since it was compiled (`ProjectModel.isWrittenSinceCompiled`) has its module built from its workbooks again
  (`ProjectModel.reset(ReloadType.RELOAD)`), as Verify builds it. Only a compilation the reader stopped is refused
  (`table.format.project.stopped.message`, `409`). It reaches the other modules through the dependency compile of the
  module open, so it has `ProjectModel.initProjectHistory(TableSyntaxNode, Module)` listen to the workbook of each
  table first. Each workbook is saved once; a table gathered from partial tables is reported in `skipped`. It notes no
  edit on the tables: no value changes.
- **An upload is formatted after one compile** (`UploadFormatting`), only its `.xlsx` files, while
  `table.format-on-save` is on. An upload into an opened project is written to the working copy first: the files
  controller's `postUpload` hook, after the session reset, has `WorkspaceProjectService.formatUploaded` format the
  tables whose workbook is an uploaded file in the compile of the module open when it holds them all
  (`UploadFormatting.compiledIn`, as Update replaces the module's own workbook), or else in a compile of the whole
  project. The workbooks are saved with `XlsWorkbookSourceCodeModule.saveLook`, which leaves the compile answering
  for them. An upload into a closed project (`ProjectFilesServiceImpl`), and a ZIP archive that overwrites a project
  (`ZipProjectSaveStrategy`), are staged in a temporary folder, each module holding an uploaded workbook compiled
  there with the dependency manager of a project opened in the session, and the formatted files committed once. A
  workbook that cannot be formatted is written as uploaded and the failure logged: formatting never fails an upload.
- **A style edit follows the setting and the palette.** While `table.format-on-save` is on, `RawTableWriter` refuses
  a `style` operation that names anything but `indent` (`TableWriter.isFormattingOnSave`, set by
  `formatOnSave(boolean)` even when the save formats nothing this time). A colour (`RawCellColor`) is a theme colour,
  `{theme, tint}`, written through the `ThemedColor` overloads of `SetFillColorAction`/`SetColorAction` (an `.xls`
  workbook, which has no theme, takes the Office RGB, `ThemedColor.toOfficeRgb`), or one of the ten standard colours,
  `{rgb}`; any other RGB is refused. The editor reads the setting from `GET /settings` (`formatTablesOnSave`).
- **A read sends each fact once.** `RawTableReader` names the styles of a read in a dictionary
  (`RawTableStyleDictionary`, `RawTableView.styles`): a cell and a piece of its text name their style by its index. A
  piece of text names where it ends (`RawTableTextRun.to`), not its text. A read of the values alone (`styles=false`)
  sends no styles. A style names no font name or size (`RawTableStyles.font`): the screen draws a table in the font of
  the application. The roles of the whole table come with the styles, once for every window of its rows. Every cell
  reports the lines it draws itself (`TableViewer.setGrid`); the screen draws each shared line once, the wider one
  winning (`collapsedBorders` of `studio-ui`). The comparison sends each side with its own dictionary
  (`RawTableCells`, `ComparisonSideView.styles`).

## Regenerating OpenAPI Goldens

Adding a description, changing an enum's wire codes, adding a `required`/`@NotBlank` field, or moving a leaked
description all change the ITEST goldens (`ITEST/itest.studio/simple/test-resources-simple/openapi.json.resp` and
`ITEST/itest.studio/multi/test-resources/000-openapi.json.resp`). `description`/`summary`/`operationId`/`version` values are masked to
`***`, so externalizing a literal to a same-text key does **not** move the golden — but enum arrays, `required`, and the
presence of a `description` key do. Rebuild the webapp (`mvn -o clean install -DskipTests -pl STUDIO/studio-backend`),
then run the capture-and-verify cycle (`WebStudioTest#simple+multi`, toggling `HttpClient.writeBodyTo`).

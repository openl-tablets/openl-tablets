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

A project and a deployment configuration are both addressed by `ProjectIdModel` — `repositoryId:name` in Base64 — and
that id travels as a **path segment**, so it **MUST** stay within one.

- `encode()` (the `@JsonValue`, so every id the API hands out) uses the **URL-safe** alphabet and reads the name as
  UTF-8. The standard alphabet is forbidden here: its `/` is read as a path separator (404), and percent-encoding it
  to `%2F` is rejected as an ambiguous separator (400). A name outside US-ASCII makes that `/` likely — Cyrillic
  names hit it about a quarter of the time.
- `decode()` accepts both alphabets, so an id kept in a bookmark or a script keeps working. Never tighten it to one
  alphabet.
- Never hand a caller an id from anything but `encode()`. A hand-rolled `Base64.getEncoder()` reintroduces the slash.
- A browser that has to build an id itself uses `encodeProjectId` from `studio-ui`'s `services/projectId.ts`. Never
  call `btoa` directly: it reads a string as Latin-1, so it throws above U+00FF and mis-encodes the range below it,
  and it emits the standard alphabet.
- **The name inside a project id is its storage folder, not the logical name declared in `rules.xml`.** A design
  project's id uses its design folder; a local-only project's id uses its workspace folder. The names differ when a
  project is renamed in `rules.xml` or when EDT loads a folder whose project declares a friendlier name. A local-only
  dependency matches that logical name because it has no Design repository identity, but the resolved dependency
  carries the folder-based id so following its link addresses the actual project (EPBDS-16518).

  A repository-backed project keeps its stable business name for dependency lookup. Do not index it by the logical
  name read from the currently selected branch: an unsaved rename there must not hide the original name from another
  branch that still contains and declares it. Branch membership remains the deciding scope as documented in
  `Docs/architecture/cross-branch-projects.md`.

  Before a mapped-project rename is saved, the workspace copy already sits in a folder named after the new name while
  the design repository still holds the old one. An id **MUST** keep resolving across that gap, otherwise the project
  becomes unaddressable and can be neither closed, reverted nor deleted (EPBDS-16229).
  `Base64ProjectResolveStrategy` therefore resolves a mapped id by its folder first — the id's name prefixed with
  `DesignTimeRepository.getRulesLocation()`, through `FolderMapper.getRealPath` and
  `UserWorkspace.getProjectByPath`. **Resolve by folder before resolving by business name**: a business name is
  carried by more than one project, so it can answer with a different project and let a destructive endpoint act on
  the wrong one. While a save-time merge conflict is unresolved, `MergeConflictProjectResolveStrategy` keeps the
  session's project addressable by that same id. The conflict dialog makes several requests after the failed save;
  each must resolve even when a workspace refresh has removed the transient renamed key (EPBDS-16269).

- **A design project named in a request body goes through the same resolver as one named in the path.** A non-flat
  repository tells its projects apart by the folder they live in, so it may carry one name in several folders;
  resolving a body field by business name therefore picks the wrong folder or none at all (EPBDS-16328). Call
  `ProjectIdentityConverter.resolveProjectIdentity(identity, repositoryId)` — the same strategy chain the
  `@ProjectId` path parameter uses, narrowed to one repository — instead of reaching for
  `UserWorkspace.getProjectsByName` or for a single `ProjectResolveStrategy`. Leave reading the project to the
  endpoint, so its own refusal message survives. An identity more than one project answers to is reported as
  `project.identifier.ambiguous.message`, naming the ids to choose from.
- **An ambiguous identity is a choice, not a failure.** `ProjectIdentityConverter` raises it as an
  `AmbiguityException`, and the 409 body (`AmbiguityError`) lists every match in `candidates` — its id, name,
  repository and, in a repository with mapped folders, the folder (`ProjectCandidateModel`) — so a client can offer
  the choice instead of parsing the message. The folder matters: one such repository may hold a name in several
  folders. Studio's project and module screens show that choice for a link by name (EPBDS-16702); a name that
  resolves moves the address on to the project's id.

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

- **A user setting is a field of the profile.** Every setting of **My Settings** is kept on the server, so it follows
  the user from browser to browser and no user reads another's (`task_EPBDS-7164-user-settings` of
  `itest.studio/users`). `UserProfileBaseModel` names the key each one is stored under, reads the settings of the
  user (`load`) and saves the ones a request carries (`store`), as `AdministrationSettings` does for the settings of
  the system, so no controller names the key of a setting, and a new setting adds a field to the profile and nothing
  else to the API. The default of each is in `openl-default.properties` under *Default values for user settings*,
  and `UserSettingManagementService` stores a value only where it differs from the default. The same store keeps the
  e-mail verification token (`MailController`), which is no setting and never part of the profile.

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
- **A role is a meaning, read from the compiled table.** A layout names the role of each place it reads, never a
  position it assumes. How the compiler read a table is asked once, in `TableRegionLayout.of`: each `LayoutKind`
  names a function of `CompiledReads`, which tells whether the compiler read the table transposed, or nothing — a
  Datatype whose parent type is not found, a Vocabulary of a type that does not exist, a Data, a Test or a Run table
  whose type or tested table is missing, a Spreadsheet, a TBasic or a ColumnMatch table whose body could not be
  built, a ColumnMatch table of an unknown algorithm. Such a body holds values (`BodyLayout.Placed.plain`). The type
  of a value (`ValueTypes`) is the data type the compiler gives the cell (`CellMetaInfo.getDataType`), a formula, or
  the body of a Method table being `code`; a region takes the type most of its values have on each of its lines,
  and a cell of another type names its own (`TableRegions.typeAt`).
- **A table is laid out once for each compilation.** `TableRegionLayout.regionsOf` keeps the regions in a weak map
  keyed by the compiled `TableSyntaxNode`, so every page of a large table reuses one layout. It is reused only while
  the workbook was not saved since the compilation read it (`XlsWorkbookSourceCodeModule.isSavedSinceRead`), and
  only for the grid the table was compiled from; otherwise the table is laid out as it stands.
- **Each kind is read as the compiler reads it.**
    - *Datatype, Vocabulary, Constants* (`DatatypeLayout`): the orientation, the columns and the row of titles of the
      compiled body (`DatatypeTableBoundNode.getColumnTitlesOrder()`, `hasColumnTitles()`); a type is `code`, a name
      `key`. A Datatype bound as the type of the values it lists (`AliasDatatypeMetaInfoReader`) is a Vocabulary. A
      Constants table is read by place, as `ConstantsTableBoundNode` reads it.
    - *Data, Test, Run* (`DataLayout`): the fields across, as `DataTableBindHelper` reads them — the field names
      (`code`), the row of references when `hasForeignKeysRow` finds one, the titles, then the values. The keys
      (`key`) are the column a reference reads the rows of a Data table by (`ITableModel.getKeyColumnIndex()`) unless
      it is a `_PK_` column, and in a Test and a Run table every column taken from a Data table
      (`ColumnDescriptor.isReference()`). A Test column whose field starts with `_res_` or `_error_` is
      `outputTitle`/`output`.
    - *Decision tables* (`DecisionLayout`): the places of the compiled `FunctionalRow`s — the code of a Rules table
      (`getInfoTable`, `getCodeTable`, `getParamsTable`), the titles (`getPresentationTable`) and the values. The
      column naming the rules is `key`. A condition value merged over several rules starts a group, and so does a
      horizontal value of a lookup merged over several columns. Conditions, Actions and Returns tables take the titles
      the compiler found (`ADtColumnsDefinitionTableBoundNode.getTitles`).
    - *Spreadsheet* (`SpreadsheetLayout`): the steps, the columns and the result the compiler read
      (`getStructureBuilder()`); a step name merged over its row is a `section`.
    - *TBasic and ColumnMatch* (`TBasicLayout`, `ColumnMatchLayout`): the ids of the columns read as `AlgorithmBuilder`
      reads them (`TableRegionLayout.idsOf`), the rows the compiled table holds; a TBasic step starting a subroutine
      is a `section` and one that returns the `result`; a ColumnMatch condition with the conditions indented under it
      makes a group. Their indent carries the structure and is never changed.
    - *Environment, Properties* (`NamedValuesLayout`): the first column is the `key`, read as written.
- **The style is code, not a file.** `TableStyle` holds the look of each role: its built-in Excel cell style (name and
  `builtinId`), the heading or body font of the theme, bold and italic, the text colour (`dk1`, or `dk1` lighter by
  half), the fill, the alignment and the lines of the style. Colours are theme slots (`TableStyle.ThemeSlot`, the
  SpreadsheetML numbering with the first pairs swapped) with a tint (`PoiExcelHelper.ThemedColor`); no RGB is written.
- **`WorkbookStyles` owns the style in one workbook.** It redefines the ten built-in styles in `cellStyleXfs` and
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
  `CellStyle` object is made per cell (`CTCell.setS`). An indented cell is aligned left. The lines follow
  `roleLinesOf` of `studio-ui`: the signature between two major lines, a minor line below the properties and below
  a section, the result between its own lines, a hair line above a group and on the left of a group column, a major
  line closing the table. The signature is the only rich text, in its pieces; rich text in any other cell is written
  plain. A table of an `.xls` workbook and a table of the type Other are not formatted.
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
  controller's `postUpload` hook, after the session reset, has `WorkspaceProjectService.formatUploaded` compile the
  project whole and format the tables whose workbook is an uploaded file, saved with
  `XlsWorkbookSourceCodeModule.saveLook`, which leaves the compile answering for the workbook. An upload into a closed
  project (`ProjectFilesServiceImpl`), and a ZIP archive that overwrites a project (`ZipProjectSaveStrategy`), are
  staged in a temporary folder, each module holding an uploaded workbook compiled there with the dependency manager of
  a project opened in the session, and the formatted files committed once. A workbook that cannot be formatted is
  written as uploaded and the failure logged: formatting never fails an upload.
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

# org.openl.rules.webstudio — Backend REST API Conventions

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
  `@Schema` description onto the **referenced component schema** (it leaks — e.g. onto `LastCommit` itself), whereas
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

## Table Theme

The looks OpenL Studio gives every table but a table of the type Other (`XLS_OTHER`, and `XLS_TABLEPART`, which the
screen shows as Other) are the `table-themes/*.yaml` files of its classpath
(`resources/table-themes/` ships `standard` and `green`). `standard` follows the formatting standard of OpenL
tables: its General section is the base, and the colours are the ones the standard names.
`TableThemeService` reads them once at startup, with the YAML anchors, aliases and merge keys resolved by SnakeYAML,
then binds them strictly with Jackson.
See `Docs/user-guides/openl-studio/appendices/table-themes.md` for the file format and `Docs/api/raw-tables-api.md`
for the endpoints.

- **A theme is known by its file name.** The file name without `.yaml` is the identifier the settings, the read
  parameter and the write actions carry; the `name` the file declares is only what the screen shows. Of two files of
  one identifier the first read is offered and the other logged as a warning (`TableThemeService`); two themes
  declaring one name are both offered.
- **The primary theme is offered first.** A file writing `primary: true` (`standard`) is listed before the others,
  then each group by name (`TableThemeService.inOfferedOrder`), in both theme lists. The screen applies a theme
  starting from the first one offered, never from an identifier it knows, and never from the table theme of the
  settings or the look of the Studio theme: those only decide what the screen draws.
- **No theme means the Excel formatting.** The `table.theme` user setting is empty by default, and a setting that
  is empty or names a theme Studio no longer offers draws the tables as the workbook formats them: the screen asks a
  read for the theme only when it is offered (`offeredTheme`). A read naming a theme Studio does not offer is refused
  with `400`. The REST mapper leaves the empty value out of the
  profile (`NON_EMPTY`), so the screen reads a missing `tableTheme` as **Excel Formatting** and sends `""` to choose
  it again.
- **A broken theme is left out, not fatal.** A file that cannot be read, declares no name, writes a key twice,
  writes a font size that is not a whole number (`ACCEPT_FLOAT_AS_INT` is off), names an unknown attribute or a
  colour it gives no name is logged as an error and not offered; Studio starts with the rest.
- **A theme names its colours once.** The `colors` key of the file maps a name to `#rrggbb` or to a theme colour of
  Excel as its palette names it (`Blue, Accent 1, Lighter 60%`: `ThemeColour.read`), made of the twelve colours the
  file writes under `themeColors` (`ExcelThemeColours`, keyed by `ExcelThemeColour`). `TableThemeService` takes
  `colors` out of the file before binding and reads `themeColors` beforehand, which also stays a key of the theme,
  and hands both to `ThemeColourReader`, which reads a colour wherever a part sets one (`color`, `background`, the
  `color` of a line). `ThemeStyle` and `ThemeBorderLine` therefore hold a `ThemeColour`: the `#rrggbb` the screen
  draws, which it checks, and the theme colour and tint it is written as (`PoiExcelHelper.ThemedColor`). The RGB of a
  theme colour is tinted as Excel tints it (`PoiExcelHelper.applyTint`), the function the reader draws a workbook
  colour with, so the overlay and the written workbook draw alike. The shipped themes set every colour by its name,
  each a theme colour of **Office 2013 - 2022** but the palest green of `green`.
- **The overlay tells where each colour is set.** `ThemeColourReader` gives every colour the key of the file it is
  read at (`ThemeColour.key`): the keys from the top of the file down, as Jackson reads the file with its aliases and
  merge keys resolved, such as `spreadsheet.values.background`, `base.header.keyword.color` or
  `spreadsheet.resultRow.border.top.color`, so a part an alias repeats has a key of its own. The key travels with the
  colour through every part laid over another, and the overlay reports it beside the colour (`backgroundKey`,
  `colorKey` of a style, a run and a border side), so the screen can draw a key in a colour of its own theme
  (**Override with Studio theme**). The key is no part of the colour (`ThemeColour.equals`): parts that look alike take
  one style in a workbook and draw one line. Which table theme the screen reads and which colour a key takes there is
  the frontend's alone: the server knows no theme of OpenL Studio and never writes a key into a workbook.
- **A theme colour is written as the theme colour only into a workbook of those theme colours.** The writer reads
  the theme colours of each workbook once (`ExcelThemeColours.areThoseOf`: all twelve compared by colour, not by the
  name, which Excel 2013-2022 writes as `Office`). When they are those of the theme, every colour of the theme is
  written as the theme colour, so the palette of Excel offers it; otherwise every colour is written as RGB, and so
  into an `.xls` workbook and one without a theme part. One table (`write`, the `theme` action) and the project
  (`writeAll`) write alike. A cell holding the colour the other way round, RGB for a theme colour or the reverse, has
  not the look, so writing again turns it into the way the workbook takes it.
- **The theme of a workbook is never changed: a limit of Apache POI.** POI reads the theme colours
  (`ThemesTable.getThemeColor`) but has no API to set them or to give a workbook another theme; `ensureThemesTable()`
  makes an empty, Excel-invalid part. Replacing the colour scheme through the schema classes (`ThemeDocument`, then
  `ThemesTable.readFrom`) was tried and dropped: the part held the new colours, yet Excel kept drawing the theme
  colours of Office 2007 - 2010 from the hybrid theme. So any workbook of other theme colours takes RGB, and the guide
  says how to give a workbook the theme colours in Excel before writing the theme. The usermodel writes a theme colour
  with a tint into fills and lines (`XSSFColor.setTheme`, `setTint`), but `XSSFFont.setColor(XSSFColor)` keeps the RGB
  alone (a themed colour turns into `<color rgb=""/>`), so a font of a theme colour is coloured through
  `getCTFont()` (`PoiExcelHelper.setThemedColor`). The colour of a font of a workbook is read there too, from a copy
  (`PoiExcelHelper.colourOf`): POI writes the RGB of a theme colour into the colour it reads
  (`ThemesTable.inheritFromThemeAsRequired`), and `findOrCreateFont` reads every font of the workbook, so each font of
  a theme colour would be saved with an RGB beside it.
- **A theme is one style for every kind.** Every theme styles every kind of table but Other, and a kind the theme
  writes nothing for takes the base alone. The shipped themes write a look only for the kinds the formatting standard
  of OpenL tables describes — a Datatype and a Vocabulary, a Spreadsheet, a Data and a Test table, the decision tables
  and the lookups — and give every other kind (TBasic, Method, Run, ColumnMatch, Conditions, Actions, Returns,
  Environment, Properties, Constants) the base alone, which is the General format of the standard. The layouts of
  those kinds stay, so a theme of one's own can still give them a look, and their tests read
  `test-table-themes/every-kind.yaml` (`TestThemes.everyKind()`), which does. The server decides which
  tables a theme suits (`GET .../tables/{id}/themes`): the screen never keeps a list of themed kinds. Each kind is a
  constant of `ThemeKind`, which names the part of `TableTheme` the kind takes its look from and the `BodyLayout` of
  its body, so a new kind of table is one constant there and one part of `TableTheme`.
- **Every kind extends the base.** `base` is the skin every table shares — the signature, the properties, the cell
  style, the closing line. `TableTheme.lookOf` lays what a kind writes over it part by part (`Look.extendedBy`), so
  a kind needs no YAML merge key and writes only what it changes. One `Look` record holds the parts of every kind,
  and each layout reads its own: a part another kind takes is not used. The base of the shipped themes is the General
  format alone, so a part the kinds share — the titles, the return titles, the returns, the types, the code, the
  groups — is written once under the first kind that takes it and repeated by an alias (`titles: *title`). A kind
  that looks like another is an alias in the file (`smartRules: *simple`, `smartLookup: *lookup`), never a rule of
  the code, so a theme can still give it a look of its own.
- **One table is themed through its edit.** The `theme` action of `RawTableSourceAction` writes the theme inside the
  edit batch, after the values and before the styling, so the rows the batch added are themed and the styling the
  user set stands over the theme. Only the whole project has an endpoint of its own (`POST /projects/{id}/theme`).
  It takes the tables from what the session compiled, so a project compiled only in part — its module set to compile
  alone — is compiled whole first, and a project whose compilation the reader stopped is refused (`409`): the tables
  of the modules left out would be missed without a word.
  It reaches the other modules of the project through the dependency compile of the module open, whose workbooks
  nothing listens to, so it has `ProjectModel.initProjectHistory(TableSyntaxNode, Module)` listen to the workbook of
  each table first: a write there is kept in the history of its module and marks the project modified, as an edit
  of the table does.
- **One layout for both uses.** `ThemeLayouts` themes the header and the properties for every kind and hands the
  body, with what a layout knows of the table (`ThemedBody`), to the `BodyLayout` its `ThemeKind` names: a method of
  `DatatypeThemeLayout`, `SpreadsheetThemeLayout`, `TBasicThemeLayout`, `DataThemeLayout`, `DecisionThemeLayout`,
  `ColumnMatchThemeLayout` or `NamedValuesThemeLayout`. The body of a Method table is code, which takes `base`
  (`BodyLayout.Placed.plain`). Both the screen overlay and `ThemeExcelWriter` ask it, so what is drawn is what
  writing the theme gives.
- **A layout tells the look of a place, `ThemeLayouts` themes the sheet.** A layout answers the places it reads the
  body in and the look of each (`BodyLayout.Placed`), and `ThemeLayouts` alone gives that look to every cell of the
  sheet the place takes, so a row written over several rows of the sheet is themed whole. It themes a merged region
  once, by the cell that holds it, and lays `lastRow` over every cell that reaches the bottom of the table.
  `ThemeLayouts.of` drops every cell past the edge of the table:
  `GridSplitter` does not widen a table for a region of empty cells, so such a region may be merged past its edge.
  How the compiler read a table is asked once for every kind, in `ThemeLayouts.of`: each `ThemeKind` names a function
  of `CompiledReads`, which tells whether the compiler read the table transposed, or nothing for a table it read none
  of — a Datatype whose parent type is not found, a Vocabulary of a type that does not exist, a Data, a Test or a Run
  table whose type is missing or defined with errors or whose tested table does not exist, a Spreadsheet, a TBasic or
  a ColumnMatch table whose body could not be built, a ColumnMatch table of an algorithm the compiler does not know.
  Such a table has no parts the compiler knows, so its body takes the base alone: the theme never guesses the parts
  from where the cells stand. Every layout takes its parts from the compiled table, each found where the edits since
  the compilation moved it (`CompiledTable`, `ThemedBody.columnsNow`, `rowsNow`, `placesNow`); only an Environment,
  which the loader reads by its rows, and a Method table, whose body is code, are read as written. A Conditions, an
  Actions and a Returns table take their axes from the titles the compiler found.
- **A Spreadsheet section is a merge.** A step whose name cell is merged over the values of its row heads a section:
  the compiler takes it for a step with no value. The steps, the columns and the result are the ones the compiler
  read (`SpreadsheetBoundNode.getStructureBuilder()`). A step or a column it read marked for the result, its name
  ending with `*` before its `: type`, takes `marked` (`getRowHeaders`, `getColumnHeaders`, `isAsteriskPresented`),
  and the step or the column it returns takes `result` (`getReturnHeaderDefinition`): the one named `RETURN`, or else
  the last step it read, so a row repeating the name of a step or one whose name cannot be read is none. A
  Spreadsheet returning `SpreadsheetResult` returns every step; the formatting standard takes its last step for the
  result all the same, and so does the layout. A Spreadsheet returning `void` returns nothing. `resultRow` is laid
  over every cell of the row of that step, its lines round the whole step however many rows of the sheet it takes
  (`ThemeStyle.atEdges`).
  `HeaderRuns` splits a header by its keyword: a Datatype names its type first, a Spreadsheet its return type, its
  name and its parameters, and a decision table reads as a Spreadsheet, its return type of several words at times
  (`Collect Error[]`). An Environment header is its keyword alone; a Properties, a Constants, a Conditions, an
  Actions and a Returns header name the table.
- **An active theme overrides the look of the workbook.** The shipped themes name every attribute in the base
  style: `none` takes every side away, the fill is white, every font flag is off. A themed table therefore shows
  only the fills, lines, fonts and alignment the theme names. A text the workbook formats in pieces of its own,
  other than the header, is drawn in the font of its cell, which a read naming the theme reports with no pieces
  (`RawTableReader`), and written so (`ThemeExcelWriter.writeRuns`): `ThemedCell.keepsOwnRuns` is the one rule the
  reader and the writer ask. The properties are one section: their top and bottom lines go round
  it, not round each property. The screen draws the side of the upper or the left cell over
  its neighbour, as `TableViewer.setBorder` hands a workbook line two cells share to that cell, so `ThemeLines`
  moves a theme line on the top or the left of a cell to the cell above or on its left in the overlay. A window of
  rows has rows under it: its last row also takes the line the theme draws over the row under it, which the next
  window keeps on its first row, so windows drawn one under the other draw it as the whole table does. A table shown
  without its header (**Show Header** off) is cut on the screen, so the screen draws the line under the last row it
  hides on the top of the first row it shows (`withoutFirstRows` of `studio-ui`). The writer keeps the sides as the
  theme names them: the workbook draws a line either cell names.
- **A Data, a Test and a Run table are read as the compiler reads them.** `DataThemeLayout` reads the body with its
  fields across, as `DataTableBindHelper` does: the field names, the row of references when `hasForeignKeysRow` finds
  one, the titles, then the values. A Run table is bound as a Test table without expected results
  (`TestMethodNodeBinder`), so it takes the same places. A transposed table is told apart by the compiled table
  (`DataTableMetaInfoReader`, `ITable.getData().isNormalOrientation()`). `ids` marks the keys of a Data table as the
  compiled table tells them, never by a position the theme assumes: in a Data table the column a reference reads its
  rows by (`ITableModel.getKeyColumnIndex()`, the rule `ForeignKeyColumnDescriptor` reads a reference with) unless it
  is a `_PK_` column, which names the keys itself and takes no `ids`, as the formatting standard writes it; in a Test
  and a Run table every column the compiled model takes from a Data table (`ColumnDescriptor.isReference()`). Each is
  found where the edits since the compilation moved it, by a cell of the compiled data mapped through `TableMoves`
  (`CompiledTable.addPlaces`), and a table the compiler read none of takes the base alone. Its
  references to other
  tables are left as values. `empty` is laid over a blank value, cell by cell — a fill written with the theme, not a
  conditional format, so the workbook reads it back as the overlay draws it. A column of a Test table whose field
  starts with `_res_` or `_error_` holds the result it expects, as `TestSuiteMethod.createFieldsToTest` tells it: its
  title takes `returnTitles` over `titles` and its values `returns` over `values`. The rows naming the fields are one
  block, the lines of `name` going round it (`ThemeStyle.atEdges`). A transposed table draws no line round them, and
  its names and titles, which label its rows there, take the alignment of the base, as the formatting standard
  writes them.
- **A decision table is read as the compiler reads it.** Every kind is compiled into the `FunctionalRow`s of its
  conditions and its actions, the returns among them, so `DecisionThemeLayout` takes the places from those: the
  kind, the code and the parameters of a Rules table (`getInfoTable`, `getCodeTable`, `getParamsTable`), the titles
  (`getPresentationTable`) and the values (`getValueCell`). A SimpleRules, a SmartRules and a lookup have no code on
  the sheet: the compiler writes it into a grid of its own, which the layout never reads, and the merges of a lookup
  are read from the sheet. A place is a line of the sheet — a row, or a column of a table compiled with a rule in each
  column — and a cross. Such a table takes the looks with their lines turned (`ThemeBorder.transposed`): a line a
  look draws above a part is on its left, while the base style keeps its lines where it names them. Unlike a
  transposed Datatype or Data table, whose shipped looks draw no lines inside the table, a decision table needs it:
  the lines between the conditions and over a group would stand across its rules. Every line under the code, the
  titles and the horizontal conditions holds rules, so a rule an edit adds is themed before the table is compiled.
  The compiled places count from where the table stood when it was compiled: `RawTableWriter` keeps the rows and the
  columns its edits insert and delete as `TableMoves`, and a `theme` action after them finds each place where they
  moved it (`CompiledTable`). A read and the project-wide writer theme a compiled table, so they pass
  `TableMoves.NONE`. A line a look draws above a rule sets it apart from the rule before it, so the first rule takes
  the top side of the base, and so does a line on the left of the first column of the grid of a lookup, which the
  right line of its conditions closes (`Looks.apart`): no two cells name a line on one edge. The column naming the
  rules is the one of kind `RULE` in a Rules table, and in a table matched by its titles the one whose title the
  compiler matched as the names of the rules (`DecisionTableMetaInfoReader.getMetaInfos().getRules()`): a title it
  matched as nothing names no rules. A condition value merged over several rules while another
  column is split makes them a group: `groups` is laid over the first rule and over the rule after it. A value of a
  horizontal condition merged over several columns of a lookup groups them the same way, the axes swapped
  (`Reader.groupEdges`), and `groups` is laid over them turned (`ThemeStyle.transposed`). A table that did not compile
  takes the base alone.
- **A Conditions, an Actions and a Returns table are read as the compiler reads them.** They declare what decision
  tables take by their titles, each declaration in its inputs, its expression, its parameters and its titles: the
  code and the titles of a Rules table without its rules, so `DecisionThemeLayout.conditions` and `actions` lay them out
  with the places and the looks of a decision table. The titles take the `titles` look in a Conditions table and
  `returnTitles` in an Actions and a Returns table, and every other line of the body is code, closed as the code of a
  Rules table, each run of it apart where the titles stand between; a keyword naming a part takes the look of the
  part. The compiler reads a part in each column and finds the titles by their keyword or by their place, so the
  engine keeps where it found them (`ADtColumnsDefinitionTableBoundNode.getTitles`, through
  `DtColumnsDefinitionMetaInfoReader`) and the layout takes them where `TableMoves` moved them. A table written as
  the Reference Guide writes it, a part in each row, is compiled transposed: its rows read as the code and the titles
  of a Rules table, so it takes the looks upright, and a table with a part in each column takes them turned. The
  shipped themes give these tables the General format alone; `every-kind.yaml` aliases the look of a Rules table
  (`conditions: *rules`, `actions: *rules`, `returns: *rules`). A table no declaration is read from takes the base
  alone.
- **A TBasic and a ColumnMatch table are read as the compiler reads them.** Both name their columns by ids in the
  first row of the body, title them in the second row, and nest by the indent of a cell. No theme changes the indent:
  `ThemeStyle` has none, and the writer clones the style of the cell. `TBasicThemeLayout` gives the ids `code`, the
  titles `titles` with `stepTitle` over the title of the labels, the labels `steps`, the conditions `condition` and
  what a step runs (`action`, `before`, `after`) `values`. A theme can leave the conditions out, as `every-kind.yaml`
  does, so only what a step runs is filled, or give them a look of their own. The compiled algorithm keeps the
  operations it runs, not where they stand, so once the compiler built the steps (`Algorithm.getAlgorithmSteps()`)
  the layout reads the ids (`ThemeLayouts.idsOf`, trimmed and in lower case) and the operation of each row of the
  sheet as `AlgorithmBuilder` reads them, and lays `sections` over every cell of a step that starts a subroutine
  (`SUB`, `FUNCTION`) and `result` over every cell of one that returns (`RETURN`). `ColumnMatchThemeLayout` takes its
  places from the compiled table (`ColumnMatch.getColumns`, `getRows`): the id of each column, the rows giving what
  the table returns or scores — as many as the algorithm the compiler read in the header reads before its conditions
  (`IMatchAlgorithmCompiler.getSpecialRowCount`: one for `MATCH`, the default, and for `SCORE`, three for `WEIGHTED`)
  — and the indent it read of each name. Their `values` take `returns` and the rest `returnTitles`. The conditions
  under them take `name` in the `names` column and `values` elsewhere, and a condition whose name is not indented,
  with the conditions indented under it, makes a group: `groups` is laid over its first row and over the row after
  it. The shipped themes give both the General format alone; `every-kind.yaml` gives a TBasic table the look of a
  Spreadsheet (`tbasic: *spreadsheet`) and the code of its base, and a ColumnMatch table the titles, the returns and
  the code of its base, with a line after the names and between the columns of its values.
- **An Environment, a Properties and a Constants table name a value in each row.** `NamedValuesThemeLayout` gives
  the first column of an Environment (the setting: `import`, `dependency`, `include`) and of a Properties table (the
  property) the `name` look and the rest `values`. The loader reads every Environment by its rows
  (`SequentialXlsLoader.preprocessEnvironmentTable`) and a Properties table is bound as the properties section of its
  tables (`PropertyTableMetaInfoReader`), so both are read as written. A Constants table is read as
  `ConstantsTableBoundNode` reads it, by place alone: the type, the name and the value of a constant in its first
  three columns, under no row of titles even where its first row reads as the titles of a Datatype
  (`DatatypeThemeLayout.constants`), with the orientation of the compiled table (`ConstantsTableMetaInfoReader`,
  `getNormalizedData().isNormalOrientation()`). The shipped themes give these tables
  the General format alone; `every-kind.yaml` draws an Environment in greys, a Properties table as an alias of its
  look (`properties: *technical`), and the names of a Constants table in the fill of the field names of a Datatype
  (`name: *fieldName`).
- **A Datatype is themed as it is compiled.** Only the compiler tells a transposed table apart, and the columns a
  titled one names, so the layout takes the orientation of the compiled body (`DatatypeTableMetaInfoReader`,
  `isNormalOrientation()`), its columns (`DatatypeTableBoundNode.getColumnTitlesOrder()`) and whether it read a row of
  titles (`hasColumnTitles()`): the places follow the fields, and `lastRow` stays the last row as written. A Datatype
  is a Vocabulary when the compiler bound it as the type of the values it lists (`AliasDatatypeMetaInfoReader`),
  whatever its header looks like. A Datatype whose parent type is not found has no fields the compiler read, and
  takes the base alone for its body, as a Data table does.
- **The overlay is a view only.** A read naming a theme reports the look of the theme in `RawTableCell.style` and
  `runs`, in place of the formatting of the workbook, and every style the theme gives names it as its source
  (`RawTableCellStyle.source`, `RawTableStyleSource.THEME`; a style of the workbook leaves it out). Both fields are
  read-only and no request model carries a source, so a table sent back as it was read writes no style. No edit
  starts from such a read: when the user edits a table drawn with a theme, `TableEditor` reads it again without the
  theme and edits that read, whose `style` is what the workbook holds. The one exception is the preview of a theme the
  user chose to write, which the save sends as a `theme` action, never as styles. The preview is laid over a copy of
  the rows that is only drawn: the toolbar and the save read the edited rows, so a theme drawn on the screen never
  reaches the workbook through an edit. Keep it that way: never edit the rows of a read naming a theme, and never
  fold the theme into the `style` of the edited rows. The preview is the look of the table as it was read,
  matched by the address each cell was read at, so it is approximate once the edit inserts or deletes rows or
  columns; the user guide says so. An exact preview would need a dry run of the edit on the server, which is
  deliberately not done.
- **A written theme is not kept up to date.** An edit after the theme was written, such as rows or columns inserted
  or deleted, writes no theme by itself: laying the theme out needs the table compiled, and its cost grows with the
  table, so the user applies the theme again by hand once the edits are finished; the user guide says so. Only the
  overlay follows the edits, since every read lays the theme out over the table as it is then. Do not make the edits
  write the theme again on their own.
- **One look on the screen and in the workbook.** A piece of the header starts from the font of the cell on both
  sides (`ThemeStyles.fontOf`, `ThemeExcelWriter`). A look is reported through the same colour, font and border
  mappings as a style read from the workbook (`RawTableStyles`, `BorderStyle.of`), so a line the theme draws looks
  like the one the written workbook shows. `ThemeStyles` sits beside `RawTableStyles` in the `read` package, so the
  theme package never depends on the reader. The writer tells fonts apart by `PoiExcelHelper.FontAttributes`, with
  the size in twips, the colour as RGB and the theme colour it is made of (`themed`), and the RGB of a theme colour
  must be `#rrggbb`, which `ThemeColour` checks when the file is read.
- **Writing keeps what the theme does not set.** `ThemeExcelWriter` clones the style of each cell and sets only
  the attributes the theme names, keeps a cell that already has the look, and reuses the fonts the workbook has.
  Writing the theme again adds no styles or fonts, which matters because unused `cellXfs` are never compacted. A
  colour is compared as the workbook holds it (`PoiExcelHelper.toStoredRgb`): the full palette of an `.xls`
  workbook holds a colour of the theme as the nearest one it has, and a theme colour is compared by the theme colour
  and the tint it is written as (`ThemedColor.of`). A batch (`writeAll`) saves every workbook it
  reaches once, and notes the edit on each table it themes as a save of the table does (`TableWriter.recordEdit`),
  after the theme, naming the table by where it stands once written. Each property the note adds is a row inserted
  at the top of the properties with the style of the row under it, a table without properties getting them so. The
  note therefore goes through `ThemeExcelWriter.noting`, which lays out the header and the properties alone and
  themes the rows the note inserted, nothing else; the save of a `theme` edit notes it the same way
  (`RawTableWriter.recordEdit`), so the styling of the edit stands.

## Regenerating OpenAPI Goldens

Adding a description, changing an enum's wire codes, adding a `required`/`@NotBlank` field, or moving a leaked
description all change the ITEST goldens (`ITEST/itest.studio/simple/test-resources-simple/openapi.json.resp` and
`ITEST/itest.studio/multi/test-resources/000-openapi.json.resp`). `description`/`summary`/`operationId`/`version` values are masked to
`***`, so externalizing a literal to a same-text key does **not** move the golden — but enum arrays, `required`, and the
presence of a `description` key do. Rebuild the webapp (`mvn -o clean install -DskipTests -pl …webstudio`), then run the
capture-and-verify cycle (`WebStudioTest#simple+multi`, toggling `HttpClient.writeBodyTo`).

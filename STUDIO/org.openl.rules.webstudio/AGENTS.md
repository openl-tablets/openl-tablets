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
  `PropertySchemaCustomizingConverter` (module `org.openl.rules.spring.openapi`) reads `@Parameter` and sets the
  description on the **property only**.
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
- A browser that has to build an id itself uses `encodeProjectId` from `studio-ui`'s `services/projectId.ts`; a legacy
  JSF page reaches the same function as `globalThis.openl.encodeProjectId`. Never call `btoa` directly: it reads a
  string as Latin-1, so it throws above U+00FF and mis-encodes the range below it, and it emits the standard alphabet.
- **The name inside a project id is its storage folder, not the logical name declared in `rules.xml`.** A design
  project's id uses its design folder; a local-only project's id uses its workspace folder. The names differ when a
  project is renamed in `rules.xml` or when EDT loads a folder whose project declares a friendlier name. A local-only
  dependency matches that logical name because it has no Design repository identity, but the resolved dependency
  carries the folder-based id so following its link addresses the actual project (EPBDS-16518).

  A repository-backed project keeps its stable business name for dependency lookup. Do not index it by the logical
  name read from the currently selected branch: an unsaved rename there must not hide the original name from another
  branch that still contains and declares it. Branch membership remains the deciding scope as documented in
  `Docs/architecture/cross-branch-projects.md`.

  Legacy Editor hash routes are separate from REST project ids: `WebStudio.init` resolves their project segment by
  the logical `ProjectDescriptor` name. Build every legacy project and module breadcrumb link from that logical name,
  not from the `RulesProject` workspace folder.

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
  `project.identifier.ambiguous.message`, naming the ids to choose from. A body that names a project of the
  user's own workspace (`createProjectsFromWorkspace`) is not covered — a local project has no design folder to
  tell apart.

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

## Regenerating OpenAPI Goldens

Adding a description, changing an enum's wire codes, adding a `required`/`@NotBlank` field, or moving a leaked
description all change the ITEST goldens (`ITEST/itest.studio/simple/test-resources-simple/openapi.json.resp` and
`ITEST/itest.studio/multi/test-resources/000-openapi.json.resp`). `description`/`summary`/`operationId`/`version` values are masked to
`***`, so externalizing a literal to a same-text key does **not** move the golden — but enum arrays, `required`, and the
presence of a `description` key do. Rebuild the webapp (`mvn -o clean install -DskipTests -pl …webstudio`), then run the
capture-and-verify cycle (`WebStudioTest#simple+multi`, toggling `HttpClient.writeBodyTo`).

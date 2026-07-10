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

## Regenerating OpenAPI Goldens

Adding a description, changing an enum's wire codes, adding a `required`/`@NotBlank` field, or moving a leaked
description all change the ITEST goldens (`ITEST/itest.webstudio/test-resources-simple/openapi.json.resp` and
`test-resources-multi/000-openapi.json.resp`). `description`/`summary`/`operationId`/`version` values are masked to
`***`, so externalizing a literal to a same-text key does **not** move the golden — but enum arrays, `required`, and the
presence of a `description` key do. Rebuild the webapp (`mvn -o clean install -DskipTests -pl …webstudio`), then run the
capture-and-verify cycle (`WebStudioTest#simple+multi`, toggling `HttpClient.writeBodyTo`).

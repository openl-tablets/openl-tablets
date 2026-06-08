---
title: OpenL Tablets 6.1.0 Migration Notes
---

Most installations upgrade to **6.1.0** without changes. Review the items below if your projects use method filters,
pass vocabulary literals to rules, run in Multi-User mode, build with the OpenL Maven Plugin, or have clients that parse
OpenL Rule Services error responses.

## Environment

* **Java** — Java 21 remains the minimum supported version, unchanged from 6.0.0. The application now also builds and
  runs on Java 25 and Java 26. No action is required for installations already on Java 21 or later.

## Rules Authors

### Method filters now use glob syntax

Include and exclude method filters changed from regular expressions to glob patterns, and moved from individual modules
to a single project-level **Exposed Methods** list. Use `*` to match any sequence of characters and `?` to match exactly
one character.

In OpenL Studio, open the project and use the **Migrate Method Filters** action, which converts existing module-level
filters to project-level **Exposed Methods** and removes the original entries. The action appears only while a project
still has filters to migrate.

Alternatively, migrate from the command line with the OpenL Maven Plugin:

```bash
mvn openl:migrate
```

Regular expressions that cannot be expressed as a glob pattern are not carried over and must be re-entered manually as
glob patterns.

### Vocabulary values are validated at compile time

Literals passed to a rule, datatype constructor, or spreadsheet step are now checked against the parameter's
vocabulary (domain) when the project compiles. A value outside the vocabulary now produces a compile error instead of
failing at runtime.

* Correct any reported values so they belong to the declared vocabulary.
* The `Datatype Name<Type[]>` form — an array as a vocabulary target type — is no longer supported. Replace it with a
  supported declaration.

### Arithmetic function results

The `mod`, `quotient`, `remainder`, and `floorDiv` functions now return consistent results across numeric types and for
large values. Rules that depended on the previous inconsistent behavior may produce different — and now correct —
results. Review tests that exercise these functions.

## Developers

### OpenL Rule Services error responses are sanitized

API error responses no longer expose Java types, package names, framework details, or generated OpenL class names.
Validation errors return concise, field-level messages, and unexpected errors return a generic message.

* Update any client that parsed Java or framework details out of error bodies.
* To correlate a client error with server logs, enable a request identifier with the `log.request-id.header` property;
  the value is echoed back as a response header and recorded in the logs.

### OpenL Maven Plugin: deployment artifacts

Deployment artifacts are generated automatically when a project depends on other OpenL projects, and each dependency's
`rules-deploy.xml` is replaced with an empty-publishers stub so it is not published twice.

* Remove `<deploymentPackage>true</deploymentPackage>` from the plugin configuration — it now has no effect and logs a
  warning.
* Delete any "deployment-only" wrapper module that existed solely to suppress publication of an embedded dependency.

### OpenL Maven Plugin: separate tests folder

`mvn package` now excludes the `tests/` folder from the main artifact and produces a separate artifact with the `tests`
classifier. This keeps test workbooks out of OpenL Rule Services deployments.

* No action is required for projects without a `tests/` folder.
* If a project explicitly listed modules under `tests/` in `rules.xml`, rely on automatic module discovery so the main
  artifact compiles without those files.
* To run the tests alongside the rules — for example in integration testing — deploy the `tests`-classified artifact
  next to the main one.

### pom.xml-less OpenL Maven projects (optional)

Existing `pom.xml`-based projects keep working unchanged. To remove per-project `pom.xml` files in a repository, add an
anchor `pom.xml` that declares `openl-maven-plugin` with `<extensions>true</extensions>` and convert the projects:

```bash
mvn openl:pomless -Dopenl.pomless.dryRun=false
```

The conversion runs as a dry run by default. Projects that cannot be converted cleanly are reported with the reason and
left unchanged for manual review.

## Administrators

### Groups in Multi-User mode

Multi-User mode no longer assigns groups automatically and no longer performs background group assignment. Configure
access through ACL roles at the repository and project levels instead. Other authentication modes are unaffected.

### Protected-branch bypass (optional)

To let users with the **Manager** role merge into protected branches from within OpenL Studio, enable the bypass:

```properties
security.allow-bypass-protected-branches = true
```

The setting is disabled by default. When enabled, an eligible Manager confirms a bypass per request with a `force` flag;
eligibility follows the Manager role's project- or repository-level scope. Without the flag, an eligible user receives a
`409` response with code `openl.error.409.protected.branch.bypass.required`, and a non-eligible user always receives
`403`.

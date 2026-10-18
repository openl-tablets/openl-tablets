# OpenL Tablets — OpenL Maven Plugin (`openl-maven-plugin`)

Integrates OpenL rules compilation into the Maven lifecycle.

```xml
<!-- inside the project's pom.xml -->

<!-- `openl` packaging is a zip file. -->
<packaging>openl</packaging>
<build>
    <plugins>
        <plugin>
            <groupId>org.openl.rules</groupId>
            <artifactId>openl-maven-plugin</artifactId>
            <version>${org.openl.version}</version>
            <extensions>true</extensions>   <!-- activates openl packaging + lifecycle bindings -->
        </plugin>
    </plugins>
</build>

    <!-- All runtime scoped dependencies will be added in lib/*.jar folder on the package phase. -->
    <!-- optional, provided and test scoped dependencies are not included in the final package. -->
<dependencies>
    <dependency>   <!-- OpenL project: zip with rules.xml -->
        <groupId>com.example</groupId>
        <artifactId>shared-data-project</artifactId>
        <version>1.0.0</version>
        <optional>true</optional>
        <type>zip</type>
    </dependency>
    <dependency>   <!-- Java library which will be added in the final OpenL packaged project. -->
        <groupId>com.example</groupId>
        <artifactId>common-library</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

| Goal            | Default Phase      | Purpose                                                       |
|-----------------|--------------------|---------------------------------------------------------------|
| `sync-versions` | `validate`         | Actualize dependency versions in `rules.xml`                  |
| `compile`       | `compile`          | Compile OpenL rules; validate syntax and types                |
| `test`          | `test`             | Execute Test tables; generate JUnit XML + XLSX reports        |
| `verify`        | `verify`           | Run all validations including OpenAPI reconciliation          |
| `generate`      | `generate-sources` | Generate Java interfaces and datatype classes from rules      |
| `package`       | `package`          | Package rules as a ZIP artifact for deployment                |
| `deploy`        | `deploy`           | Deploy the packaged artifact to an OpenL Tablets repository   |
| `migrate`       |                    | Migrates or modernizes OpenL Project and Maven modules [3]    |
| `prepare-bom`   | `package`          | Generates BOM with all OpenL projects [1]                     |
| `prepare-pom`   | `validate`         | Writes `openl-pom.xml` for install/deploy [1]                 |
| `pomless`       |                    | Converts classic pom-ful OpenL projects to pom-less [2]       |

Notes:

- **[1]** — Applicable to pom-less projects only.
- **[2]** — Aggregator goal, dry-run by default. On apply (`-Dopenl.pomless.dryRun=false`) it deletes cleanly-convertible
  projects' `pom.xml` and edits each project's **anchor** — the nearest non-OpenL aggregator pom that contains it,
  never a higher parent — to declare the plugin (`<extensions>true</>` + version), set the max
  `<dependenciesThreshold>`, and hoist non-OpenL deps, then prunes the converted modules from `<modules>`. Blocked
  projects (per-project config other than `dependenciesThreshold`, `deploymentPackage` and `deploymentName`,
  executions, profiles, …) are left untouched and reported. Pom edits re-serialise the model — review the diff.
- **[3]** — Rewrites `rules.xml`, `rules-deploy.xml`, Groovy and removed-function calls, drops no-effect plugin params
  in `pom.xml`; a commit per step. `openl:help -Dgoal=migrate -Ddetail` lists steps, `-Dopenl.migrate.migrators` picks.

> [!Note]
> `generate` creates a Java interface from the project's `<exposed-methods>` filter, for type-safe rule invocation.

## Supplementary artifacts

`openl:package` attaches supplementary artifacts next to the main archive:

| Classifier   | When attached                                                               |
|--------------|-----------------------------------------------------------------------------|
| `classes`    | `target/classes` is not empty — built as `<finalName>-classes.jar`          |
| `tests`      | The OpenL source folder contains `tests/**`                                 |
| `deployment` | Depends on other OpenL projects and `rules-deploy.xml` publishers not empty |

- The `classes` jar is also embedded into the zip archive as `lib/<finalName>.jar` (runtime classpath);
  depend on it with `<classifier>classes</classifier>`.
- Not built when a main jar artifact already exists or for `openl-jar` packaging (classes go to the jar root).
- The `deployment` archive is `<finalName>-deployment.zip`, named `<finalName>` in its `deployment.yaml`; `openl:deploy`
  uploads it instead of the main archive. The deprecated `deploymentName` has no effect and logs a warning.

## Packaging workbooks as Markdown

With `<markdown>true</markdown>` in the plugin configuration (or `-Dopenl.markdown=true`), the project is packaged
with its workbooks written as Markdown. The setting is off by default.

- `openl:compile` compiles the project as it is, then writes a copy of it into `target/openl-markdown`
  (`markdownDirectory`): every module read from a workbook becomes a module folder of Markdown files, a table to a
  file, at the path of the workbook without its extension, and the `rules.xml` of the copy names the folders.
- The tables of a sheet go into the folder of its name. A character a name of OpenL Studio cannot hold, such as `%`
  or `;`, is written as `_`, and the name loses a space at either end and a dot at its end.
- A file is named after its table, whether the table compiles or not: the rule, the type or the data it declares,
  `rate.md`, and a test with no name of its own after the rule it tests and its kind, `rateTest.md`. A name another
  table of the folder holds gets a number, `rate1.md`, `rate2.md`, and these files are read after `rate.md` in the
  order of the sheet.
- `openl:test` runs the tests of the project as it is; `openl:package` packages the copy instead of the sources, and
  `openl:verify` deploys it.
- A workbook holding a table no Markdown file can hold stays a workbook in the copy, and the build log names the
  table and the reason: a table written in parts; a table whose layout has no Markdown form. The log also gives the
  size of each workbook and of its folder.
- The indent of a cell of a TBasic or a ColumnMatch table, which builds the table, is written as two `&nbsp;` before
  the text for each level, `&nbsp;&nbsp;SET`.
- The Environment tables of a workbook are joined in the one `Environment.md` of its module, and so are its Constants
  tables in `Constants.md`; a Properties table is written as any other table, in the folder of its sheet.
- A Markdown file keeps no formatting, comments or formulas: a cell keeps the value its formula computed. A table of
  no kind OpenL knows, such as a note, is kept as text in the `notes.md` file of its sheet.
- A value is written as its text, and a whole number, a boolean and a date read back as the values of the types the
  cells of the workbook hold: a `Spreadsheet` step keeps the type its value took from its cell, and its result keeps
  its fields, with no type named in the file.

## Pom-less OpenL projects

Anchor `pom.xml`:

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>rules-parent</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <build>
        <plugins>
            <plugin>
                <groupId>org.openl.rules</groupId>
                <artifactId>openl-maven-plugin</artifactId>
                <version>${org.openl.version}</version>
                <extensions>true</extensions>
            </plugin>
        </plugins>
    </build>
</project>
```

It discovers every `rules.xml` folder under its basedir as a `<packaging>openl</packaging>` module.
Each OpenL folder holds only `rules.xml` + the standard layout ([project-structure.md](project-structure.md)).
`mvn install` from the anchor synthesises a model per project and installs the `.zip` + pom.

- Multiple anchors allowed (top-level + submodule, or a submodule alone).
- Anchor packaging: any except `openl`/`openl-jar` (those are OpenL projects themselves, not rescanned).

Coordinates derive from the folder path under the anchor:

- `artifactId` — folder name
- `groupId` — anchor `<groupId>` + dotted intermediate path
- `version` — inherited from the anchor

Example: anchor `com.example` + `pricing/auto/rules.xml` → `com.example.pricing:auto:<anchor-version>`.

### Inheritance from the anchor

Synthesised with the anchor as `<parent>`; inherits via Maven:

- `<build><plugins>` — anchor plugins (e.g. source generators on `generate-sources`) fire on every pom-less project;
  `<inherited>false</inherited>` opts one out.
- `<build><pluginManagement>` — config defaults for plugins the project declares (i.e. `openl-maven-plugin`).
- `<properties>`, `<dependencyManagement>`, `<distributionManagement>`, `<repositories>` — visible during the build.

`flatten-maven-plugin` is auto-skipped on pom-less projects: they expose `rules.xml`, not a pom, so it can't build
their model — `openl:prepare-pom` produces the flattened install/deploy pom instead. The skip switch (`flatten.skip`)
exists only since 1.6.0, so an older inherited version is bumped to it for the in-memory build.

Installed pom strips `<parent>` and `<build>` → flat artefact pom, no anchor lineage.

### Cross-project dependencies

A `rules.xml` `<dependency>` becomes a Maven dependency:

- **Explicit** — `<mavenArtifact>g:a[:type[:classifier]]:v</mavenArtifact>` (Aether order: version last; default type
  `zip`); `jar`-type entries get `<optional>true</optional>` (self-contained in `lib/`, not inherited downstream).
- **By name** — `<name>` only; resolved against reactor sibling OpenL projects, emitted with `<type>zip</type>`.

`<mavenArtifact>` wins when both present. Every `<dependency>` needs a `<name>` (OpenL loader requirement).

A `<mavenArtifact>` resolving to a reactor OpenL artefact (pom-less under any anchor, or a classic openl module) takes
the reactor version, whatever the coordinate declares. Precedence: anchor `<dependencyManagement>` > reactor-sibling
version > literal `<mavenArtifact>` version. Management is the opt-out — pin a sibling to a published version on
purpose.

openl:sync-versions (bound to validate) rewrites each `<mavenArtifact>` version in rules.xml in place to that resolved
value — managed coordinates and reactor siblings alike, same precedence.

### Discovery rules

- Folders with a real `pom.xml` are NOT discovered — classic projects untouched.
- `target/` and hidden dirs skipped.
- Scanner stops at the first `rules.xml` — nested OpenL projects need a separate anchor.
- Folder names must match `[A-Za-z0-9._-]+`; invalid paths fail the build.

### Generated artifacts

The synthesised pom stays in memory; the project root is never written. `openl:prepare-pom` (validate phase) writes a
flat `target/openl-pom.xml` for install/deploy. The `.zip` lands in the same `target/`, installed with the derived GAV.

### Deployment BOM

`openl:prepare-bom` (auto-bound, `package` phase) attaches a BOM to the anchor — classifier `deployment-bom`, type
`pom`, same GA — listing every OpenL project under the anchor (pom-less + classic `openl` modules):

- `<dependencyManagement>` — each project's main entry, plus a `<classifier>deployment</classifier>` entry for each
  that ships a `*-deployment.zip` (has an OpenL dep AND non-empty `<publishers>`). All versions managed; opt in
  explicitly.
- `<dependencies>` — deployable projects only (non-empty `<publishers/>`): deployment-classifier entry for OpenL-dep
  projects, main entry for leaves. Empty-publisher projects are omitted here but stay in `<dependencyManagement>`.

Consumer — canonical BOM (`scope=import`, then list deps without versions):

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>rules-parent</artifactId>
    <version>1.0.0</version>
    <classifier>deployment-bom</classifier>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

One-shot bundle (one dep pulls every OpenL artefact transitively): same block, drop `<scope>import</scope>`.

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
| `compile`       | `compile`          | Compile OpenL rules; validate syntax and types                |
| `test`          | `test`             | Execute Test tables; generate JUnit XML + XLSX reports        |
| `verify`        | `verify`           | Run all validations including OpenAPI reconciliation          |
| `generate`      | `generate-sources` | Generate Java interfaces and datatype classes from rules      |
| `package`       | `package`          | Package rules as a ZIP artifact for deployment                |
| `deploy`        | `deploy`           | Deploy the packaged artifact to OpenL Studio or a repository  |
| `migrate`       |                    | Migrates or modernizes OpenL Project and Maven modules        |
| `prepare-bom`   | `package`          | Generates BOM with all OpenL projects [1]                     |
| `prepare-pom`   | `install`          | Writes `openl-pom.xml` for install/deploy [1]                 |

[1]: It is applicable to pom-less project only

> [!Note]
> `generate` creates a Java interface from the project's `<exposed-methods>` filter, for type-safe rule invocation.

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

### Discovery rules

- Folders with a real `pom.xml` are NOT discovered — classic projects untouched.
- `target/` and hidden dirs skipped.
- Scanner stops at the first `rules.xml` — nested OpenL projects need a separate anchor.
- Folder names must match `[A-Za-z0-9._-]+`; invalid paths fail the build.

### Generated artifacts

The synthesised pom stays in memory; the project root is never written. `openl:prepare-pom` (install phase) writes a
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

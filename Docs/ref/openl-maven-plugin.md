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
            <!-- `extensions: true` activates `packaging: openl` and binds  goals to the default Maven lifecycle phases. -->
            <extensions>true</extensions>
        </plugin>
    </plugins>
</build>

<!-- All runtime scoped dependencies will be added in lib/*.jar folder on the package phase. -->
<!-- optional, provided and test scoped dependencies are not included in the final package. -->
<dependencies>
    <!-- Dependency on OpenL Project. Must contain rules.xml or rules-deploy.xml file. -->
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>shared-data-project</artifactId>
        <version>1.0.0</version>
        <optional>true</optional>
        <type>zip</type>
    </dependency>
    <!-- Regular Java library dependency which will be added in the final OpenL packaged project. -->
    <dependency>
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
| `migrate-list`  |                    | List available `migrate` ids and their commit messages        |

> [Note:]
> The `generate` goal creates a Java interface matching the project's `<exposed-methods>` filter.
> Use it to get type-safe rule invocation from Java code.

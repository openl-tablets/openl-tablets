# Util Module - Tools & Utilities Conventions

**Module**: Util (Tools, Maven Plugins, Utilities)
**Location**: `/home/user/openl-tablets/Util/`
**Version**: 6.0.0-SNAPSHOT
**Last Updated**: 2025-11-05

---

## Module Purpose

The Util module provides **development tools and utilities** for OpenL Tablets, including:
- Maven plugin for OpenL compilation
- Project archetypes for quick starts
- OpenAPI code generation and parsing
- Excel file builder utilities
- Performance profiler
- OpenTelemetry integration
- YAML support

**Target Audience**: Developers building applications with OpenL Tablets

---

## Submodules Overview

| Submodule | Purpose | Type | Usage |
|-----------|---------|------|-------|
| **`openl-maven-plugin`** | Maven plugin for OpenL | Build Tool | 🔧 Development |
| **`openl-project-archetype`** | Full project template | Maven Archetype | 🏗️ Project setup |
| **`openl-simple-project-archetype`** | Simple project template | Maven Archetype | 🏗️ Project setup |
| **`openl-openapi-model-scaffolding`** | OpenAPI code generator | Code Generator | 🔧 Development |
| **`openl-openapi-parser`** | OpenAPI spec parser | Library | 📚 Integration |
| **`openl-excel-builder`** | Excel file builder | Library | 📚 Testing |
| **`openl-yaml`** | YAML support | Library | 📚 Configuration |
| **`org.openl.rules.profiler`** | Performance profiler | Library | 📊 Monitoring |
| **`openl-rules-opentelemetry`** | Observability | Library | 📊 Monitoring |

---

## Technology Stack

### Build Tools
- **Maven**: 3.9.9+
- **Maven Plugin API**: 3.9.8

### Code Generation
- **JCodeModel**: 4.0.0 (Java code generation)
- **Apache Velocity**: 2.3 (Template engine)
- **Swagger Parser**: 2.1.26 (OpenAPI parsing)

### Utilities
- **Apache POI**: 5.3.0 (Excel manipulation)
- **SnakeYAML**: 2.3 (YAML parsing)
- **OpenTelemetry**: 1.48.0 (Observability)

---

## 1. Maven Plugin (`openl-maven-plugin`)

### Purpose

Provides Maven goals for:
- Compiling OpenL rule projects
- Generating Java interfaces from Excel rules
- Validating rule projects
- Running rule tests

### Usage

**Add to your pom.xml**:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.openl</groupId>
            <artifactId>openl-maven-plugin</artifactId>
            <version>6.0.0-SNAPSHOT</version>
            <configuration>
                <sourceDirectory>src/main/openl</sourceDirectory>
                <outputDirectory>target/generated-sources/openl</outputDirectory>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>generate</goal>
                        <goal>compile</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Available Goals

#### `openl:generate`
**Description**: Generates Java interface from Excel rules

**Parameters**:
- `sourceDirectory` - Location of Excel files (default: `src/main/openl`)
- `outputDirectory` - Where to generate Java files (default: `target/generated-sources/openl`)
- `interfaceClass` - Name of interface to generate
- `displayName` - Module display name

**Example**:
```bash
mvn openl:generate \
  -DsourceDirectory=rules \
  -DinterfaceClass=com.example.IMyRules
```

#### `openl:compile`
**Description**: Compiles OpenL rules and validates them

**Parameters**:
- `failOnError` - Fail build on compilation errors (default: `true`)
- `showWarnings` - Display warnings (default: `true`)

**Example**:
```bash
mvn openl:compile
```

#### `openl:test`
**Description**: Runs rule tests defined in Excel

**Parameters**:
- `testSourceDirectory` - Location of test files
- `skipTests` - Skip test execution

**Example**:
```bash
mvn openl:test
```

### Configuration Options

```xml
<configuration>
    <!-- Source Configuration -->
    <sourceDirectory>src/main/openl</sourceDirectory>
    <outputDirectory>target/generated-sources/openl</outputDirectory>

    <!-- Interface Generation -->
    <interfaceClass>com.example.IMyRules</interfaceClass>
    <displayName>MyRules</displayName>
    <isProvideRuntimeContext>true</isProvideRuntimeContext>
    <isProvideVariations>false</isProvideVariations>

    <!-- Compilation Options -->
    <failOnError>true</failOnError>
    <showWarnings>true</showWarnings>

    <!-- Advanced Options -->
    <externalDependencies>
        <dependency>
            <name>my-datatypes</name>
            <className>com.example.CustomDataType</className>
        </dependency>
    </externalDependencies>
</configuration>
```

### Developing the Plugin

**Key Classes**:
- `GenerateMojo` - Code generation goal
- `CompileMojo` - Compilation goal
- `TestMojo` - Test execution goal

**Example Mojo**:
```java
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = "sourceDirectory", defaultValue = "src/main/openl")
    private File sourceDirectory;

    @Parameter(property = "interfaceClass", required = true)
    private String interfaceClass;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating interface: " + interfaceClass);

        try {
            // Generate interface from Excel rules
            generateInterface();

            // Add generated sources to project
            project.addCompileSourceRoot(outputDirectory.getPath());

        } catch (Exception e) {
            throw new MojoExecutionException("Generation failed", e);
        }
    }
}
```

---

## 2. Project Archetypes

### Purpose
Provide Maven archetypes for quickly bootstrapping OpenL projects.

### openl-project-archetype (Full Template)

**Features**:
- Complete project structure
- Sample Excel rules
- Spring Boot integration
- Web service setup
- Docker configuration
- CI/CD templates

**Usage**:
```bash
mvn archetype:generate \
  -DarchetypeGroupId=org.openl \
  -DarchetypeArtifactId=openl-project-archetype \
  -DarchetypeVersion=6.0.0-SNAPSHOT \
  -DgroupId=com.example \
  -DartifactId=my-rules-project \
  -Dversion=1.0.0-SNAPSHOT \
  -Dpackage=com.example.rules
```

**Generated Structure**:
```
my-rules-project/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/rules/
│   │   │       └── Application.java
│   │   ├── openl/
│   │   │   └── rules/
│   │   │       └── Tutorial.xlsx
│   │   └── resources/
│   │       ├── application.yml
│   │       └── rules-deploy.xml
│   └── test/
│       └── java/
│           └── com/example/rules/
│               └── RulesTest.java
├── Dockerfile
└── README.md
```

### openl-simple-project-archetype (Minimal Template)

**Features**:
- Minimal project structure
- Basic Excel rules
- No Spring Boot (standalone)
- Suitable for learning/testing

**Usage**:
```bash
mvn archetype:generate \
  -DarchetypeGroupId=org.openl \
  -DarchetypeArtifactId=openl-simple-project-archetype \
  -DarchetypeVersion=6.0.0-SNAPSHOT \
  -DgroupId=com.example \
  -DartifactId=simple-rules
```

---

## 3. OpenAPI Tools

### openl-openapi-parser

**Purpose**: Parse and validate OpenAPI 3.0 specifications

**Usage**:
```java
import org.openl.rules.openapi.OpenAPIParser;

// Parse OpenAPI spec
OpenAPIParser parser = new OpenAPIParser();
OpenAPI spec = parser.parse("path/to/openapi.yaml");

// Access paths
spec.getPaths().forEach((path, pathItem) -> {
    System.out.println("Path: " + path);
    pathItem.getOperations().forEach((httpMethod, operation) -> {
        System.out.println("  " + httpMethod + ": " + operation.getOperationId());
    });
});
```

### openl-openapi-model-scaffolding

**Purpose**: Generate Java data model classes from OpenAPI schemas

**Usage**:
```java
import org.openl.rules.openapi.scaffolding.ModelGenerator;

ModelGenerator generator = new ModelGenerator();
generator.setPackageName("com.example.model");
generator.setOutputDirectory(new File("target/generated-sources"));

// Generate from OpenAPI spec
generator.generate("path/to/openapi.yaml");
```

**Generated Classes**:
```java
// Generated from OpenAPI schema
package com.example.model;

public class Driver {
    private Integer age;
    private String name;
    private Integer yearsLicensed;

    // Getters, setters, constructors
}
```

**Configuration**:
```xml
<plugin>
    <groupId>org.openl</groupId>
    <artifactId>openl-openapi-model-scaffolding</artifactId>
    <version>6.0.0-SNAPSHOT</version>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <inputSpec>src/main/resources/api.yaml</inputSpec>
                <packageName>com.example.model</packageName>
                <outputDirectory>target/generated-sources</outputDirectory>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## 4. Excel Builder (`openl-excel-builder`)

### Purpose
Programmatically create Excel files for testing or rule generation.

### Usage

**Creating a Simple Excel File**:
```java
import org.openl.rules.excel.builder.ExcelBuilder;

ExcelBuilder builder = new ExcelBuilder();

// Create decision table
builder.addTable("CalculatePremium")
    .addColumn("Driver Age", "Integer")
    .addColumn("Premium", "Double")
    .addRow(18, 2500.0)
    .addRow(25, 1500.0)
    .addRow(35, 1000.0)
    .addRow(65, 1200.0);

// Save to file
builder.save("rules/Insurance.xlsx");
```

**Creating Data Tables**:
```java
builder.addDataTable("Driver")
    .addColumn("name", "String")
    .addColumn("age", "Integer")
    .addColumn("yearsLicensed", "Integer")
    .addRow("John Doe", 25, 7)
    .addRow("Jane Smith", 35, 15);
```

**Creating Test Tables**:
```java
builder.addTestTable("TestCalculatePremium")
    .addInputColumn("driverAge", "Integer")
    .addOutputColumn("premium", "Double")
    .addTestCase(25, 1500.0)
    .addTestCase(35, 1000.0);
```

**Use Case**: Generating test data for integration tests
```java
@BeforeEach
void setUp() {
    // Generate test rules
    ExcelBuilder builder = new ExcelBuilder();
    builder.addTable("MyRule")
        .addColumn("input", "Integer")
        .addColumn("output", "String")
        .addRow(1, "One")
        .addRow(2, "Two");

    builder.save("target/test-rules.xlsx");
}
```

---

## 5. Performance Profiler (`org.openl.rules.profiler`)

### Purpose
Profile rule execution performance to identify bottlenecks.

### Usage

**Enable Profiling**:
```java
import org.openl.rules.profiler.Profiler;
import org.openl.rules.profiler.ProfilerConfiguration;

// Configure profiler
ProfilerConfiguration config = new ProfilerConfiguration();
config.setEnabled(true);
config.setSamplingInterval(100); // ms
config.setOutputFile("profiler-results.txt");

// Enable profiling
Profiler.configure(config);

// Execute rules
MyRules rules = factory.newInstance();
rules.calculate(input);

// Get profiling results
ProfilerResults results = Profiler.getResults();
results.print(System.out);
```

**Output**:
```
Rule Profiling Results
======================
Method: calculatePremium
  Invocations: 1,000,000
  Total Time: 5,234 ms
  Avg Time: 0.005 ms
  Min Time: 0.002 ms
  Max Time: 1.234 ms

Method: validateDriver
  Invocations: 1,000,000
  Total Time: 1,234 ms
  Avg Time: 0.001 ms
```

**Programmatic Analysis**:
```java
ProfilerResults results = Profiler.getResults();

// Find slowest methods
List<MethodProfile> slowest = results.getSlowestMethods(10);
for (MethodProfile profile : slowest) {
    System.out.printf("%s: %.3f ms avg%n",
        profile.getMethodName(),
        profile.getAverageTime());
}
```

---

## 6. OpenTelemetry Integration (`openl-rules-opentelemetry`)

### Purpose
Integrate OpenL with OpenTelemetry for distributed tracing and metrics.

### Usage

**Add Dependency**:
```xml
<dependency>
    <groupId>org.openl</groupId>
    <artifactId>openl-rules-opentelemetry</artifactId>
    <version>6.0.0-SNAPSHOT</version>
</dependency>
```

**Configure Tracing**:
```java
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.openl.rules.opentelemetry.OpenLTelemetry;

// Initialize OpenTelemetry
OpenLTelemetry.initialize();

// Tracing is automatic for rule methods
MyRules rules = factory.newInstance();
rules.calculate(input); // Automatically traced
```

**Custom Spans**:
```java
Tracer tracer = GlobalOpenTelemetry.getTracer("openl-rules");

Span span = tracer.spanBuilder("custom-operation").startSpan();
try (Scope scope = span.makeCurrent()) {
    // Your code here
    span.setAttribute("input.value", input.toString());
} finally {
    span.end();
}
```

**Metrics**:
```java
// Metrics are automatically collected
// - Rule invocation count
// - Rule execution duration
// - Error count
```

**Export to Jaeger/Zipkin**:
```yaml
# application.yml
otel:
  exporter:
    jaeger:
      endpoint: http://localhost:14250
  service:
    name: openl-ruleservice
```

---

## 7. YAML Support (`openl-yaml`)

### Purpose
Read/write YAML configuration files.

### Usage

**Reading YAML**:
```java
import org.openl.rules.yaml.YamlReader;

YamlReader reader = new YamlReader();
Map<String, Object> config = reader.read("config.yaml");

String serviceName = (String) config.get("serviceName");
Integer port = (Integer) config.get("port");
```

**Writing YAML**:
```java
import org.openl.rules.yaml.YamlWriter;

Map<String, Object> config = new HashMap<>();
config.put("serviceName", "MyService");
config.put("port", 8080);

YamlWriter writer = new YamlWriter();
writer.write(config, "config.yaml");
```

**Type-Safe Reading**:
```java
public class ServiceConfig {
    private String serviceName;
    private Integer port;
    // Getters, setters
}

YamlReader reader = new YamlReader();
ServiceConfig config = reader.read("config.yaml", ServiceConfig.class);
```

---

## Testing Requirements

### Maven Plugin Tests

**Integration Tests**:
```java
@ExtendWith(MavenITExtension.class)
class GenerateMojoIT {

    @TempDir
    Path projectDir;

    @Test
    void testGenerate() throws Exception {
        // Set up test project
        Files.copy(
            Path.of("src/test/resources/test-project"),
            projectDir,
            REPLACE_EXISTING
        );

        // Run Maven goal
        MavenExecutionResult result = maven.withGoal("openl:generate")
            .execute();

        // Assert
        assertTrue(result.isSuccessful());
        assertTrue(Files.exists(
            projectDir.resolve("target/generated-sources/IMyRules.java")
        ));
    }
}
```

### Archetype Tests

**Test Archetype Generation**:
```bash
# Generate from archetype
mvn archetype:generate \
  -DarchetypeArtifactId=openl-simple-project-archetype \
  -DgroupId=test \
  -DartifactId=test-project \
  -Dversion=1.0 \
  -DinteractiveMode=false

# Build generated project
cd test-project
mvn clean install

# Verify it builds successfully
echo $? # Should be 0
```

---

## Common Tasks

### Task: Create a New Maven Plugin Goal

**Steps**:
1. Create new Mojo class extending `AbstractMojo`
2. Annotate with `@Mojo(name = "mygoal")`
3. Add parameters with `@Parameter`
4. Implement `execute()` method
5. Add integration test
6. Document in plugin documentation

### Task: Add New OpenAPI Generator Feature

**Steps**:
1. Update OpenAPI parser to extract new metadata
2. Update code generation templates
3. Add configuration option
4. Add unit tests
5. Add integration test with sample OpenAPI spec
6. Document new feature

### Task: Create New Project Archetype

**Steps**:
1. Create template project structure
2. Convert to archetype using `archetype:create-from-project`
3. Customize `archetype-metadata.xml`
4. Add to parent POM
5. Test archetype generation
6. Document usage

---

## Best Practices

### Maven Plugin Development
- ✅ Use meaningful parameter names
- ✅ Provide good default values
- ✅ Validate all inputs
- ✅ Log useful messages
- ✅ Handle errors gracefully
- ⚠️ Don't pollute user's console with debug logs
- ⚠️ Test with different Maven versions

### Code Generation
- ✅ Generate clean, readable code
- ✅ Add copyright headers
- ✅ Generate JavaDoc comments
- ✅ Follow Java naming conventions
- ⚠️ Don't generate unnecessary code
- ⚠️ Handle edge cases (null values, special characters)

### Performance
- ✅ Cache compiled classes when possible
- ✅ Use incremental compilation
- ✅ Parallelize when appropriate
- ⚠️ Don't hold references to large objects
- ⚠️ Clean up temporary files

---

## Documentation References

- **Module Analysis**: [Util Overview](../docs/analysis/studio-wsfrontend-util-overview.md)
- **Root Conventions**: [/CLAUDE.md](../CLAUDE.md)
- **Architecture**: [docs/architecture/](../docs/architecture/)
- **Maven Plugin Guide**: [Maven Plugin Development](https://maven.apache.org/plugin-developers/)

---

## When in Doubt

1. **Check existing plugins** - Follow established patterns
2. **Keep it simple** - Don't over-engineer utilities
3. **Document well** - Utilities need good docs
4. **Test thoroughly** - Tools are used by many developers
5. **Version carefully** - Breaking changes affect user builds

---

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT
**Target Audience**: OpenL Tablets developers and integrators

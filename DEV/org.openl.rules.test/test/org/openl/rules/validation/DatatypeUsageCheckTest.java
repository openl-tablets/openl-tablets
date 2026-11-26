package org.openl.rules.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.tools.ToolProvider;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.CompiledOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLLintMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.table.xls.XlsUrlParser;

class DatatypeUsageCheckTest {

    /**
     * Two modules: Model declares the types, Rating uses them in rules, a Data table and Test tables. Each field is
     * used in another way: a formula, a column title of a Smart rule, a return of a Smart rule, a filter of an array
     * by a variable or by the names of the fields, an assignment, a named argument of a constructor, a getter, a
     * setter, the constructor that takes every field, a column of a Data table, a column of a Test table, a property
     * of the runtime context. A datatype is used in an expression of a TBasic table only, a vocabulary only as the type
     * of a field of the runtime context, another only as a type an expression names. A datatype is the type of a field
     * no table uses only, another the parent of a datatype no table refers to only. Some fields, datatypes and a
     * vocabulary are used by no table.
     */
    private static final String PROJECT = "test-resources/org/openl/rules/validation/datatype-usage";

    @Test
    void findsWhatNoTableOfTheProjectUses() throws Exception {
        var found = check(false);

        // Every type is declared by Model, and Rating uses them.
        assertEquals(List.of("Model"), found.keySet().stream().map(XlsModuleOpenClass::getModuleName).toList());
        // A field an inherited one stands beside is told on its own datatype; the fields of a type no table refers
        // to are not told one by one. Sticker and Rebate stay while a field and a child name them, so they are not
        // told, and their fields no table uses are. Node naming itself is not used. Policy.lob fills the runtime
        // context, so its vocabulary is used too. Note is used by a TBasic algorithm that reads none of its fields.
        assertEquals(List.of("Datatype 'Discount' is not used in the current project.",
                        "Datatype 'Node' is not used in the current project.",
                        "Datatype 'Voucher' is not used in the current project.",
                        "Field 'agentCode' of datatype 'Policy' is not used in the current project.",
                        "Field 'amount' of datatype 'Rebate' is not used in the current project.",
                        "Field 'email' of datatype 'Driver' is not used in the current project.",
                        "Field 'hobby' of datatype 'Adult' is not used in the current project.",
                        "Field 'limit' of datatype 'Coverage' is not used in the current project.",
                        "Field 'premium' of datatype 'Policy' is not used in the current project.",
                        "Field 'sticker' of datatype 'Policy' is not used in the current project.",
                        "Field 'text' of datatype 'Note' is not used in the current project.",
                        "Field 'text' of datatype 'Sticker' is not used in the current project.",
                        "Vocabulary 'Region' is not used in the current project."),
                summaries(found));
        // Each is an information message of the linter
        assertTrue(messages(found).stream().allMatch(OpenLLintMessage.class::isInstance));
        assertTrue(messages(found).stream().allMatch(message -> message.getSeverity() == Severity.INFO));
    }

    @Test
    void pointsAtTheNameOfAFieldAndAtTheTableOfAType() throws Exception {
        var messages = messages(check(false));

        // The rows of Policy: its header, its description, the titles of the columns, then a field a row.
        var premium = location(messages, "Field 'premium'");
        assertTrue(premium.getWbName().endsWith("Policy.md"), premium.getWbName());
        assertEquals("B7", premium.getCell());
        var discount = location(messages, "Datatype 'Discount'");
        assertTrue(discount.getWbName().endsWith("Discount.md"), discount.getWbName());
        assertEquals("A1", discount.getCell());
        var region = location(messages, "Vocabulary 'Region'");
        assertTrue(region.getWbName().endsWith("Region.md"), region.getWbName());
        assertEquals("A1", region.getCell());
        // The cell of a transient field holds the name and its suffix: only the name is marked
        var email = messageOf(messages, "Field 'email'").getSourceRange();
        assertNotNull(email);
        assertEquals("email", email.code().substring(email.start(), email.end()));
    }

    /** An error of the whole project, such as one of a check made after the compilation, names no place of a table. */
    @Test
    void checksAProjectWithAnErrorOfNoPlace() throws Exception {
        var compiled = compile(Path.of(PROJECT), null, false);
        var error = new OpenLErrorMessage(new OpenLCompilationException("The project does not match its OpenAPI file."));
        var withError = new CompiledOpenClass(compiled.getOpenClassWithErrors(), List.of(error), List.of(error));

        assertEquals(summaries(check(false)), summaries(DatatypeUsageCheck.check(withError, module -> true)));
    }

    /**
     * The compiled module is left out of the check, as a module of another project is: its error stops nothing of the
     * modules it depends on that are checked.
     */
    @Test
    void checksTheModulesItKeepsThoughTheCompiledModuleLeftOutHasAnError(@TempDir Path project) throws Exception {
        var model = Files.createDirectories(project.resolve("rules/Model"));
        var check = Files.createDirectories(project.resolve("rules/Check"));
        Files.writeString(project.resolve("rules.xml"), """
                <project>
                    <name>Two</name>
                    <modules>
                        <module>
                            <name>Model</name>
                            <rules-root path="rules/Model"/>
                        </module>
                        <module>
                            <name>Check</name>
                            <rules-root path="rules/Check"/>
                        </module>
                    </modules>
                </project>
                """);
        Files.writeString(model.resolve("Dog.md"), datatype("Dog", "| String | name  |"));
        Files.writeString(check.resolve("Environment.md"), """
                ---
                table: Environment
                ---

                | dependency | Model |
                |------------|-------|
                """);
        Files.writeString(check.resolve("broken.md"), """
                ---
                table: Method String broken()
                ---

                ```java
                return unknownName;
                ```
                """);
        var root = (XlsModuleOpenClass) compile(project, null, false).getOpenClassWithErrors();
        var checkModule = root.getDependencies()
                .stream()
                .map(CompiledDependency::getCompiledOpenClass)
                .filter(compiled -> "Check".equals(((XlsModuleOpenClass) compiled.getOpenClassWithErrors()).getModuleName()))
                .findFirst()
                .orElseThrow();
        assertTrue(checkModule.hasErrors());

        var found = DatatypeUsageCheck.check(checkModule, module -> "Model".equals(module.getModuleName()));

        assertEquals(List.of("Datatype 'Dog' is not used in the current project."), summaries(found));
    }

    @Test
    void checksNothingCompiledForExecution() throws Exception {
        assertTrue(check(true).isEmpty());
    }

    @Test
    void leavesATypeWithAnErrorToTheError(@TempDir Path project) throws Exception {
        var compiled = compileBroken(project, Map.of(
                "Cat.md", datatype("Cat", "| Person | owner |"),
                "Dog.md", datatype("Dog", "| String | name  |")));

        // Cat names a type no table declares: the error tells about it, the linter does not
        assertTrue(compiled.hasErrors());
        assertEquals(List.of("Datatype 'Dog' is not used in the current project."),
                summaries(DatatypeUsageCheck.check(compiled, module -> true)));
    }

    @Test
    void checksNothingWhileARuleHasAnError(@TempDir Path project) throws Exception {
        var compiled = compileBroken(project, Map.of(
                "Dog.md", datatype("Dog", "| String | name  |"),
                "bark.md", """
                        ---
                        table: Method String bark(Dog dog)
                        ---

                        ```java
                        return dog.name + sound;
                        ```
                        """));

        // The rule reads Dog.name, but its error leaves what it reads unknown: the field is not told as not used
        assertTrue(compiled.hasErrors());
        assertTrue(DatatypeUsageCheck.check(compiled, module -> true).isEmpty());
    }

    @Test
    void checksNothingWhileAFileCannotBeRead(@TempDir Path project) throws Exception {
        var compiled = compileBroken(project, Map.of(
                "Dog.md", datatype("Dog", "| String | name  |"),
                "bark.md", """
                        ---
                        table: Method String bark(Dog dog)

                        ```java
                        return dog.name;
                        ```
                        """));

        // The front matter of bark.md is not closed: the file holds no table, and what it reads is not known
        assertTrue(compiled.hasErrors());
        assertTrue(DatatypeUsageCheck.check(compiled, module -> true).isEmpty());
    }

    @Test
    void checksAProjectWhoseDependencyHasAnError(@TempDir Path workspace) throws Exception {
        writeProject(workspace.resolve("Library"), "Broken", "", Map.of("bark.md", """
                ---
                table: Method String bark()
                ---

                ```java
                return "Woof";
                ```
                """, "growl.md", """
                ---
                table: Method String growl()
                ---

                ```java
                return sound;
                ```
                """));
        writeProject(workspace.resolve("Pets"), "Pets", """
                <dependencies>
                    <dependency>
                        <name>Broken</name>
                        <autoIncluded>true</autoIncluded>
                    </dependency>
                </dependencies>
                """, Map.of("Dog.md", datatype("Dog", "| String | name  |"), "talk.md", """
                ---
                table: Method String talk()
                ---

                ```java
                return bark();
                ```
                """));
        var compiled = compile(workspace.resolve("Pets"), workspace, false);

        // The error is of the project Pets depends on: it tells nothing of what the tables of Pets use
        assertTrue(compiled.hasErrors());
        assertEquals(List.of("Datatype 'Dog' is not used in the current project."),
                summaries(DatatypeUsageCheck.check(compiled, module -> true)));
    }

    /**
     * A project may hold the beans of its datatypes compiled, and a module that imports their package sees each as a
     * Java class: the fields it reads of the class are the fields of the datatype.
     */
    @Test
    void countsTheBeanOfADatatypeAsTheDatatype(@TempDir Path project) throws Exception {
        var compiled = compileWithDogBean(project, Map.of("nameOf.md", """
                ---
                table: Spreadsheet String nameOf(Dog dog)
                ---

                | Step | Value           |
                |------|-----------------|
                | Name | = dog.name      |
                | Kind | = dog.getKind() |
                """, "breedOf.md", """
                ---
                table: Method Dog breedOf(Dog dog)
                ---

                ```java
                dog.setBreed("husky");
                return dog;
                ```
                """));

        // Check reads the name, calls the getter of the kind and the setter of the breed through the Java class: only
        // the age is used by no table
        assertEquals(List.of("Field 'age' of datatype 'Dog' is not used in the current project."),
                summaries(DatatypeUsageCheck.check(compiled, module -> true)));
    }

    /** The constructor of the bean that takes every field, which a module importing the bean calls, sets them all. */
    @Test
    void countsTheFieldsTheConstructorOfTheBeanSets(@TempDir Path project) throws Exception {
        var compiled = compileWithDogBean(project, Map.of("puppy.md", """
                ---
                table: Method Dog puppy()
                ---

                ```java
                return new Dog("Rex", "dog", "husky", 1);
                ```
                """));

        assertEquals(List.of(), summaries(DatatypeUsageCheck.check(compiled, module -> true)));
    }

    /**
     * Compiles a project whose module {@code Model} declares the datatype {@code Dog}, and whose module {@code Check}
     * holds the given tables and imports the package of the beans of the datatypes, which the project holds compiled.
     */
    private static CompiledOpenClass compileWithDogBean(Path project, Map<String, String> tables) throws Exception {
        var model = Files.createDirectories(project.resolve("rules/Model"));
        var check = Files.createDirectories(project.resolve("rules/Check"));
        Files.writeString(project.resolve("rules.xml"), """
                <project>
                    <name>Beans</name>
                    <modules>
                        <module>
                            <name>Model</name>
                            <rules-root path="rules/Model"/>
                        </module>
                        <module>
                            <name>Check</name>
                            <rules-root path="rules/Check"/>
                        </module>
                    </modules>
                    <classpath>
                        <entry path="classes"/>
                    </classpath>
                </project>
                """);
        Files.writeString(model.resolve("Dog.md"), datatype("Dog", """
                | String  | name  |
                | String  | kind  |
                | String  | breed |
                | Integer | age   |"""));
        Files.writeString(check.resolve("Environment.md"), """
                ---
                table: Environment
                ---

                | import | org.openl.generated.beans |
                |--------|---------------------------|
                """);
        for (var table : tables.entrySet()) {
            Files.writeString(check.resolve(table.getKey()), table.getValue());
        }
        compileBean(project, "org/openl/generated/beans/Dog.java", """
                package org.openl.generated.beans;

                public class Dog {
                    private String name;
                    private String kind;
                    private String breed;
                    private Integer age;

                    public Dog() {
                    }

                    public Dog(String name, String kind, String breed, Integer age) {
                        this.name = name;
                        this.kind = kind;
                        this.breed = breed;
                        this.age = age;
                    }

                    public String getName() { return name; }
                    public void setName(String name) { this.name = name; }
                    public String getKind() { return kind; }
                    public void setKind(String kind) { this.kind = kind; }
                    public String getBreed() { return breed; }
                    public void setBreed(String breed) { this.breed = breed; }
                    public Integer getAge() { return age; }
                    public void setAge(Integer age) { this.age = age; }
                }
                """);
        var compiled = compile(project, null, false);
        assertFalse(compiled.hasErrors(), () -> compiled.getAllMessages().toString());
        return compiled;
    }

    /** Compiles a Java class into the folder {@code classes} of the project. */
    private static void compileBean(Path project, String path, String source) throws Exception {
        var file = project.resolve("src").resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
        var classes = Files.createDirectories(project.resolve("classes"));
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", classes.toString(),
                file.toString()));
    }

    /** Compiles a project of one module of the given Markdown tables, which has a compilation error. */
    private static CompiledOpenClass compileBroken(Path project, Map<String, String> tables) throws Exception {
        writeProject(project, "Broken", "", tables);
        return compile(project, null, false);
    }

    /** Compiles a project, with the projects of the workspace it depends on. */
    private static CompiledOpenClass compile(Path project, @Nullable Path workspace, boolean executionMode)
            throws Exception {
        var builder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
                .setExecutionMode(executionMode)
                .setProject(project.toString());
        if (workspace != null) {
            builder.setWorkspace(workspace.toString());
        }
        return builder.build().getCompiledOpenClass();
    }

    /** Writes a project of one module of the given Markdown tables, with the dependencies given in its descriptor. */
    private static void writeProject(Path project, String name, String dependencies, Map<String, String> tables)
            throws Exception {
        var module = Files.createDirectories(project.resolve("rules/Main"));
        Files.writeString(project.resolve("rules.xml"), """
                <project>
                    <name>%s</name>
                    <modules>
                        <module>
                            <rules-root path="rules/Main"/>
                        </module>
                    </modules>
                %s</project>
                """.formatted(name, dependencies.indent(4)));
        for (var table : tables.entrySet()) {
            Files.writeString(module.resolve(table.getKey()), table.getValue());
        }
    }

    private static String datatype(String name, String field) {
        return """
                ---
                table: Datatype %s
                ---

                | Type   | Name  |
                |--------|-------|
                %s
                """.formatted(name, field);
    }

    private static Map<XlsModuleOpenClass, List<OpenLMessage>> check(boolean executionMode) throws Exception {
        var compiled = compile(Path.of(PROJECT), null, executionMode);
        assertFalse(compiled.hasErrors(), () -> compiled.getAllMessages().toString());
        return DatatypeUsageCheck.check(compiled, module -> true);
    }

    private static List<OpenLMessage> messages(Map<XlsModuleOpenClass, List<OpenLMessage>> found) {
        return found.values().stream().flatMap(List::stream).toList();
    }

    /** The texts of the messages, in the order of the alphabet. */
    private static List<String> summaries(Map<XlsModuleOpenClass, List<OpenLMessage>> found) {
        return messages(found).stream().map(OpenLMessage::getSummary).sorted().toList();
    }

    private static OpenLMessage messageOf(List<OpenLMessage> messages, String summary) {
        return messages.stream().filter(message -> message.getSummary().startsWith(summary)).findFirst().orElseThrow();
    }

    private static XlsUrlParser location(List<OpenLMessage> messages, String summary) {
        return new XlsUrlParser(messageOf(messages, summary).getSourceLocation());
    }
}

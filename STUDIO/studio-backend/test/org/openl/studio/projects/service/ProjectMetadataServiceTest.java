package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.CompiledOpenClass;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.table.properties.def.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.ProjectTypeFieldView;
import org.openl.studio.projects.model.ProjectTypeKind;
import org.openl.studio.projects.model.ProjectTypeView;
import org.openl.studio.projects.model.PropertyDefinitionView;
import org.openl.studio.projects.model.PropertyValueView;
import org.openl.studio.projects.model.files.FileNode;
import org.openl.studio.projects.model.files.FolderNode;
import org.openl.studio.projects.service.files.FileCriteriaQuery;
import org.openl.studio.projects.service.files.FileRoot;
import org.openl.studio.projects.service.files.FileViewMode;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.studio.projects.service.tables.TableTestProjects;
import org.openl.types.IOpenClass;

class ProjectMetadataServiceTest {

    private final ProjectFilesService projectFilesService = mock(ProjectFilesService.class);
    private final ProjectFileRootFactory projectFileRootFactory = mock(ProjectFileRootFactory.class);
    private final RulesProject project = mock(RulesProject.class);
    private final FileRoot root = mock(FileRoot.class);

    private final ProjectMetadataService service =
            new ProjectMetadataService(projectFilesService, projectFileRootFactory);

    @Test
    void listsTheTypesOfAProjectAsTheCompilerResolvedThem() {
        var model = mock(ProjectModel.class);
        when(model.getCompiledOpenClass())
                .thenReturn(new RulesEngineFactory<>("test/rules/EPBDS-16427/Types.xlsx").getCompiledOpenClass());

        var types = service.getTypes(model);

        // By name; a spreadsheet result is not among them, as no table is written of one.
        assertEquals(List.of("Address", "Base", "Country", "Driver", "Policy", "Vehicle"),
                types.stream().map(ProjectTypeView::name).toList());
        var policy = type(types, "Policy");
        assertEquals(ProjectTypeKind.DATATYPE, policy.kind());
        // The inherited field comes first, then the declared ones, a collection told apart.
        assertEquals(List.of(new ProjectTypeFieldView("id", "String", false),
                new ProjectTypeFieldView("number", "String", false),
                new ProjectTypeFieldView("address", "Address", false),
                new ProjectTypeFieldView("drivers", "Driver[]", true),
                new ProjectTypeFieldView("country", "Country", false)), policy.fields());
        assertTrue(policy.values().isEmpty());
        // A datatype laid out across the sheet has its fields read as the binder reads them.
        assertEquals(List.of("make", "year", "used", "color"),
                type(types, "Vehicle").fields().stream().map(ProjectTypeFieldView::name).toList());
        assertEquals(List.of("String", "Integer", "Boolean", "String"),
                type(types, "Vehicle").fields().stream().map(ProjectTypeFieldView::type).toList());
        var country = type(types, "Country");
        assertEquals(ProjectTypeKind.VOCABULARY, country.kind());
        assertEquals(List.of("US", "CA"), country.values());
        assertTrue(country.fields().isEmpty());
    }

    @Test
    void answersEachNameOnceForTheTypeTheRulesMeanByIt() {
        // Two datatypes of one name in different packages: the module knows both, by their Java names, and resolves
        // the name to one of them.
        var meant = new DatatypeOpenClass("Address", "org.provider");
        var other = new DatatypeOpenClass("Address", "org.consumer");
        var openClass = mock(IOpenClass.class);
        when(openClass.getTypes()).thenReturn(List.of(other, meant));
        when(openClass.findType("Address")).thenReturn(meant);
        var compiled = mock(CompiledOpenClass.class);
        when(compiled.getOpenClassWithErrors()).thenReturn(openClass);
        var model = mock(ProjectModel.class);
        when(model.getCompiledOpenClass()).thenReturn(compiled);

        var types = service.getTypes(model);

        assertEquals(List.of("Address"), types.stream().map(ProjectTypeView::name).toList());
    }

    @Test
    void namesTheModuleFolderOfATypeWrittenInAMarkdownFile(@TempDir Path project) throws IOException {
        Files.writeString(project.resolve("rules.xml"), """
                <project>
                    <name>Drivers</name>
                    <modules>
                        <module>
                            <name>Drivers</name>
                            <rules-root path="rules/Drivers"/>
                        </module>
                    </modules>
                </project>
                """);
        Files.createDirectories(project.resolve("rules/Drivers"));
        Files.writeString(project.resolve("rules/Drivers/Driver.md"), """
                ---
                table: Datatype Driver
                ---

                | String  | name |
                |---------|------|
                | Integer | age  |
                """);

        var driver = type(service.getTypes(TableTestProjects.projectModel(project)), "Driver");

        assertEquals("Drivers", driver.module());
        assertEquals(List.of(new ProjectTypeFieldView("name", "String", false),
                new ProjectTypeFieldView("age", "Integer", false)), driver.fields());
    }

    @Test
    void namesNoTypeOfAProjectNotCompiled() {
        assertTrue(service.getTypes(mock(ProjectModel.class)).isEmpty());
    }

    @Test
    void describesWhatAValueOfEachPropertyLooksLike() {
        var properties = service.getProperties(null);

        // A dimension property offers the values of its enum, several at a time.
        var state = property(properties, "state");
        assertEquals("enum", state.type());
        assertTrue(state.multiple());
        assertTrue(state.values().contains(new PropertyValueView("AL", "Alabama")));
        // One value out of the same kind of list.
        var origin = property(properties, "origin");
        assertEquals("enum", origin.type());
        assertFalse(origin.multiple());
        assertFalse(origin.values().isEmpty());

        assertEquals("date", property(properties, "effectiveDate").type());
        assertEquals("boolean", property(properties, "failOnMiss").type());
        // Text, whatever the values a description of it suggests: OpenL types `scope` as a string.
        assertEquals("text", property(properties, "scope").type());
        assertTrue(property(properties, "category").values().isEmpty());
    }

    @Test
    void namesAndGroupsPropertiesTheWayTableDetailsDoes() {
        var properties = service.getProperties("Rules");

        var state = property(properties, "state");
        assertEquals("US States", state.displayName());
        assertEquals("Business Dimension", state.group());
        assertTrue(state.dimensional());

        var version = property(properties, "version");
        assertEquals("Version", version.displayName());
        assertEquals("Version", version.group());
        assertFalse(version.dimensional());

        // The pattern the compiler validates the property with, so a dialog refuses what the module would refuse.
        assertEquals("([a-zA-Z_][a-zA-Z0-9_]*)", property(properties, "id").pattern());
        assertNull(property(properties, "category").pattern());
        // The version states no pattern the compiler recognises, so its shape is the copy dialog's editor to keep.
        assertNull(version.pattern());

        assertEquals("true", property(properties, "active").defaultValue());
        assertNull(version.defaultValue());
    }

    @Test
    void offersOnlyPropertiesAPropertiesTableMayDeclare() {
        // A Properties table declares nothing on itself: asked for that kind, the dictionary is of its contents.
        var names = service.getProperties("Properties").stream().map(PropertyDefinitionView::name).toList();

        // Stamped by OpenL Studio, never typed.
        assertFalse(names.contains("createdBy"));
        assertFalse(names.contains("modifiedOn"));
        // Allowed on a table only, so a Properties table declaring one fails to compile.
        assertFalse(names.contains("description"));
        assertFalse(names.contains("active"));
        assertTrue(names.containsAll(List.of("scope", "category")));
    }

    @Test
    void offersPropertiesForTheRequestedTableTypeAndLevel() {
        var rules = service.getProperties("Rules").stream().map(PropertyDefinitionView::name).toList();
        var spreadsheet = service.getProperties("Spreadsheet").stream().map(PropertyDefinitionView::name).toList();

        assertTrue(rules.containsAll(List.of("version", "active", "failOnMiss")));
        assertFalse(rules.contains("scope"));
        assertFalse(rules.contains("autoType"));
        assertTrue(spreadsheet.containsAll(List.of("version", "active", "autoType")));
        assertFalse(spreadsheet.contains("scope"));
        assertFalse(spreadsheet.contains("failOnMiss"));
    }

    @Test
    void offersEveryPropertyATableMayCarryWhenAskedForNoKind() {
        var names = service.getProperties(null).stream().map(PropertyDefinitionView::name).toList();

        // Written on a table, declared for it by a Properties table, or stamped by OpenL Studio: a search across
        // tables of every kind may narrow by any of them.
        assertTrue(names.containsAll(List.of("description", "tags", "id", "active", "scope", "createdBy")));
    }

    @Test
    void leavesADeprecatedPropertyOut() {
        var deprecated = Arrays.stream(DefaultPropertyDefinitions.getDefaultDefinitions())
                .filter(definition -> definition.getDeprecation() != null)
                .map(TablePropertyDefinition::getName)
                .toList();
        var names = service.getProperties(null).stream().map(PropertyDefinitionView::name).toList();

        assertFalse(deprecated.isEmpty(), "the dictionary keeps a deprecated property to leave out");
        assertTrue(deprecated.stream().noneMatch(names::contains));
    }

    @Test
    void rejectsAnUnknownTableType() {
        assertThrows(BadRequestException.class, () -> service.getProperties("Unknown"));
    }

    @Test
    void readsSheetNamesOffTheModuleWorkbook() throws Exception {
        stubWorkbook("Rules", "Data");

        assertEquals(List.of("Rules", "Data"), service.getSheets(project, "rules/Main.xlsx"));
    }

    @Test
    void readsTheFoldersOfAModuleOfMarkdownFilesAsItsSheets() {
        when(projectFileRootFactory.of(project)).thenReturn(root);
        var files = FileCriteriaQuery.builder().basePath("rules/Rating").extension("md").build();
        when(projectFilesService.getResources(root, files, true, FileViewMode.FLAT, null)).thenReturn(List.of(
                FolderNode.builder().path("rules/Rating/Factors").name("Factors").basePath("rules/Rating").build(),
                markdownFile("rules/Rating/Factors", "ageFactor.md"),
                markdownFile("rules/Rating/Premium", "base.md"),
                markdownFile("rules/Rating", "premium.md"),
                markdownFile("rules/Rating", "discount.md")));

        // The module folder first: a new table goes there unless the reader names another
        assertEquals(List.of("Rating", "Factors", "Premium"), service.getSheets(project, "rules/Rating"));
    }

    @Test
    void reportsAModuleWorkbookItCannotRead() throws Exception {
        var resource = mock(AProjectResource.class);
        when(resource.getContent()).thenReturn(new ByteArrayInputStream("not a workbook".getBytes()));
        when(projectFileRootFactory.of(project)).thenReturn(root);
        when(projectFilesService.getResource(root, "rules/Main.xlsx", null)).thenReturn(resource);

        assertThrows(BadRequestException.class, () -> service.getSheets(project, "rules/Main.xlsx"));
    }

    private static FileNode markdownFile(String folder, String name) {
        return FileNode.builder().path(folder + "/" + name).name(name).basePath(folder).extension("md").build();
    }

    private static PropertyDefinitionView property(List<PropertyDefinitionView> properties, String name) {
        return properties.stream()
                .filter(property -> name.equals(property.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No '%s' property is offered".formatted(name)));
    }

    private void stubWorkbook(String... sheets) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var workbook = new XSSFWorkbook()) {
            for (var sheet : sheets) {
                workbook.createSheet(sheet);
            }
            workbook.write(bytes);
        }
        var resource = mock(AProjectResource.class);
        when(resource.getContent()).thenReturn(new ByteArrayInputStream(bytes.toByteArray()));
        when(projectFileRootFactory.of(project)).thenReturn(root);
        when(projectFilesService.getResource(root, "rules/Main.xlsx", null)).thenReturn(resource);
    }

    private static ProjectTypeView type(List<ProjectTypeView> types, String name) {
        return types.stream().filter(type -> type.name().equals(name)).findFirst().orElseThrow();
    }
}

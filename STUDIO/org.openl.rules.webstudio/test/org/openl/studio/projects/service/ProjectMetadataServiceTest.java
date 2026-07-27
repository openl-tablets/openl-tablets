package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.PropertyDefinitionView;
import org.openl.studio.projects.service.files.FileRoot;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.files.ProjectFilesService;

class ProjectMetadataServiceTest {

    private final ProjectFilesService projectFilesService = mock(ProjectFilesService.class);
    private final ProjectFileRootFactory projectFileRootFactory = mock(ProjectFileRootFactory.class);
    private final RulesProject project = mock(RulesProject.class);
    private final FileRoot root = mock(FileRoot.class);

    private final ProjectMetadataService service =
            new ProjectMetadataService(projectFilesService, projectFileRootFactory);

    @Test
    void describesWhatAValueOfEachPropertyLooksLike() {
        var properties = service.getProperties();

        // A dimension property offers the values of its enum, several at a time.
        var state = property(properties, "state");
        assertEquals("enum", state.type());
        assertTrue(state.multiple());
        assertTrue(state.values().contains("AL"));
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
    void offersOnlyPropertiesAPropertiesTableMayDeclare() {
        var names = service.getProperties().stream().map(PropertyDefinitionView::name).toList();

        // Stamped by OpenL Studio, never typed.
        assertFalse(names.contains("createdBy"));
        assertFalse(names.contains("modifiedOn"));
        // Allowed on a table only, so a Properties table declaring one fails to compile.
        assertFalse(names.contains("description"));
        assertFalse(names.contains("active"));
        assertTrue(names.containsAll(List.of("scope", "category")));
    }

    @Test
    void readsSheetNamesOffTheModuleWorkbook() throws Exception {
        stubWorkbook("Rules", "Data");

        assertEquals(List.of("Rules", "Data"), service.getSheets(project, "rules/Main.xlsx"));
    }

    @Test
    void reportsAModuleWorkbookItCannotRead() throws Exception {
        var resource = mock(AProjectResource.class);
        when(resource.getContent()).thenReturn(new ByteArrayInputStream("not a workbook".getBytes()));
        when(projectFileRootFactory.of(project)).thenReturn(root);
        when(projectFilesService.getResource(root, "rules/Main.xlsx", null)).thenReturn(resource);

        assertThrows(BadRequestException.class, () -> service.getSheets(project, "rules/Main.xlsx"));
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
}

package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.CreateNewTableRequest;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.model.tables.TableKind;
import org.openl.studio.projects.service.files.FileRoot;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.studio.projects.service.tables.write.TableWriterExecutor;
import org.openl.studio.projects.service.tables.write.TableWritersFactory;

class TableCreatorServiceTest {

    private final TableWritersFactory tableWritersFactory = mock(TableWritersFactory.class);
    private final TableWriterExecutor tableWriterExecutor = mock(TableWriterExecutor.class);
    private final ProjectFilesService projectFilesService = mock(ProjectFilesService.class);
    private final ProjectFileRootFactory projectFileRootFactory = mock(ProjectFileRootFactory.class);
    private final SystemPropertiesService systemPropertiesService = mock(SystemPropertiesService.class);
    private final RulesProject project = mock(RulesProject.class);
    private final FileRoot root = mock(FileRoot.class);
    private final Map<String, byte[]> createdResources = new LinkedHashMap<>();
    private final Map<String, byte[]> updatedResources = new LinkedHashMap<>();

    private TableCreatorService service;

    @BeforeEach
    void setUp() {
        service = new TableCreatorService(
                tableWritersFactory,
                tableWriterExecutor,
                projectFilesService,
                projectFileRootFactory,
                systemPropertiesService);
        when(systemPropertiesService.onCreate()).thenReturn(Map.of());
        when(projectFileRootFactory.of(project)).thenReturn(root);
        when(project.getBusinessName()).thenReturn("Example");
        doAnswer(invocation -> {
            var path = invocation.getArgument(1, String.class);
            try (var content = invocation.getArgument(2, InputStream.class)) {
                createdResources.put(path, content.readAllBytes());
            }
            return null;
        }).when(projectFilesService).createResource(any(), anyString(), any(), anyBoolean());
        doAnswer(invocation -> {
            var path = invocation.getArgument(1, String.class);
            try (var content = invocation.getArgument(2, InputStream.class)) {
                updatedResources.put(path, content.readAllBytes());
            }
            return null;
        }).when(projectFilesService).updateResource(any(), anyString(), any());
    }

    @Test
    void createsDescriptorForSimpleProjectAndPreservesRootModule() throws Exception {
        var request = request("rules/Greeting.xlsx", rawTable(mergedHeader()));

        createModuleWithTable(simpleDescriptor(), request);

        assertEquals(List.of("rules/Greeting.xlsx", ProjectDescriptor.FILE_NAME),
                createdResources.keySet().stream().toList());
        var descriptor = ProjectDescriptor.read(new ByteArrayInputStream(
                createdResources.get(ProjectDescriptor.FILE_NAME)));
        assertEquals(List.of("Main.xlsx", "rules/Greeting.xlsx"),
                descriptor.getModules().stream().map(Module::getRulesRootPath).toList());
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(createdResources.get("rules/Greeting.xlsx")))) {
            var sheet = workbook.getSheet("Greeting");
            assertEquals("Rules Boolean Greeting()", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("Condition", sheet.getRow(2).getCell(1).getStringCellValue());
            assertEquals(1, sheet.getNumMergedRegions());
        }
    }

    @Test
    void registersModuleOutsideDefaultWildcards() throws Exception {
        var request = request("custom/Greeting.xlsx", rawTable(mergedHeader()));

        createModuleWithTable(simpleDescriptor(), request);

        assertEquals(List.of("custom/Greeting.xlsx", ProjectDescriptor.FILE_NAME),
                createdResources.keySet().stream().toList());
        var descriptor = ProjectDescriptor.read(new ByteArrayInputStream(
                createdResources.get(ProjectDescriptor.FILE_NAME)));
        var module = descriptor.getModules().getLast();
        assertEquals("Greeting", module.getName());
        assertEquals("custom/Greeting.xlsx", module.getRulesRootPath());
        assertEquals(List.of("Main.xlsx", "custom/Greeting.xlsx"),
                descriptor.getModules().stream().map(Module::getRulesRootPath).toList());
    }

    @Test
    void updatesExistingDescriptorAndPreservesCellValueTypes() throws Exception {
        var descriptor = new ProjectDescriptor();
        descriptor.setName("Example");
        when(project.hasArtefact(ProjectDescriptor.FILE_NAME)).thenReturn(true);
        var descriptorResource = mock(AProjectResource.class);
        when(descriptorResource.getContent()).thenReturn(new ByteArrayInputStream(descriptor.toBytes()));
        when(projectFilesService.getResource(root, ProjectDescriptor.FILE_NAME, null)).thenReturn(descriptorResource);
        var source = List.of(
                List.of(cell("Rules Boolean Greeting()", 2), RawTableCell.COVERED_CELL),
                List.of(cell(true), cell(42)),
                List.of(cell(new Date(0)), RawTableCell.builder().build()));

        createModuleWithTable(simpleDescriptor(), request("custom/Greeting.xlsx", rawTable(source)));

        assertEquals(List.of(ProjectDescriptor.FILE_NAME), updatedResources.keySet().stream().toList());
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(
                createdResources.get("custom/Greeting.xlsx")))) {
            var sheet = workbook.getSheet("Greeting");
            assertTrue(sheet.getRow(2).getCell(1).getBooleanCellValue());
            assertEquals(42, sheet.getRow(2).getCell(2).getNumericCellValue());
            // Excel stores a date as a number, and OpenL reads it back as a date only while it is formatted as one.
            assertEquals(0, sheet.getRow(3).getCell(1).getDateCellValue().getTime());
            assertTrue(DateUtil.isCellDateFormatted(sheet.getRow(3).getCell(1)));
        }
    }

    @Test
    void rejectsMergeOutsideTableBounds() {
        var invalidHeader = List.of(
                List.of(cell("Rules Boolean Greeting()", 3), RawTableCell.COVERED_CELL),
                List.of(cell("Condition"), cell("Result")));
        var request = request("rules/Greeting.xlsx", rawTable(invalidHeader));

        var descriptor = simpleDescriptor();

        assertThrows(BadRequestException.class,
                () -> createModuleWithTable(descriptor, request));
        verify(projectFilesService, never()).createResource(any(), anyString(), any(), anyBoolean());
    }

    @Test
    void rejectsOverlappingMergedCells() {
        var overlapping = List.of(
                List.of(cell("Rules Boolean Greeting()", 2), cell("Overlap", 2), RawTableCell.COVERED_CELL),
                List.of(cell("Condition"), cell("Result"), cell("Extra")));
        var request = request("rules/Greeting.xlsx", rawTable(overlapping));

        var descriptor = simpleDescriptor();

        assertThrows(BadRequestException.class,
                () -> createModuleWithTable(descriptor, request));
    }

    @Test
    void rejectsOversizedMergeInsteadOfOverflowing() {
        var overflowing = List.of(
                List.of(RawTableCell.builder().value("Rules Boolean Greeting()").rowspan(Integer.MAX_VALUE).build()),
                List.of(cell("Condition")));
        var request = request("rules/Greeting.xlsx", rawTable(overflowing));

        var descriptor = simpleDescriptor();

        assertThrows(BadRequestException.class,
                () -> createModuleWithTable(descriptor, request));
    }

    @Test
    void rejectsNamelessTableRatherThanFailingOnIt() {
        // The name is what the header declares; a table saved without one can never be found again by its creator.
        assertThrows(BadRequestException.class, () -> service.requireTableName(null));
        assertThrows(BadRequestException.class, () -> service.requireTableName("  "));
    }

    @Test
    void rejectsBlankRowTheExistingModulePathRejectsToo() {
        var withBlankRow = List.of(
                List.of(cell("Rules Boolean Greeting()")),
                List.<RawTableCell>of(),
                List.of(cell("Condition")));
        var request = request("rules/Greeting.xlsx", rawTable(withBlankRow));

        // OpenL reads a blank line as the end of the table, so everything below it would be lost.
        var descriptor = simpleDescriptor();

        assertThrows(BadRequestException.class,
                () -> createModuleWithTable(descriptor, request));
    }

    @Test
    void rejectsRowThatReachesTheSheetBlank() {
        var falselyCovered = List.of(
                List.of(cell("Rules Boolean Greeting()")),
                // A covered cell gets the row past the payload check, and the blank beside it puts a cell on the
                // sheet — but no merge above spans into the row, so it is written empty all the same.
                List.of(RawTableCell.COVERED_CELL, cell(null)),
                List.of(cell("Condition")));
        var request = request("rules/Greeting.xlsx", rawTable(falselyCovered));

        var descriptor = simpleDescriptor();

        assertThrows(BadRequestException.class,
                () -> createModuleWithTable(descriptor, request));
    }

    @Test
    void rejectsColumnThatReachesTheSheetBlank() {
        var withBlankColumn = List.of(
                // Both rows carry a value, so neither is blank on its own — the middle column is blank in both.
                List.of(cell("Rules Boolean Greeting()"), cell(null), cell(null)),
                List.of(cell("Condition"), cell(null), cell("Result")));
        var request = request("rules/Greeting.xlsx", rawTable(withBlankColumn));

        // OpenL reads the table only as far as the blank column, so the cells beyond it would be lost.
        var descriptor = simpleDescriptor();

        assertThrows(BadRequestException.class,
                () -> createModuleWithTable(descriptor, request));
    }

    @Test
    void keepsTrailingBlankColumnTheTableSimplyDoesNotReach() throws Exception {
        var trailingBlank = List.of(
                List.of(cell("Rules Boolean Greeting()"), cell(null)),
                List.of(cell("Condition"), cell(null)));

        // A blank line at the edge leaves the table narrower than the payload declared, which loses nothing — the
        // same answer a write into an existing module gives.
        createModuleWithTable(simpleDescriptor(), request("rules/Greeting.xlsx", rawTable(trailingBlank)));

        assertTrue(createdResources.containsKey("rules/Greeting.xlsx"));
    }

    @Test
    void keepsColumnCoveredByAMergeDeclaredToItsLeft() throws Exception {
        var merged = List.of(
                List.of(cell("Rules Boolean Greeting()", 2), RawTableCell.COVERED_CELL),
                // The second column carries nothing of its own, but the header really does span into it.
                List.of(cell("Condition"), cell(null)));

        createModuleWithTable(simpleDescriptor(), request("rules/Greeting.xlsx", rawTable(merged)));

        assertTrue(createdResources.containsKey("rules/Greeting.xlsx"));
    }

    @Test
    void keepsRowCoveredByAMergeDeclaredAbove() throws Exception {
        var merged = List.of(
                List.of(RawTableCell.builder().value("Rules Boolean Greeting()").rowspan(2).build()),
                List.of(RawTableCell.COVERED_CELL),
                List.of(cell("Condition")));

        // The row carries nothing of its own, but the header really does span into it.
        createModuleWithTable(simpleDescriptor(), request("rules/Greeting.xlsx", rawTable(merged)));

        assertTrue(createdResources.containsKey("rules/Greeting.xlsx"));
    }

    @Test
    void rejectsSheetNameExcelCannotStore() {
        var request = new CreateNewTableRequest("Greeting", "Rates/2026", "rules/Greeting.xlsx",
                rawTable(mergedHeader()));

        var descriptor = simpleDescriptor();

        assertThrows(BadRequestException.class,
                () -> createModuleWithTable(descriptor, request));
    }

    @Test
    void declaresWildcardCoveredModuleWhenItsNameDiffersFromTheFile() throws Exception {
        var request = new CreateNewTableRequest("Pricing", "Greeting", "rules/Greeting.xlsx",
                rawTable(mergedHeader()));

        // The wildcard already covers the path, but it would name the module after its workbook, so the module is
        // declared explicitly to keep the requested name.
        createModuleWithTable(wildcardDescriptor(), request);

        // Left auto-discovered, the module would be named after its workbook and the requested name would be lost.
        var descriptor = ProjectDescriptor.read(new ByteArrayInputStream(
                createdResources.get(ProjectDescriptor.FILE_NAME)));
        assertTrue(descriptor.getModules().stream().anyMatch(module -> "Pricing".equals(module.getName())
                && "rules/Greeting.xlsx".equals(module.getRulesRootPath())));
    }

    @Test
    void removesWorkbookWhenDescriptorCannotBeRead() throws Exception {
        mockBrokenDescriptor();
        var request = request("custom/Greeting.xlsx", rawTable(mergedHeader()));

        var descriptor = simpleDescriptor();

        assertThrows(BadRequestException.class,
                () -> createModuleWithTable(descriptor, request));

        verify(projectFilesService).deleteResource(root, "custom/Greeting.xlsx");
    }

    @Test
    void preservesDescriptorErrorWhenWorkbookRollbackFails() throws Exception {
        mockBrokenDescriptor();
        doThrow(new IllegalStateException("rollback failed"))
                .when(projectFilesService)
                .deleteResource(root, "custom/Greeting.xlsx");
        var request = request("custom/Greeting.xlsx", rawTable(mergedHeader()));

        var descriptor = simpleDescriptor();

        assertThrows(BadRequestException.class,
                () -> createModuleWithTable(descriptor, request));
    }

    @Test
    void createsAnEmptyModuleWithABlankSheet() throws Exception {
        service.createEmptyModule(project, simpleDescriptor(), "Greeting", "custom/Greeting.xlsx", "Sheet1");

        assertEquals(List.of("custom/Greeting.xlsx", ProjectDescriptor.FILE_NAME),
                createdResources.keySet().stream().toList());
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(createdResources.get("custom/Greeting.xlsx")))) {
            assertNotNull(workbook.getSheet("Sheet1"), "the sheet the caller named exists");
            assertEquals(0, workbook.getSheet("Sheet1").getPhysicalNumberOfRows(), "the module starts empty");
        }
    }

    @Test
    void deleteModuleRemovesTheWorkbookOfADescriptorFreeProject() {
        // A project with no rules.xml has nothing to undeclare, so only the workbook is removed.
        service.deleteModule(project, "Greeting", "custom/Greeting.xlsx");

        verify(projectFilesService).deleteResource(root, "custom/Greeting.xlsx");
        verify(projectFilesService, never()).updateResource(any(), anyString(), any());
    }

    @Test
    void deleteModuleDropsTheDeclarationFromRulesXml() throws Exception {
        var declared = new ProjectDescriptor();
        declared.setName("Example");
        var module = new Module();
        module.setName("Greeting");
        module.setRulesRootPath("custom/Greeting.xlsx");
        declared.setModules(new ArrayList<>(List.of(module)));
        when(project.hasArtefact(ProjectDescriptor.FILE_NAME)).thenReturn(true);
        var descriptorResource = mock(AProjectResource.class);
        when(descriptorResource.getContent()).thenReturn(new ByteArrayInputStream(declared.toBytes()));
        when(projectFilesService.getResource(root, ProjectDescriptor.FILE_NAME, null)).thenReturn(descriptorResource);

        service.deleteModule(project, "Greeting", "custom/Greeting.xlsx");

        verify(projectFilesService).deleteResource(root, "custom/Greeting.xlsx");
        var rewritten = ProjectDescriptor.read(new ByteArrayInputStream(
                updatedResources.get(ProjectDescriptor.FILE_NAME)));
        assertTrue(rewritten.getModules() == null || rewritten.getModules().isEmpty(),
                "the declaration of the removed module is dropped");
    }

    @Test
    void deleteModuleKeepsASameNamedModuleDeclaredAtADifferentPath() throws Exception {
        var declared = new ProjectDescriptor();
        declared.setName("Example");
        var module = new Module();
        module.setName("Greeting");
        module.setRulesRootPath("rules/Greeting.xlsx");
        declared.setModules(new ArrayList<>(List.of(module)));
        when(project.hasArtefact(ProjectDescriptor.FILE_NAME)).thenReturn(true);
        var descriptorResource = mock(AProjectResource.class);
        when(descriptorResource.getContent()).thenReturn(new ByteArrayInputStream(declared.toBytes()));
        when(projectFilesService.getResource(root, ProjectDescriptor.FILE_NAME, null)).thenReturn(descriptorResource);

        // Roll back a module of the same name but a different path: the declared one is matched by path and left alone.
        service.deleteModule(project, "Greeting", "custom/Greeting.xlsx");

        verify(projectFilesService).deleteResource(root, "custom/Greeting.xlsx");
        verify(projectFilesService, never()).updateResource(any(), anyString(), any());
    }

    @Test
    void stampsTheTableItWritesIntoANewModuleAsCreated() throws Exception {
        when(systemPropertiesService.onCreate()).thenReturn(stamp());

        createModuleWithTable(simpleDescriptor(), request("rules/Greeting.xlsx", rawTable(mergedHeader())));

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(createdResources.get("rules/Greeting.xlsx")))) {
            var sheet = workbook.getSheet("Greeting");
            assertEquals("Rules Boolean Greeting()", sheet.getRow(1).getCell(1).getStringCellValue());
            // The properties section stands between the header and the body, the way OpenL reads it.
            assertEquals("properties", sheet.getRow(2).getCell(1).getStringCellValue());
            assertEquals("createdBy", sheet.getRow(2).getCell(2).getStringCellValue());
            assertEquals("jane", sheet.getRow(2).getCell(3).getStringCellValue());
            assertEquals("createdOn", sheet.getRow(3).getCell(2).getStringCellValue());
            assertEquals("Condition", sheet.getRow(4).getCell(1).getStringCellValue());
            // The header spans the widened table, and the marker spans both property rows.
            assertEquals(List.of("B2:D2", "B3:B4"),
                    sheet.getMergedRegions().stream().map(CellRangeAddress::formatAsString).sorted().toList());
        }
    }

    @Test
    void stampsTheCreationDateAsADateTheEngineReadsBack() throws Exception {
        when(systemPropertiesService.onCreate()).thenReturn(stamp());

        createModuleWithTable(simpleDescriptor(), request("rules/Greeting.xlsx", rawTable(mergedHeader())));

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(createdResources.get("rules/Greeting.xlsx")))) {
            var createdOn = workbook.getSheet("Greeting").getRow(3).getCell(3);
            assertEquals(0, createdOn.getDateCellValue().getTime());
            assertTrue(DateUtil.isCellDateFormatted(createdOn));
        }
    }

    @Test
    void stampsAWideTableWithItsValuesReachingTheRightEdge() throws Exception {
        when(systemPropertiesService.onCreate()).thenReturn(stamp());
        var wide = List.of(
                List.of(cell("Rules Boolean Greeting()", 4), RawTableCell.COVERED_CELL, RawTableCell.COVERED_CELL,
                        RawTableCell.COVERED_CELL),
                List.of(cell("C1"), cell("C2"), cell("C3"), cell("RET1")),
                List.of(cell("true"), cell("true"), cell("true"), cell("true")));

        createModuleWithTable(simpleDescriptor(), request("rules/Greeting.xlsx", rawTable(wide)));

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(createdResources.get("rules/Greeting.xlsx")))) {
            var sheet = workbook.getSheet("Greeting");
            // The value spans the columns beside it, which would otherwise read as cells of the section.
            assertEquals(List.of("B2:E2", "B3:B4", "D3:E3", "D4:E4"),
                    sheet.getMergedRegions().stream().map(CellRangeAddress::formatAsString).sorted().toList());
        }
    }

    @Test
    void leavesATableThatCarriesItsOwnPropertiesUnstamped() throws Exception {
        when(systemPropertiesService.onCreate()).thenReturn(stamp());
        var declaring = List.of(
                List.of(cell("Rules Boolean Greeting()", 3), RawTableCell.COVERED_CELL, RawTableCell.COVERED_CELL),
                List.of(cell("properties"), cell("category"), cell("Auto")),
                List.of(cell("Condition"), cell("Result"), cell("Extra")));

        createModuleWithTable(simpleDescriptor(), request("rules/Greeting.xlsx", rawTable(declaring)));

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(createdResources.get("rules/Greeting.xlsx")))) {
            var sheet = workbook.getSheet("Greeting");
            // OpenL reads the first properties section alone, so a second one would leave these rows in the body.
            assertEquals("category", sheet.getRow(2).getCell(2).getStringCellValue());
            assertEquals("Condition", sheet.getRow(3).getCell(1).getStringCellValue());
        }
    }

    @Test
    void leavesATableOpenLReadsNoPropertiesOnUnstamped() throws Exception {
        when(systemPropertiesService.onCreate()).thenReturn(stamp());
        var environment = RawTableView.builder()
                .kind(TableKind.ENVIRONMENT)
                .name("Env")
                .source(List.of(List.of(cell("Environment"), RawTableCell.COVERED_CELL),
                        List.of(cell("import"), cell("org.openl"))))
                .build();

        createModuleWithTable(simpleDescriptor(), request("rules/Greeting.xlsx", environment));

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(createdResources.get("rules/Greeting.xlsx")))) {
            // An Environment table reads no row as a property, so a stamped one would not compile.
            assertEquals("import", workbook.getSheet("Greeting").getRow(2).getCell(1).getStringCellValue());
        }
    }

    @Test
    void leavesAFreeFormTableUnstamped() throws Exception {
        when(systemPropertiesService.onCreate()).thenReturn(stamp());
        var freeForm = RawTableView.builder()
                .kind(TableKind.OTHER)
                .name("Notes")
                .source(List.of(List.of(cell("Notes")), List.of(cell("anything"))))
                .build();

        createModuleWithTable(simpleDescriptor(), request("rules/Greeting.xlsx", freeForm));

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(createdResources.get("rules/Greeting.xlsx")))) {
            var sheet = workbook.getSheet("Greeting");
            // OpenL reads none of a free-form table's rows as properties, so nothing is written between them.
            assertEquals("anything", sheet.getRow(2).getCell(1).getStringCellValue());
        }
    }

    private static Map<String, Object> stamp() {
        var stamped = new LinkedHashMap<String, Object>();
        stamped.put("createdBy", "jane");
        stamped.put("createdOn", new Date(0));
        return stamped;
    }

    private void createModuleWithTable(ProjectDescriptor descriptor,
                                       CreateNewTableRequest request) throws ProjectException {
        service.createModuleWithTable(project, descriptor, request, (RawTableView) request.table());
    }

    private void mockBrokenDescriptor() throws Exception {
        when(project.hasArtefact(ProjectDescriptor.FILE_NAME)).thenReturn(true);
        var descriptorResource = mock(AProjectResource.class);
        when(descriptorResource.getContent()).thenReturn(new ByteArrayInputStream("<broken".getBytes()));
        when(projectFilesService.getResource(root, ProjectDescriptor.FILE_NAME, null)).thenReturn(descriptorResource);
    }

    private static CreateNewTableRequest request(String modulePath, RawTableView table) {
        return new CreateNewTableRequest("Greeting", "Greeting", modulePath, table);
    }

    private static ProjectDescriptor simpleDescriptor() {
        var module = new Module();
        module.setName("Main");
        module.setRulesRootPath("Main.xlsx");
        var descriptor = new ProjectDescriptor();
        descriptor.setName("Example");
        descriptor.setModules(List.of(module));
        return descriptor;
    }

    /** A descriptor whose only declaration is a wildcard matching the module the tests create. */
    private static ProjectDescriptor wildcardDescriptor() {
        var module = new Module();
        module.setRulesRootPath("rules/*.xlsx");
        var descriptor = new ProjectDescriptor();
        descriptor.setName("Example");
        descriptor.setModules(List.of(module));
        return descriptor;
    }

    private static RawTableView rawTable(List<List<RawTableCell>> source) {
        return RawTableView.builder()
                .kind(TableKind.RULES)
                .name("Greeting")
                .source(source)
                .build();
    }

    private static List<List<RawTableCell>> mergedHeader() {
        return List.of(
                List.of(cell("Rules Boolean Greeting()", 2), RawTableCell.COVERED_CELL),
                List.of(cell("Condition"), cell("Result")),
                List.of(cell("true"), cell("true")));
    }

    private static RawTableCell cell(Object value) {
        return RawTableCell.builder().value(value).build();
    }

    private static RawTableCell cell(Object value, int colspan) {
        return RawTableCell.builder().value(value).colspan(colspan).build();
    }
}

package org.openl.studio.projects.service.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.webstudio.web.Props;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.projects.model.openapi.OpenApiGenerationRequest;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.studio.projects.service.history.ProjectHistoryService;

/**
 * What a generation is refused for, and which workbooks it says it would write.
 *
 * <p>The scaffolding itself is the engine's; that a module the project already reads is written over where
 * it stands, and that a name no module answers to is answered with the workbook it would be given, is this
 * service's own.
 */
class ProjectOpenApiGenerationServiceTest {

    private final WorkspaceProjectService projects = mock(WorkspaceProjectService.class);
    private final ProjectOpenApiGenerationService service = new ProjectOpenApiGenerationService(
            projects, mock(ProjectFilesService.class), mock(ProjectFileRootFactory.class),
            mock(ProjectHistoryService.class));

    private Environment previousEnvironment;

    @BeforeEach
    void namesTheDefaultsTheEditorOffered() {
        previousEnvironment = Props.getEnvironment();
        var environment = new MockEnvironment();
        environment.setProperty("openapi.default.algorithm.module.name", "Algorithms");
        environment.setProperty("openapi.default.algorithm.module.path", "rules/Algorithms.xlsx");
        environment.setProperty("openapi.default.data.module.name", "Models");
        environment.setProperty("openapi.default.data.module.path", "rules/Models.xlsx");
        Props.setEnvironment(environment);
    }

    @AfterEach
    void leavesTheSettingsAsTheyWere() {
        Props.setEnvironment(previousEnvironment);
    }

    @Test
    void writesOverTheWorkbookOfAModuleTheProjectDeclares() {
        var project = holding(projectReading(module("Algorithms", "api/Rules.xlsx")), "api/Rules.xlsx");

        var plan = service.plan(project, "Algorithms", "Models");

        // The module is there, so the generation replaces the workbook it reads rather than adding another.
        assertEquals("api/Rules.xlsx", plan.algorithm().path());
        assertTrue(plan.algorithm().declared());
        assertTrue(plan.algorithm().overwrites());
    }

    @Test
    void namesTheWorkbookAModuleNobodyReadsWouldBeGiven() {
        var plan = service.plan(projectReading(module("Algorithms", "api/Rules.xlsx")), "Algorithms", "Models");

        assertEquals("rules/Models.xlsx", plan.model().path());
        assertFalse(plan.model().declared());
    }

    @Test
    void namesTheModuleAfterTheNameItWasAskedFor() {
        var plan = service.plan(projectReading(module("Algorithms", "api/Rules.xlsx")), "Pricing", null);

        // A name of the reader's own is answered with a workbook of its own, beside the default one.
        assertEquals("Pricing", plan.algorithm().name());
        assertEquals("rules/Pricing.xlsx", plan.algorithm().path());
        assertEquals("Models", plan.model().name());
    }

    @Test
    void writesOverTheWorkbookOfAModuleDeclaredWithoutAName() {
        var plan = service.plan(projectReading(module(null, "api/Algorithms.xlsx")), "Algorithms", "Models");

        // The engine names such a module after the workbook it reads, so this is that module, and its
        // workbook is what the generation replaces — not a second one laid beside it.
        assertEquals("api/Algorithms.xlsx", plan.algorithm().path());
        assertTrue(plan.algorithm().declared());
    }

    @Test
    void writesOverTheWorkbookAPatternMatchedWhereverTheProjectKeepsIt() {
        // A project declaring nothing reads its tests under tests/ by the default patterns, and a module is
        // known by that name there — not by the workbook a generation would lay under rules/.
        var project = projectReading(module("AutoPolicyCalculation", "rules/AutoPolicyCalculation.xlsx"),
                module("AutoPolicyTests", "tests/AutoPolicyTests.xlsx"));

        var plan = service.plan(project, "AutoPolicyCalculation", "AutoPolicyTests");

        assertEquals("tests/AutoPolicyTests.xlsx", plan.model().path());
        assertTrue(plan.model().declared());
    }

    @Test
    void doesNotCallAWorkbookNobodyReadsReplaced() {
        // rules/Models.xlsx would be matched by the default pattern, but no such module exists yet: the
        // generation adds it, and the reader is told so rather than warned about a replacement.
        var plan = service.plan(projectReading(module("Rules", "rules/Rules.xlsx")), "Rules", "Models");

        assertEquals("rules/Models.xlsx", plan.model().path());
        assertFalse(plan.model().declared());
    }

    @Test
    void refusesAModuleNameARepositoryCannotHold() {
        var project = mock(RulesProject.class);
        var request = asked("Rates/2026", "Models");

        var refused = assertThrows(ConflictException.class, () -> service.generateTables(project, request));

        assertEquals("openl.error.409.projects.openapi.module-name.invalid.message", refused.getErrorCode());
    }

    @Test
    void refusesToWriteBothModulesIntoOneWorkbook() {
        var project = projectReading();
        var request = new OpenApiGenerationRequest("openapi.json", "Algorithms", "rules/Both.xlsx",
                "Models", "rules/BOTH.xlsx");

        var refused = assertThrows(ConflictException.class, () -> service.generateTables(project, request));

        // Named the same but for their case: one workbook cannot be two modules.
        assertEquals("openl.error.409.projects.openapi.module-path.same.message", refused.getErrorCode());
    }

    @Test
    void refusesToWriteBothModulesWhereOneTheProjectReadsStands() {
        // Two workbooks are asked for, but the module the rules are named after already reads the one the
        // data types would be written to: the request's paths are not what would be written.
        var project = projectReading(module("Algorithms", "rules/Models.xlsx"));
        var request = new OpenApiGenerationRequest("openapi.json", "Algorithms", "rules/Algorithms.xlsx",
                "Models", "rules/Models.xlsx");

        var refused = assertThrows(ConflictException.class, () -> service.generateTables(project, request));

        assertEquals("openl.error.409.projects.openapi.module-path.same.message", refused.getErrorCode());
    }

    @Test
    void refusesToWriteAModuleToAFileThatIsNoWorkbook() {
        var project = projectReading();
        var request = new OpenApiGenerationRequest("openapi.json", "Algorithms", "rules/Alg.txt",
                "Models", "rules/Models.xlsx");

        var refused = assertThrows(ConflictException.class, () -> service.generateTables(project, request));

        // A module is read from a workbook; a file named anything else is served as that kind of file and
        // read as no module at all, while rules.xml names it as one.
        assertEquals("openl.error.409.projects.openapi.module-path.not-a-workbook.message",
                refused.getErrorCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"rules/.xlsx", "rules/Al?g.xlsx", "rules/ Alg.xlsx", "rules//Alg.xlsx"})
    void refusesAPathTheRepositoryCannotHold(String workbook) {
        var project = projectReading();
        var request = new OpenApiGenerationRequest("openapi.json", "Algorithms", workbook,
                "Models", "rules/Models.xlsx");

        var refused = assertThrows(ConflictException.class, () -> service.generateTables(project, request));

        // Refused before either module is written: the repository refuses such a path when the write
        // reaches it, by which time the other module has been replaced.
        assertEquals("openl.error.409.projects.openapi.module-path.invalid.message", refused.getErrorCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"rules/Alg.xlsx", "rules/Alg.XLS", "rules/Alg.xlsm"})
    void writesAModuleToEveryWorkbookExcelReads(String workbook) {
        var project = projectReading();
        var request = new OpenApiGenerationRequest("openapi.json", "Algorithms", workbook,
                "Models", "rules/Models.xlsx");

        var refused = assertThrows(ConflictException.class, () -> service.generateTables(project, request));

        // Refused further on, for want of a checked-out copy to read the specification from — the workbook
        // itself was not what stood in the way.
        assertEquals("openl.error.409.projects.openapi.not-checked-out.message", refused.getErrorCode());
    }

    @Test
    void refusesOneNameForBothModules() {
        var request = new OpenApiGenerationRequest("openapi.json", "Both", "rules/Rules.xlsx",
                "Both", "rules/Models.xlsx");

        var refused = assertThrows(ConflictException.class,
                () -> service.generateTables(mock(RulesProject.class), request));

        // Two workbooks, but declared under one name: the descriptor would name one module twice.
        assertEquals("openl.error.409.projects.openapi.module-name.same.message", refused.getErrorCode());
    }

    @Test
    void doesNotCallAWorkbookNobodyWroteYetReplaced() {
        // The project declares the module, so the generation writes where it reads and the workbook is not
        // the reader's to choose — but no file stands there, so nothing is taken away.
        var plan = service.plan(projectReading(module("Algorithms", "api/Rules.xlsx")), "Algorithms", "Models");

        assertTrue(plan.algorithm().declared());
        assertFalse(plan.algorithm().overwrites());
    }

    @Test
    void doesNotCallAFileNoModuleReadsReplaced() {
        // A workbook stands where the module would be added, but the project reads no module there. The
        // generation refuses to write over it rather than replace it, so calling it a replacement would
        // promise what cannot happen.
        var project = holding(projectReading(), "rules/Models.xlsx");

        var plan = service.plan(project, "Algorithms", "Models");

        assertEquals("rules/Models.xlsx", plan.model().path());
        assertFalse(plan.model().declared());
        assertFalse(plan.model().overwrites());
    }

    private static OpenApiGenerationRequest asked(String algorithmModuleName, String modelModuleName) {
        return new OpenApiGenerationRequest("openapi.json", algorithmModuleName, "rules/Algorithms.xlsx",
                modelModuleName, "rules/Models.xlsx");
    }

    /** A project the engine resolved to the given modules, declared or matched by a pattern alike. */
    private RulesProject projectReading(Module... modules) {
        var project = mock(RulesProject.class);
        var resolved = new ProjectDescriptor();
        resolved.setModules(List.of(modules));
        when(projects.getProjectDescriptor(project)).thenReturn(resolved);
        return project;
    }

    /** The same project, with a file standing at the given path. */
    private static RulesProject holding(RulesProject project, String path) {
        when(project.hasArtefact(path)).thenReturn(true);
        return project;
    }

    private static Module module(String name, String path) {
        var module = new Module();
        module.setName(name);
        module.setRulesRootPath(path);
        return module;
    }
}

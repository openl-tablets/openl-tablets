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
        var plan = service.plan(projectReading(module("Algorithms", "api/Rules.xlsx")), "Algorithms", "Models");

        // The module is there, so the generation replaces the workbook it reads rather than adding another.
        assertEquals("api/Rules.xlsx", plan.algorithm().path());
        assertTrue(plan.algorithm().declared());
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
        var refused = assertThrows(ConflictException.class,
                () -> service.generateTables(mock(RulesProject.class), asked("Rates/2026", "Models")));

        assertEquals("openl.error.409.projects.openapi.module-name.invalid.message", refused.getErrorCode());
    }

    @Test
    void refusesToWriteBothModulesIntoOneWorkbook() {
        var request = new OpenApiGenerationRequest("openapi.json", "Algorithms", "rules/Both.xlsx",
                "Models", "rules/BOTH.xlsx");

        var refused = assertThrows(ConflictException.class,
                () -> service.generateTables(projectReading(), request));

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
    void refusesOneNameForBothModules() {
        var request = new OpenApiGenerationRequest("openapi.json", "Both", "rules/Rules.xlsx",
                "Both", "rules/Models.xlsx");

        var refused = assertThrows(ConflictException.class,
                () -> service.generateTables(mock(RulesProject.class), request));

        // Two workbooks, but declared under one name: the descriptor would name one module twice.
        assertEquals("openl.error.409.projects.openapi.module-name.same.message", refused.getErrorCode());
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

    private static Module module(String name, String path) {
        var module = new Module();
        module.setName(name);
        module.setRulesRootPath(path);
        return module;
    }
}

package org.openl.studio.projects.service.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.webstudio.web.Props;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.projects.model.openapi.OpenApiGenerationRequest;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.files.FileRoot;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.studio.projects.service.history.ProjectHistoryService;

/**
 * What a generation is refused for, and which workbooks it says it would write.
 *
 * <p>The scaffolding itself is the engine's; that a module the project already declares is written over
 * where it stands, and that a name nobody declared is answered with the workbook it would be given, is
 * this service's own.
 */
class ProjectOpenApiGenerationServiceTest {

    private static final String DESCRIPTOR = """
            <project>
                <name>Rates</name>
                <modules>
                    <module>
                        <name>Algorithms</name>
                        <rules-root path="api/Rules.xlsx"/>
                    </module>
                </modules>
            </project>
            """;

    private final ProjectFilesService files = mock(ProjectFilesService.class);
    private final ProjectFileRootFactory roots = mock(ProjectFileRootFactory.class);
    private final ProjectOpenApiGenerationService service = new ProjectOpenApiGenerationService(
            mock(WorkspaceProjectService.class), files, roots, mock(ProjectHistoryService.class));

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
        var plan = service.plan(projectDeclaring(DESCRIPTOR), "Algorithms", "Models");

        // The module is there, so the generation replaces the workbook it reads rather than adding another.
        assertEquals("api/Rules.xlsx", plan.algorithm().path());
        assertTrue(plan.algorithm().declared());
    }

    @Test
    void namesTheWorkbookAModuleNobodyDeclaredWouldBeGiven() {
        var plan = service.plan(projectDeclaring(DESCRIPTOR), "Algorithms", "Models");

        assertEquals("rules/Models.xlsx", plan.model().path());
        assertFalse(plan.model().declared());
    }

    @Test
    void namesTheModuleAfterTheNameItWasAskedFor() {
        var plan = service.plan(projectDeclaring(DESCRIPTOR), "Pricing", null);

        // A name of the reader's own is answered with a workbook of its own, beside the default one.
        assertEquals("Pricing", plan.algorithm().name());
        assertEquals("rules/Pricing.xlsx", plan.algorithm().path());
        assertEquals("Models", plan.model().name());
    }

    @Test
    void writesOverTheWorkbookOfAModuleDeclaredWithoutAName() {
        var declared = """
                <project>
                    <name>Rates</name>
                    <modules>
                        <module>
                            <rules-root path="api/Algorithms.xlsx"/>
                        </module>
                    </modules>
                </project>
                """;

        var plan = service.plan(projectDeclaring(declared), "Algorithms", "Models");

        // The engine names such a module after the workbook it reads, so this is that module, and its
        // workbook is what the generation replaces — not a second one laid beside it.
        assertEquals("api/Algorithms.xlsx", plan.algorithm().path());
        assertTrue(plan.algorithm().declared());
    }

    @Test
    void writesOverTheWorkbookAWildcardAlreadyNamesTheModuleBy() {
        var declared = """
                <project>
                    <name>Rates</name>
                    <modules>
                        <module>
                            <rules-root path="rules/*.xlsx"/>
                        </module>
                    </modules>
                </project>
                """;

        var plan = service.plan(projectDeclaring(declared), "Algorithms", "Models");

        // The wildcard already contributes a module called Algorithms from rules/Algorithms.xlsx.
        assertEquals("rules/Algorithms.xlsx", plan.algorithm().path());
        assertTrue(plan.algorithm().declared());
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
                () -> service.generateTables(mock(RulesProject.class), request));

        // Named the same but for their case: one workbook cannot be two modules.
        assertEquals("openl.error.409.projects.openapi.module-path.same.message", refused.getErrorCode());
    }

    private static OpenApiGenerationRequest asked(String algorithmModuleName, String modelModuleName) {
        return new OpenApiGenerationRequest("openapi.json", algorithmModuleName, "rules/Algorithms.xlsx",
                modelModuleName, "rules/Models.xlsx");
    }

    /** A project whose rules.xml reads as the given text. */
    private RulesProject projectDeclaring(String descriptor) {
        var project = mock(RulesProject.class);
        when(project.hasArtefact(ProjectDescriptor.FILE_NAME)).thenReturn(true);
        var root = mock(FileRoot.class);
        when(roots.of(project)).thenReturn(root);
        var resource = mock(AProjectResource.class);
        try {
            when(resource.getContent())
                    .thenReturn(new ByteArrayInputStream(descriptor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
        when(files.getResource(root, ProjectDescriptor.FILE_NAME, null)).thenReturn(resource);
        return project;
    }
}

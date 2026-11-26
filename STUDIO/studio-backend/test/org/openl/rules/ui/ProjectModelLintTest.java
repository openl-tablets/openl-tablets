package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.openl.rules.ui.ProjectModelTestProjects.awaitCompiled;
import static org.openl.rules.ui.ProjectModelTestProjects.module;
import static org.openl.rules.ui.ProjectModelTestProjects.writeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.message.OpenLLintMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;

/**
 * The messages of the linter about the datatypes and the fields no table of the project uses: which modules they are
 * told about, and while which module is open.
 */
class ProjectModelLintTest {

    private static final String UNUSED_AGENT_CODE =
            "Field 'agentCode' of datatype 'Policy' is not used in the current project.";
    private static final String UNUSED_ADDRESS = "Datatype 'Address' is not used in the current project.";

    @TempDir
    Path dir;

    private ProjectDescriptor shared;
    private ProjectDescriptor policies;
    private ProjectModel model;

    @BeforeEach
    void writeProjects() throws Exception {
        shared = ProjectResolver.getInstance().resolve(writeShared());
        policies = ProjectResolver.getInstance().resolve(writePolicies());
        var studio = mock(WebStudio.class);
        when(studio.getProjectResolver()).thenReturn(ProjectResolver.getInstance());
        when(studio.getAllProjects()).thenReturn(List.of(shared, policies));
        model = new ProjectModel(studio);
    }

    /** Lets go of what the model compiled, its compilation thread included. */
    @AfterEach
    void destroyModel() {
        model.destroy();
    }

    /**
     * A module of the project depends on a module of another project, which declares a datatype no table uses. That
     * datatype is another project's to use, so only the field of the project's own datatype is told. It is an
     * information message, which leaves the project with no warning.
     */
    @Test
    void tellsAboutTheModulesOfTheProjectOnly() throws Exception {
        model.setModuleInfo(module(policies, "Rating"));
        awaitCompiled(model);

        assertEquals(List.of(UNUSED_AGENT_CODE), lintOf(model.getModuleMessages()));
        var status = model.getCompilationStatus();
        assertEquals(List.of(UNUSED_AGENT_CODE), lintOf(status.getAllMessage()));
        assertEquals(List.of(Severity.INFO), status.getAllMessage()
                .stream()
                .filter(OpenLLintMessage.class::isInstance)
                .map(OpenLMessage::getSeverity)
                .toList());
        assertEquals(0, status.getWarningsCount());
    }

    /**
     * A module of another project, which a module of the project depends on, holds a rule with a compilation error.
     * The error leaves the tables of the project as they are, so their messages are told all the same.
     */
    @Test
    void tellsAboutTheProjectThoughAModuleOfAnotherProjectHasAnError() throws Exception {
        writeModule(dir.resolve("Shared"), "Types", new String[][]{{"Datatype Address"}, {"String", "city"}},
                new String[][]{{"Method String broken()"}, {"return unknownName;"}});

        model.setModuleInfo(module(policies, "Rating"));
        awaitCompiled(model);

        assertTrue(model.getCompilationStatus().getErrorsCount() > 0, "the module of the other project has an error");
        assertEquals(List.of(UNUSED_AGENT_CODE), lintOf(model.getModuleMessages()));
    }

    /**
     * A rule of a module of the project that no module depends on has a compilation error: what the rule reads is not
     * known, so nothing is told.
     */
    @Test
    void tellsNothingWhileARuleOfTheProjectHasAnError() throws Exception {
        writeModule(dir.resolve("Policies"), "Rating",
                new String[][]{{"Environment"}, {"dependency", "Model"}, {"dependency", "Types"}},
                new String[][]{{"Method String stateOf(Policy policy)"}, {"return policy.state;"}},
                new String[][]{{"Method String broken()"}, {"return unknownName;"}});

        model.setModuleInfo(module(policies, "Rating"));
        awaitCompiled(model);

        assertTrue(model.getCompilationStatus().getErrorsCount() > 0, "the rule has an error");
        assertEquals(List.of(), lintOf(model.getModuleMessages()));
        assertEquals(List.of(), lintOf(model.getCompilationStatus().getAllMessage()));
    }

    /**
     * A module of a project of several compiled alone shows no message of the linter, as the modules that may use its
     * datatypes are not compiled with it. The messages come back with the module opened as usual.
     */
    @Test
    void hidesTheMessagesWhileAModuleOfSeveralCompilesAlone() throws Exception {
        model.setModuleInfo(module(policies, "Rating"));
        awaitCompiled(model);

        var alone = module(policies, "Model");
        alone.getWebstudioConfiguration().setCompileThisModuleOnly(true);
        model.setModuleInfo(alone);

        assertEquals(List.of(), lintOf(model.getModuleMessages()));
        assertEquals(List.of(), lintOf(model.getCompilationStatus().getAllMessage()));

        model.setModuleInfo(module(policies, "Rating"));
        awaitCompiled(model);

        assertEquals(List.of(UNUSED_AGENT_CODE), lintOf(model.getModuleMessages()));
    }

    /**
     * A module read again, as after a write, drops the messages of the linter: they would point at the cells as they
     * were. The compilation of the project the reload starts tells them again.
     */
    @Test
    void dropsTheMessagesUntilTheProjectCompilesAgainAfterAReload() throws Exception {
        model.setModuleInfo(module(policies, "Rating"));
        awaitCompiled(model);
        assertEquals(List.of(UNUSED_AGENT_CODE), lintOf(model.getModuleMessages()));

        synchronized (model) {
            // The compilation of the project cannot end while the model is held
            model.setModuleInfo(module(policies, "Rating"), ReloadType.RELOAD);
            assertEquals(List.of(), lintOf(model.getModuleMessages()));
            assertEquals(List.of(), lintOf(model.getCompilationStatus().getAllMessage()));
        }
        awaitCompiled(model);

        assertEquals(List.of(UNUSED_AGENT_CODE), lintOf(model.getModuleMessages()));
    }

    /** A project of one module is the module: compiled alone, it is checked all the same. */
    @Test
    void tellsAboutAProjectOfOneModuleCompiledAlone() throws Exception {
        var types = module(shared, "Types");
        types.getWebstudioConfiguration().setCompileThisModuleOnly(true);
        model.setModuleInfo(types);

        assertEquals(List.of(UNUSED_ADDRESS), lintOf(model.getModuleMessages()));
        assertEquals(List.of(UNUSED_ADDRESS), lintOf(model.getCompilationStatus().getAllMessage()));
    }

    private static List<String> lintOf(Collection<OpenLMessage> messages) {
        return messages.stream()
                .filter(OpenLLintMessage.class::isInstance)
                .map(OpenLMessage::getSummary)
                .toList();
    }

    /** A project of one module declaring a datatype no table of it uses. */
    private Path writeShared() throws IOException {
        var project = Files.createDirectories(dir.resolve("Shared"));
        writeModule(project, "Types", new String[][]{{"Datatype Address"}, {"String", "city"}});
        return project;
    }

    /**
     * A project of two modules: Model declares a datatype, Rating reads one of its fields. Rating depends on Model,
     * and on the module of Shared too.
     */
    private Path writePolicies() throws IOException {
        var project = Files.createDirectories(dir.resolve("Policies"));
        Files.writeString(project.resolve("rules.xml"), """
                <project>
                  <name>Policies</name>
                  <modules>
                    <module>
                      <name>Model</name>
                      <rules-root path="Model.xls"/>
                    </module>
                    <module>
                      <name>Rating</name>
                      <rules-root path="Rating.xls"/>
                    </module>
                  </modules>
                  <dependencies>
                    <dependency>
                      <name>Shared</name>
                      <autoIncluded>false</autoIncluded>
                    </dependency>
                  </dependencies>
                </project>
                """);
        writeModule(project, "Model",
                new String[][]{{"Datatype Policy"}, {"String", "state"}, {"String", "agentCode"}});
        writeModule(project, "Rating",
                new String[][]{{"Environment"}, {"dependency", "Model"}, {"dependency", "Types"}},
                new String[][]{{"Method String stateOf(Policy policy)"}, {"return policy.state;"}});
        return project;
    }
}

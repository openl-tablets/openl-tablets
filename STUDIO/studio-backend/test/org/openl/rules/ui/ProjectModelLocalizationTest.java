package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.message.OpenLMessage;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;

/**
 * The localization of a project opened in a session: its files checked as the first step of the compilation, the
 * calls of {@code msg} as its modules compile, and both again once the files are written, without compiling.
 */
class ProjectModelLocalizationTest {

    private static final String CALL_WARNING = "'greeting' has 1 placeholder, but 2 values are passed.";
    private static final String TWICE = "Written twice, on lines 1 and 2: the last text wins.";

    @TempDir
    Path dir;

    private Path folder;
    private ProjectDescriptor project;
    private ProjectModel model;

    @BeforeEach
    void writeProject() throws Exception {
        folder = Files.createDirectories(dir.resolve("Greetings"));
        Files.writeString(folder.resolve("rules.xml"), """
                <project>
                  <name>Greetings</name>
                  <modules>
                    <module>
                      <name>Main</name>
                      <rules-root path="Main.xls"/>
                    </module>
                  </modules>
                </project>
                """);
        writeModule(folder.resolve("Main.xls"));
        Files.createDirectories(folder.resolve("i18n"));
        writeMessages("greeting = Hello, {0}!\ngreeting = Hi, {0}!\n");
        project = ProjectResolver.getInstance().resolve(folder);
        var studio = mock(WebStudio.class);
        when(studio.getProjectResolver()).thenReturn(ProjectResolver.getInstance());
        when(studio.getAllProjects()).thenReturn(List.of(project));
        model = new ProjectModel(studio);
    }

    @AfterEach
    void destroyModel() {
        model.destroy();
    }

    @Test
    void checksTheFilesAsTheFirstStepAndTheCallsAsTheModulesCompile() throws Exception {
        model.setModuleInfo(main());
        awaitCompiled();

        var status = model.getCompilationStatus();

        // The files are a step of their own, before the one module.
        assertEquals(2, status.getModulesCount());
        assertEquals(2, status.getModulesCompiled());
        assertEquals(List.of(TWICE, CALL_WARNING), summariesOf(status.getAllMessage()));
        // The warning about the call belongs to the table it is written in, as the warnings of the compilation do.
        assertTrue(summariesOf(model.getModuleMessages()).contains(CALL_WARNING));
    }

    @Test
    void checksAWriteOfTheFilesAgainWithoutCompilingAnything() throws Exception {
        model.setModuleInfo(main());
        awaitCompiled();
        var compiled = model.getCurrentCompilation();

        writeMessages("greeting = Hello, {0} and {1}!\n");
        model.checkLocalizationAgain();

        assertSame(compiled, model.getCurrentCompilation(), "nothing is compiled again");
        assertTrue(model.getCompilationStatus().getAllMessage().isEmpty());
        assertTrue(model.getModuleMessages().isEmpty());
        // The rules read the new texts from now on.
        var module = (XlsModuleOpenClass) model.getCompiledOpenClass().getOpenClassWithErrors();
        assertEquals("Hello, {0} and {1}!", module.getMessageSource().getMessageBundle(Locale.ROOT).msg("greeting"));
    }

    @Test
    void compilesNothingWhenTheFilesOfAProjectNoModuleOfWhichIsOpenAreWritten() {
        var before = model.getCurrentCompilation();

        model.checkLocalizationAgain();

        assertSame(before, model.getCurrentCompilation(), "no compilation is started");
        assertFalse(model.isCompilationInProgress());
        assertEquals(0, model.getCompilationStatus().getModulesCount());
    }

    @Test
    void countsNoStepForAProjectWithoutLocalizationFiles() throws Exception {
        Files.delete(folder.resolve("i18n/message.properties"));
        model.setModuleInfo(main());
        awaitCompiled();

        var status = model.getCompilationStatus();

        assertEquals(1, status.getModulesCount());
        // The call reads a key no file has.
        assertEquals(List.of("Key 'greeting' has no default text: a language without a text of its own returns the "
                + "key."), summariesOf(status.getAllMessage()));
    }

    private Module main() {
        return project.getModules().getFirst();
    }

    private void awaitCompiled() {
        try {
            model.getCurrentCompilation().future().join();
        } catch (CancellationException | CompletionException ended) {
            // However the compilation ended, what it built is what the model answers with.
        }
    }

    private void writeMessages(String text) throws IOException {
        Files.writeString(folder.resolve("i18n/message.properties"), text);
    }

    private static List<String> summariesOf(Collection<OpenLMessage> messages) {
        return messages.stream().map(OpenLMessage::getSummary).toList();
    }

    /** A module of one method that asks for a message with more values than its text takes. */
    private static void writeModule(Path file) throws IOException {
        try (var book = new HSSFWorkbook()) {
            var sheet = book.createSheet("Main");
            sheet.createRow(0).createCell(0).setCellValue("Method String hello()");
            sheet.createRow(1).createCell(0).setCellValue("return msg(\"greeting\", \"Ann\", \"Bob\");");
            try (OutputStream out = Files.newOutputStream(file)) {
                book.write(out);
            }
        }
    }
}

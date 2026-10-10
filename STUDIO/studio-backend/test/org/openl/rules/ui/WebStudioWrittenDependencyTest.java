package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.jupiter.api.Test;

import org.openl.CompiledOpenClass;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;

/**
 * What the session compiles a module from when the module it opens next is not the one a write changed.
 *
 * @author Yury Molchan
 */
class WebStudioWrittenDependencyTest extends AbstractWorkbookGeneratingTest {

    private static final String REPOSITORY = "design";

    private String projectName;

    @Test
    void buildsTheModuleUsingTheWrittenOneFromTheWorkbookAsWritten() throws Exception {
        writeModules();
        var studio = studio();
        open(studio, "Main");
        assertEquals(150.0, invoke(studio.getModel().getOpenedModuleCompiledOpenClass(), "premium"));

        writeBase(studio, "2.0");

        // A client reading through the module that uses the written one, as a test run `fromModule=Main` does.
        open(studio, "Main");
        assertEquals(200.0,
                invoke(studio.getModel().getOpenedModuleCompiledOpenClass(), "premium"),
                "the module is compiled against the workbook as written");
        assertEquals(200.0,
                invoke(studio.getModel().getCompiledOpenClass(), "premium"),
                "the project compiled as a whole answers for the workbook as written");
        assertFalse(studio.isAwaitingRecompile(), "nothing compiled from the workbook before the write is left");
    }

    @Test
    void buildsTheWrittenModuleWhenItIsOpenedAfterAnother() throws Exception {
        writeModules();
        var studio = studio();
        open(studio, "Main");

        writeBase(studio, "2.0");
        open(studio, "Other");
        open(studio, "Base");

        assertEquals(2.0, invoke(studio.getModel().getOpenedModuleCompiledOpenClass(), "rate"));
    }

    @Test
    void leavesTheModulesNotUsingTheWrittenOneAsCompiled() throws Exception {
        writeModules();
        var studio = studio();
        open(studio, "Other");
        var other = studio.getModel().getOpenedModuleCompiledOpenClass();
        open(studio, "Main");

        writeBase(studio, "2.0");
        open(studio, "Other");

        assertSame(other, studio.getModel().getOpenedModuleCompiledOpenClass(), "a module nobody wrote to");
    }

    /**
     * A session whose model compiles the project written into the temporary folder, as the one design repository
     * holds it.
     */
    private WebStudio studio() throws Exception {
        var studio = WebStudioMocks.studio(new ProjectModel(mock(WebStudio.class)));
        var project = ProjectResolver.getInstance().resolve(tempFolder);
        projectName = project.getName();
        doReturn(project).when(studio).getProjectByName(REPOSITORY, project.getName());
        doReturn(null).when(studio).getProject(any(), any());
        doReturn(true).when(studio).isAutoCompile();
        return studio;
    }

    /** Opens the module as a request naming it does, and waits for the project to be compiled behind it. */
    private void open(WebStudio studio, String module) {
        studio.init(REPOSITORY, null, projectName, module);
        try {
            studio.getModel().getCurrentCompilation().future().join();
        } catch (CancellationException | CompletionException ended) {
            // However the compilation ended, what it built is what the model answers with.
        }
    }

    /**
     * Writes the rate into Base as a write of a table does: through the module the table belongs to, which then
     * asks for it to be built again.
     */
    private void writeBase(WebStudio studio, String rate) throws IOException {
        open(studio, "Base");
        writeModule("Base", new String[][]{{"Method Double rate()"}, {"return " + rate + ";"}});
        studio.recompileCurrentModule();
    }

    private void writeModules() throws IOException {
        writeModule("Base", new String[][]{{"Method Double rate()"}, {"return 1.5;"}});
        writeModule("Main",
                new String[][]{{"Environment"}, {"dependency", "Base"}},
                new String[][]{{"Method Double premium()"}, {"return rate() * 100;"}});
        writeModule("Other", new String[][]{{"Method String hello()"}, {"return \"Hi\";"}});
    }

    private void writeModule(String name, String[][]... tables) throws IOException {
        try (var book = new HSSFWorkbook()) {
            var sheet = book.createSheet(name);
            for (var table : tables) {
                createTable(sheet, table);
            }
            writeBook(book, name + ".xls");
        }
    }

    private static Object invoke(CompiledOpenClass compiled, String method) {
        var openClass = compiled.getOpenClassWithErrors();
        var env = new SimpleRulesVM().getRuntimeEnv();
        return openClass.getMethod(method, IOpenClass.EMPTY).invoke(openClass.newInstance(env), new Object[0], env);
    }
}

package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.dependency.CompiledDependency;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.webstudio.dependencies.WebStudioWorkspaceRelatedDependencyManager;

/**
 * Which modules of a dependency project the session compiles and counts: all of them for a project included
 * automatically, only those used for one that is not — also when a module of another project is opened while the
 * project opened before is still compiling.
 */
class ProjectModelDependencyModulesTest {

    private static final int RATES_MODULES = 20;

    @TempDir
    Path dir;

    private ProjectDescriptor rates;
    private ProjectDescriptor policies;
    private ProjectDescriptor quotes;
    private ProjectModel model;

    @BeforeEach
    void writeProjects() throws Exception {
        rates = ProjectResolver.getInstance().resolve(writeRates());
        policies = ProjectResolver.getInstance().resolve(writeUsingRates("Policies", "Policy", false));
        quotes = ProjectResolver.getInstance().resolve(writeUsingRates("Quotes", "Quote", true));
        var studio = mock(WebStudio.class);
        when(studio.getProjectResolver()).thenReturn(ProjectResolver.getInstance());
        when(studio.getAllProjects()).thenReturn(List.of(rates, policies, quotes));
        model = new ProjectModel(studio);
    }

    /** Lets go of what the model compiled, its compilation thread included. */
    @AfterEach
    void destroyModel() {
        model.destroy();
    }

    /**
     * The project opened next uses one module of the first project and does not include it whole
     * ({@code autoIncluded = false}). The rest of the first project is no longer compiled, though the dependency
     * manager stays the same for both projects.
     */
    @Test
    void stopsCompilingTheProjectOpenedBeforeWhenAnotherProjectIsOpened() throws Exception {
        model.setModuleInfo(module(rates, "Rate01"));
        var manager = model.getWebStudioWorkspaceDependencyManager();
        var replaced = model.getCurrentCompilation();

        model.setModuleInfo(module(policies, "Policy"));
        awaitCompiled(model);

        assertSame(manager, model.getWebStudioWorkspaceDependencyManager(), "the manager serves both projects");
        assertTrue(replaced.future().isCancelled(), "the compilation of the first project ends without a result");
        assertFalse(model.getCompiledOpenClass().hasErrors(),
                String.valueOf(model.getCompiledOpenClass().getAllMessages()));
        assertNull(loaderOf(rates).getRefToCompiledDependency(), "the first project is not compiled as a whole");
        var compiledRates = manager.getDependencyLoaders()
                .stream()
                .filter(loader -> !loader.isProjectLoader() && loader.getProject().equals(rates))
                .filter(loader -> loader.getRefToCompiledDependency() != null)
                .count();
        assertTrue(compiledRates < RATES_MODULES, "only some modules of the first project are compiled");
        assertCounted(2, "the module opened and the one module of the first project it uses");
        var tables = model.getAllTableSyntaxNodes()
                .stream()
                .map(table -> table.getHeaderLineValue().getValue())
                .collect(Collectors.toSet());
        assertEquals(Set.of("Environment", "Method Double premium()", "Method Double rate()"), tables,
                "the tables of the modules compiled for the first project alone are left out");
    }

    /**
     * A module a background load is compiling when the load is stopped is finished and kept, and so is what the load
     * hands over.
     */
    @Test
    void keepsTheModuleAStoppedBackgroundLoadFinishes() throws Exception {
        var manager = managerOf();
        try {
            // Stopped while the module is compiling: the one it uses is compiled, the module itself is not yet.
            manager.registerOnCompilationCompleteListener((loader, compiled) -> {
                if (isModule(loader, "All")) {
                    manager.stopBackgroundLoads();
                }
            });
            var policy = AbstractDependencyManager.buildResolvedDependency(module(policies, "Policy"));
            var handed = new CompletableFuture<CompiledDependency>();
            manager.loadDependencyAsync(policy, handed::complete);

            assertFalse(handed.get(1, TimeUnit.MINUTES).getCompiledOpenClass().hasErrors());
            assertNotNull(manager.findDependencyLoader(policy).getRefToCompiledDependency(), "the module is kept");
        } finally {
            manager.shutdown();
        }
    }

    /** A stopped background load of a project starts no other module, keeps no project and hands nothing over. */
    @Test
    void startsNothingMoreOnceABackgroundLoadIsStopped() throws Exception {
        var manager = managerOf();
        try {
            var first = new AtomicReference<IDependencyLoader>();
            var next = new CompletableFuture<CompiledDependency>();
            manager.registerOnCompilationCompleteListener((loader, compiled) -> {
                if (!loader.isProjectLoader() && first.compareAndSet(null, loader)) {
                    manager.stopBackgroundLoads();
                    // Asked for after the stop, it runs once the stopped one has ended. It is compiled already.
                    manager.loadDependencyAsync(loader.getDependency(), next::complete);
                }
            });
            var handed = new CompletableFuture<CompiledDependency>();
            manager.loadDependencyAsync(AbstractDependencyManager.buildResolvedDependency(rates), handed::complete);

            next.get(1, TimeUnit.MINUTES);
            assertFalse(handed.isDone(), "the stopped load hands nothing over");
            var projectLoader = manager.findDependencyLoader(AbstractDependencyManager.buildResolvedDependency(rates));
            assertNull(projectLoader.getRefToCompiledDependency(), "the project is not kept");
            var compiled = manager.getDependencyLoaders()
                    .stream()
                    .filter(loader -> !loader.isProjectLoader() && loader.getRefToCompiledDependency() != null)
                    .toList();
            assertEquals(List.of(first.get()), compiled, "no module is started after the stop");
        } finally {
            manager.shutdown();
        }
    }

    private WebStudioWorkspaceRelatedDependencyManager managerOf() {
        return new WebStudioWorkspaceRelatedDependencyManager(List.of(rates, policies),
                getClass().getClassLoader(),
                false,
                Map.of(),
                false);
    }

    private static boolean isModule(IDependencyLoader loader, String name) {
        return !loader.isProjectLoader() && loader.getModule().getName().equals(name);
    }

    /** A dependency project that is not included automatically counts with the one module the project uses. */
    @Test
    void countsOnlyTheModuleUsedOfAProjectNotIncludedAutomatically() throws Exception {
        model.setModuleInfo(module(policies, "Policy"));
        awaitCompiled(model);

        assertTrue(model.isProjectCompilationCompleted(), "the project is compiled");
        assertCounted(2, "the module opened and the one module of Rates it uses");
        assertEquals(List.of("Policy", "All"), modulesCompiledWithTheProject());
    }

    /** A dependency project included automatically counts with all its modules. */
    @Test
    void countsEveryModuleOfAProjectIncludedAutomatically() throws Exception {
        model.setModuleInfo(module(quotes, "Quote"));
        awaitCompiled(model);

        assertTrue(model.isProjectCompilationCompleted(), "the project is compiled");
        assertFalse(model.getCompiledOpenClass().hasErrors(),
                String.valueOf(model.getCompiledOpenClass().getAllMessages()));
        assertCounted(1 + RATES_MODULES, "the module opened and every module of Rates");
    }

    private void assertCounted(int modules, String message) {
        var status = model.getCompilationStatus();
        assertEquals(modules, status.getModulesCount(), message);
        assertEquals(modules, status.getModulesCompiled(), message);
    }

    private List<String> modulesCompiledWithTheProject() {
        return model.getWebStudioWorkspaceDependencyManager()
                .findLoadersCompiledWith(model.getModuleInfo().getProject())
                .stream()
                .filter(loader -> !loader.isProjectLoader())
                .map(loader -> loader.getModule().getName())
                .toList();
    }

    /** Opening the first project again compiles it whole on the manager its compilation was stopped on. */
    @Test
    void compilesTheStoppedProjectWhenItIsOpenedAgain() throws Exception {
        model.setModuleInfo(module(rates, "Rate01"));
        model.setModuleInfo(module(policies, "Policy"));
        awaitCompiled(model);

        model.setModuleInfo(module(rates, "Rate02"));
        awaitCompiled(model);

        assertTrue(model.isProjectCompilationCompleted(), "the first project is compiled");
        assertNotNull(loaderOf(rates).getRefToCompiledDependency(), "the first project is compiled as a whole");
        assertFalse(model.getCompiledOpenClass().hasErrors(),
                String.valueOf(model.getCompiledOpenClass().getAllMessages()));
    }

    /** Opening another module of a project that is still compiling ends with the project compiled. */
    @Test
    void compilesTheProjectWhenAnotherOfItsModulesIsOpenedWhileItCompiles() throws Exception {
        model.setModuleInfo(module(rates, "Rate01"));
        var replaced = model.getCurrentCompilation();

        model.setModuleInfo(module(rates, "Rate02"));
        awaitCompiled(model);

        assertTrue(replaced.future().isDone(), "the compilation replaced ends");
        assertTrue(model.isProjectCompilationCompleted(), "the project is compiled");
        assertNotNull(loaderOf(rates).getRefToCompiledDependency(), "the project is compiled as a whole");
    }

    private IDependencyLoader loaderOf(ProjectDescriptor project) {
        return model.getWebStudioWorkspaceDependencyManager()
                .findDependencyLoader(AbstractDependencyManager.buildResolvedDependency(project));
    }

    private static Module module(ProjectDescriptor project, String name) {
        return project.getModules()
                .stream()
                .filter(module -> module.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static void awaitCompiled(ProjectModel model) {
        try {
            model.getCurrentCompilation().future().join();
        } catch (CancellationException | CompletionException ended) {
            // However the compilation ended, what it built is what the model answers with.
        }
    }

    /** A project of many modules, one of which the other project uses. */
    private Path writeRates() throws IOException {
        var project = Files.createDirectories(dir.resolve("Rates"));
        writeModule(project, "All", new String[][]{{"Method Double rate()"}, {"return 1.5;"}});
        for (var i = 1; i < RATES_MODULES; i++) {
            writeModule(project, "Rate%02d".formatted(i),
                    new String[][]{{"Method Double rate%02d()".formatted(i)}, {"return %d;".formatted(i)}});
        }
        return project;
    }

    /**
     * A project of one module using the rate of Rates. Included automatically, Rates is included whole; otherwise the
     * module names the one module of Rates it uses.
     */
    private Path writeUsingRates(String name, String module, boolean autoIncluded) throws IOException {
        var project = Files.createDirectories(dir.resolve(name));
        Files.writeString(project.resolve("rules.xml"), """
                <project>
                  <name>%s</name>
                  <modules>
                    <module>
                      <name>%s</name>
                      <rules-root path="%s.xls"/>
                    </module>
                  </modules>
                  <dependencies>
                    <dependency>
                      <name>Rates</name>
                      <autoIncluded>%s</autoIncluded>
                    </dependency>
                  </dependencies>
                </project>
                """.formatted(name, module, module, autoIncluded));
        var premium = new String[][]{{"Method Double premium()"}, {"return rate() * 100;"}};
        if (autoIncluded) {
            writeModule(project, module, premium);
        } else {
            writeModule(project, module, new String[][]{{"Environment"}, {"dependency", "All"}}, premium);
        }
        return project;
    }

    /** Writes a module of one sheet holding the tables one under another, an empty row between them. */
    private static void writeModule(Path project, String name, String[][]... tables) throws IOException {
        try (var book = new HSSFWorkbook()) {
            var sheet = book.createSheet(name);
            var rowIndex = 0;
            for (var table : tables) {
                for (var cells : table) {
                    var row = sheet.createRow(rowIndex++);
                    for (var column = 0; column < cells.length; column++) {
                        row.createCell(column).setCellValue(cells[column]);
                    }
                }
                rowIndex++;
            }
            try (OutputStream out = Files.newOutputStream(project.resolve(name + ".xls"))) {
                book.write(out);
            }
        }
    }
}

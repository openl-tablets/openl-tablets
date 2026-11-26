package org.openl.rules.maven;

import java.io.File;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import org.openl.OpenClassUtil;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.markdown.MarkdownProjectWriter;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.validation.openapi.OpenApiProjectValidator;

/**
 * Compiles and validates an OpenL Tablets project.
 *
 * @author Yury Molchan
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public final class CompileMojo extends TestsSkippingMojo {

    /**
     * Additional options for compilation defined externally,
     * such as external dependencies and overridden system properties.
     */
    @Parameter
    private Map<String, Object> externalParameters;

    @Parameter(defaultValue = "${project.compileClasspathElements}", required = true, readonly = true)
    private List<String> classpath;

    /**
     * The build folder of the project, wherever the build names it. The Markdown copy of the project leaves it out
     * when the sources hold it.
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
    File buildDirectory;

    // OpenClassUtil releases the class loader and also clears the OpenL caches bound to it.
    @SuppressWarnings("java:S2093")
    @Override
    public void execute(String sourcePath, boolean hasDependencies) throws Exception {
        var urls = toURLs(classpath);
        ClassLoader classLoader = null;
        try {
            classLoader = new URLClassLoader(urls, SimpleProjectEngineFactory.class.getClassLoader());

            var start = System.nanoTime();

            var builder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>();
            if (hasDependencies) {
                builder.setWorkspace(workspaceFolder.getPath());
            }
            SimpleProjectEngineFactory<?> factory = builder.setProject(sourcePath)
                    .setClassLoader(classLoader)
                    .setExecutionMode(true)
                    .setExternalParameters(externalParameters)
                    .build();
            if (isTestsSkipped()) {
                // Before the instantiation strategy is built: it takes the modules of the descriptor once.
                leaveTestsOut(factory.getProjectDescriptor());
            }

            var openLRules = new OpenApiProjectValidator()
                    .validate(factory.getProjectDescriptor(), factory.getRulesInstantiationStrategy());
            openLRules.throwErrorExceptionsIfAny();
            var openClass = openLRules.getOpenClass();
            var end = System.nanoTime();

            Collection<OpenLMessage> warnMessages = OpenLMessagesUtils
                    .filterMessagesBySeverity(openLRules.getAllMessages(), Severity.WARN);
            info("Compilation has finished.");
            info("DataTypes    : ", openClass.getTypes().size());
            info("Methods      : ", openClass.getMethods().size());
            info("Warnings     : ", warnMessages.size());
            info("Time elapsed : ", (end - start) / 10_000_000L / 100.0, " s");
            if (markdown) {
                writeMarkdown(sourcePath, hasDependencies, classLoader);
            }
        } finally {
            OpenClassUtil.releaseClassLoader(classLoader);
        }
    }

    /**
     * Removes the modules of the {@code tests} folder from the project, so that they are not compiled.
     * <p>
     * The build log tells how many modules are left out. A project without such modules is not changed.
     */
    void leaveTestsOut(ProjectDescriptor descriptor) {
        var removed = PackageMojo.leaveTestsModulesOut(descriptor);
        if (removed > 0) {
            info("Tests are skipped: the '", PackageMojo.TESTS_FOLDER, "' folder is left out of compilation, modules: ",
                    removed);
        }
    }

    /**
     * Writes the copy of the project with its workbooks written as Markdown, compiled again with the tables kept, which
     * the execution mode drops.
     */
    private void writeMarkdown(String sourcePath, boolean hasDependencies, ClassLoader classLoader) throws Exception {
        var builder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>();
        if (hasDependencies) {
            builder.setWorkspace(workspaceFolder.getPath());
        }
        var factory = builder.setProject(sourcePath)
                .setClassLoader(classLoader)
                .setExecutionMode(false)
                .setExternalParameters(externalParameters)
                .build();
        var written = MarkdownProjectWriter.write(factory, markdownDirectory.toPath(), leftOutOfCopy(sourcePath));
        info("Markdown     : ", markdownDirectory);
        for (var module : written.converted()) {
            info("  module '", module.name(), "' ", module.workbook(), " (", module.workbookBytes(), " bytes) -> ",
                    module.folder(), " (", module.files(), " files, ", module.markdownBytes(), " bytes)");
        }
        for (var module : written.kept()) {
            warn("  module '", module.name(), "' stays a workbook ", module.path(), ": ",
                    String.join("; ", module.reasons()));
        }
        for (var module : written.dropped()) {
            info("  module '", module.name(), "' ", module.path(), " is dropped with the folder it lies in");
        }
    }

    /**
     * The files of the sources the Markdown copy leaves out: the build folder of the project and the {@code pom.xml} of
     * the sources. Both lie in the sources of a project kept in the folder of its build. The archive leaves them out
     * too, unless {@code package} writes it into another folder than the build folder.
     */
    List<Path> leftOutOfCopy(String sourcePath) {
        return List.of(buildDirectory.toPath(), Path.of(sourcePath, PackageMojo.POM_FILE_NAME));
    }

    @Override
    String getHeader() {
        return "OPENL COMPILATION";
    }
}

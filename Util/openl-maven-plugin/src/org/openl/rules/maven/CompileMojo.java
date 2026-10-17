package org.openl.rules.maven;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import org.openl.CompiledOpenClass;
import org.openl.OpenClassUtil;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.markdown.MarkdownProjectWriter;
import org.openl.rules.project.validation.openapi.OpenApiProjectValidator;
import org.openl.types.IOpenClass;

/**
 * Compiles and validates an OpenL Tablets project.
 *
 * @author Yury Molchan
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public final class CompileMojo extends BaseOpenLMojo {

    /**
     * Additional options for compilation defined externally,
     * such as external dependencies and overridden system properties.
     */
    @Parameter
    private Map<String, Object> externalParameters;

    @Parameter(defaultValue = "${project.compileClasspathElements}", required = true, readonly = true)
    private List<String> classpath;

    // OpenClassUtil releases the class loader and also clears the OpenL caches bound to it.
    @SuppressWarnings("java:S2093")
    @Override
    public void execute(String sourcePath, boolean hasDependencies) throws Exception {
        URL[] urls = toURLs(classpath);
        ClassLoader classLoader = null;
        try {
            classLoader = new URLClassLoader(urls, SimpleProjectEngineFactory.class.getClassLoader());

            long start = System.nanoTime();

            SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<?> builder =
                    new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>();
            if (hasDependencies) {
                builder.setWorkspace(workspaceFolder.getPath());
            }
            SimpleProjectEngineFactory<?> factory = builder.setProject(sourcePath)
                    .setClassLoader(classLoader)
                    .setExecutionMode(true)
                    .setExternalParameters(externalParameters)
                    .build();

            CompiledOpenClass openLRules = new OpenApiProjectValidator()
                    .validate(factory.getProjectDescriptor(), factory.getRulesInstantiationStrategy());
            openLRules.throwErrorExceptionsIfAny();
            IOpenClass openClass = openLRules.getOpenClass();
            long end = System.nanoTime();

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
        var written = MarkdownProjectWriter.write(factory, markdownDirectory.toPath());
        info("Markdown     : ", markdownDirectory);
        for (var module : written.converted()) {
            info("  module '", module.name(), "' ", module.workbook(), " (", module.workbookBytes(), " bytes) -> ",
                    module.folder(), " (", module.files(), " files, ", module.markdownBytes(), " bytes)");
        }
        for (var module : written.kept()) {
            warn("  module '", module.name(), "' stays a workbook ", module.path(), ": ",
                    String.join("; ", module.reasons()));
        }
    }

    @Override
    String getHeader() {
        return "OPENL COMPILATION";
    }
}

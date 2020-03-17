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
import org.openl.types.IOpenClass;

/**
 * Compiles and validates an OpenL project.
 *
 * @author Yury Molchan
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE)
public final class CompileMojo extends BaseOpenLMojo {

    /**
     * Additional options for compilation defined externally, such as external dependencies and overridden system properties.
     */
    @Parameter
    private Map<String, Object> externalParameters;

    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
    private List<String> classpath;

    @Override
    public void execute(String sourcePath, boolean hasDependencies) throws Exception {
        URL[] urls = toURLs(classpath);
        ClassLoader classLoader = null;
        try {
            classLoader = new URLClassLoader(urls, SimpleProjectEngineFactory.class.getClassLoader());

            long memStart = memUsed();
            long start = System.nanoTime();

            SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<?> builder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>();
            if (hasDependencies) {
                builder.setWorkspace(workspaceFolder.getPath());
            }
            SimpleProjectEngineFactory<?> factory = builder.setProject(sourcePath)
                .setClassLoader(classLoader)
                .setExecutionMode(true)
                .setExternalParameters(externalParameters)
                .build();

            CompiledOpenClass openLRules = factory.getCompiledOpenClass();
            IOpenClass openClass = openLRules.getOpenClass();
            long end = System.nanoTime();

            long memEnd = memUsed();

            Collection<OpenLMessage> warnMessages = OpenLMessagesUtils
                .filterMessagesBySeverity(openLRules.getMessages(), Severity.WARN);
            info("Compilation has finished.");
            info("DataTypes    : ", openClass.getTypes().size());
            info("Methods      : ", openClass.getMethods().size());
            info("Warnings     : ", warnMessages.size());
            info("Memory used  : ", (memEnd - memStart) / 262144 / 4.0, " MiB");
            info("Time elapsed : ", (end - start) / 10000000 / 100.0, " s");
        } finally {
            OpenClassUtil.releaseClassLoader(classLoader);
        }
    }

    @Override
    String getHeader() {
        return "OPENL COMPILATION";
    }

    private long memUsed() throws InterruptedException {
        Runtime rt = Runtime.getRuntime();
        rt.gc();
        rt.gc();
        rt.gc();
        Thread.sleep(10);
        rt.runFinalization();
        Thread.sleep(3);
        rt.gc();
        rt.gc();
        Thread.sleep(3);
        rt.gc();
        rt.gc();
        Thread.sleep(3);
        rt.gc();
        Thread.sleep(10);
        return rt.totalMemory() - rt.freeMemory();
    }
}

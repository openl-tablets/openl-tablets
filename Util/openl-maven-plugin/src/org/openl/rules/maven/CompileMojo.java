package org.openl.rules.maven;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.openl.CompiledOpenClass;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.types.IOpenClass;

/**
 * Compile and validate OpenL project
 * 
 * @author Yury Molchan
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE)
public final class CompileMojo extends BaseOpenLMojo {

    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
    private List<String> classpath;

    @Override
    public void execute(String sourcePath) throws Exception {
        URL[] urls = toURLs(classpath);
        ClassLoader classLoader = new URLClassLoader(urls, SimpleProjectEngineFactory.class.getClassLoader());

        SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<?> builder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object>();
        SimpleProjectEngineFactory<?> factory = builder.setProject(sourcePath)
            .setClassLoader(classLoader)
            .setExecutionMode(true)
            .build();

        CompiledOpenClass openLRules = factory.getCompiledOpenClass();
        IOpenClass openClass = openLRules.getOpenClassWithErrors();
        List<OpenLMessage> messages = openLRules.getMessages();
        List<OpenLMessage> warnings = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.WARN);
        info("Compilation has finished.");
        info("DataTypes: " + openClass.getTypes().size());
        info("Methods  : " + openClass.getMethods().size());
        info("Fields   : " + openClass.getFields().size());
        info("Warnings : " + warnings.size());
    }

    @Override
    String getHeader() {
        return "OPENL COMPILATION";
    }
}

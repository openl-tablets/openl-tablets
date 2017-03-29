package org.openl.rules.maven;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;
import org.openl.CompiledOpenClass;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.types.IOpenClass;

/**
 * Compile and validate OpenL project
 * 
 * @author Yury Molchan
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class CompileMojo extends BaseOpenLMojo {

    @Override
    public String execute(CompiledOpenClass openLRules) throws Exception {
        IOpenClass openClass = openLRules.getOpenClass();
        List<OpenLMessage> messages = openLRules.getMessages();
        List<OpenLMessage> warnings = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.WARN);
        info("Compilation has finished.");
        info("DataTypes: " + openClass.getTypes().size());
        info("Methods  : " + openClass.getMethods().size());
        info("Fields   : " + openClass.getFields().size());
        info("Warnings : " + warnings.size());
        return null;
    }

    @Override
    String getHeader() {
        return "OPENL COMPILATION";
    }

    @Override
    ClassLoader composeClassLoader() throws Exception {
        debug("Composing the classloader using the following classpaths:");
        List<String> files = project.getCompileClasspathElements();
        URL[] urls = toURLs(files);
        return new URLClassLoader(urls, this.getClass().getClassLoader());
    }
}

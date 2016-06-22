package org.openl.rules.maven;

import static org.apache.commons.io.filefilter.FileFilterUtils.directoryFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.makeFileOnly;
import static org.apache.commons.io.filefilter.FileFilterUtils.or;
import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenLException;
import org.openl.exception.OpenLExceptionUtils;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolvingException;

/**
 * Compile and validate OpenL project
 * 
 * @author NSamatov
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE)
public class CompileMojo extends BaseOpenLMojo {
    private static final String SEPARATOR = "-------------------------------------------------------------";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            copyOpenLFiles();
        } catch (IOException e) {
            if (getLog().isErrorEnabled()) {
                getLog().error(e.getMessage(), e);
            }
            throw new MojoFailureException(String.format("Cannot copy files to '%s'", openlOutputDirectory), e);
        }

        if (getLog().isInfoEnabled()) {
            getLog().info(String.format("Compiling the project in %s", openlOutputDirectory));
        }

        boolean hasErrors = false;

        ProjectDescriptor projectDescriptor;
        try {
            projectDescriptor = ProjectHelpers.resolveProject(openlOutputDirectory);
            if (projectDescriptor == null) {
                throw new MojoFailureException(String.format("Cannot find OpenL project in directory %s", openlOutputDirectory));
            }
        } catch (ProjectResolvingException e) {
            if (getLog().isErrorEnabled()) {
                getLog().error(e.getMessage(), e);
            }
            throw new MojoFailureException(String.format("Cannot resolve OpenL project in directory %s", openlOutputDirectory), e);
        }

        IDependencyManager dependencyManager = ProjectHelpers.getDependencyManager(projectDescriptor);

        int currentError = 1;
        for (Module module : projectDescriptor.getModules()) {
            RulesInstantiationStrategy instantiationStrategy = RulesInstantiationStrategyFactory.getStrategy(module, false, dependencyManager);
            try {
                CompiledOpenClass compiledOpenClass = instantiationStrategy.compile();
                
                if (compiledOpenClass.hasErrors()) {
                    if (!hasErrors) {
                        hasErrors = true;
                        logHeader();
                    }
                    List<OpenLMessage> errors = OpenLMessagesUtils.filterMessagesBySeverity(compiledOpenClass.getMessages(),
                        Severity.ERROR);
                    for (OpenLMessage message : errors) {
                        if (getLog().isErrorEnabled()) {
                            String errorMessage = "";
                            if (message instanceof OpenLErrorMessage) {
                                OpenLException error = ((OpenLErrorMessage) message).getError();
                                StringWriter stringWriter = new StringWriter();
                                PrintWriter printWriter = new PrintWriter(stringWriter);

                                OpenLExceptionUtils.printError(error, printWriter);
                                printWriter.close();

                                errorMessage = stringWriter.toString();
                            }
                            getLog().error(String.format("  %d. %s", currentError++, errorMessage));
                        }
                    }
                }
            } catch (RulesInstantiationException e) {
                // TODO add new compile error
                if (getLog().isErrorEnabled()) {
                    getLog().error(e.getMessage(), e);
                }
                throw new MojoFailureException(String.format("Failed to compile module '%s'", module.getName()), e);
            }
        }

        if (hasErrors) {
            logFooter();
            throw new MojoFailureException("There are OpenL errors");
        }
    }

    private void copyOpenLFiles() throws IOException {
        if (new File(openlResourcesDirectory).exists()) {
            FileFilter openlFilter = or(directoryFileFilter(),
                makeFileOnly(or(suffixFileFilter(".xml"), suffixFileFilter(".xls"), suffixFileFilter(".xlsx"))));
            FileUtils.copyDirectory(new File(openlResourcesDirectory), new File(openlOutputDirectory), openlFilter);
        } else {
            if (getLog().isWarnEnabled()) {
                getLog().warn(String.format("Can't find openl resources directory %s", openlResourcesDirectory));
            }
        }

        FileUtils.copyDirectory(new File(project.getBuild().getOutputDirectory()), new File(openlOutputDirectory));
    }


    private void logHeader() {
        if (getLog().isErrorEnabled()) {
            getLog().error(SEPARATOR);
            getLog().error("OPENL COMPILATION ERROR");
            getLog().error(SEPARATOR);
        }
    }

    private void logFooter() {
        getLog().error(SEPARATOR);
    }
}

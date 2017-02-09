package org.openl.rules.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import org.openl.CompiledOpenClass;

/**
 * Compile and validate OpenL project
 * 
 * @author Yury Molchan
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE)
public class CompileMojo extends BaseOpenLMojo {
    private static final String SEPARATOR = "-------------------------------------------------------------";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        CompiledOpenClass openLRules;
        try {
            openLRules = compileOpenLRules();
        } catch (Exception e) {
            throw new MojoFailureException("Failed to compile OpenL project", e);
        }

        boolean hasErrors = openLRules.hasErrors();

        if (hasErrors) {
            logHeader();
            try {
                openLRules.getOpenClass();
            } catch (Exception e) {
                getLog().error(e);
            }
            logFooter();
            throw new MojoFailureException("There are OpenL errors");
        }
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

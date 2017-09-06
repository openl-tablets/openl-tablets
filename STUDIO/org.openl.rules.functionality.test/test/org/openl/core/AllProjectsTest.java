package org.openl.core;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AllProjectsTest {

    private static final Logger LOG = LoggerFactory.getLogger(AllProjectsTest.class);

    public static final String DIR = "test-resources/functionality-projects/";
    private Locale defaultLocale;
    private TimeZone defaultTimeZone;

    @Before
    public void setupLocale() {
        defaultLocale = Locale.getDefault();
        defaultTimeZone = TimeZone.getDefault();
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @After
    public void restoreLocale() {
        Locale.setDefault(defaultLocale);
        TimeZone.setDefault(defaultTimeZone);
    }

    @Test
    public void testAllProjects() throws NoSuchMethodException {
        LOG.info(">>> Positive projects tests...");
        boolean hasErrors = false;
        final File sourceDir = new File(DIR);

        if (!sourceDir.exists()) {
            LOG.warn("Tests directory doesn't exist!");
            return;
        }

        for (File file : sourceDir.listFiles()) {
            if (file.isDirectory()) {
                SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object> engineFactoryBuilder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object>();
                engineFactoryBuilder.setExecutionMode(false);
                engineFactoryBuilder.setProject(file.getPath());
                SimpleProjectEngineFactory<Object> engineFactory = engineFactoryBuilder.build();
                
                IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
                CompiledOpenClass compiledOpenClass = null; 
                try {
                    compiledOpenClass = engineFactory.getCompiledOpenClass();
                } catch (ClassNotFoundException | ProjectResolvingException | RulesInstantiationException e) {
                    LOG.error("Compilation fails for [" + file.getName() + "].", e);
                    hasErrors = true;
                    continue;
                } 

                if (compiledOpenClass.hasErrors()) {
                    LOG.error("Compilation errors in [" + file.getName() + "].");
                    LOG.error(compiledOpenClass.getMessages().toString());
                    hasErrors = true;
                    continue;
                }

                IOpenClass openClass = compiledOpenClass.getOpenClass();
                Object target = openClass.newInstance(env);
                int errors = 0;
                for (IOpenMethod method : openClass.getDeclaredMethods()) {
                    if (method instanceof TestSuiteMethod) {
                        TestUnitsResults res = (TestUnitsResults) method.invoke(target, new Object[0], env);
                        final int numberOfFailures = res.getNumberOfFailures();
                        errors += numberOfFailures;
                        if (numberOfFailures != 0) {
                            LOG.error("Errors in [" + file.getName() + "]. Failed test: " + res
                                .getName() + "  Errors #:" + numberOfFailures);
                        }

                    }
                }
                if (errors != 0) {
                    hasErrors = true;
                    LOG.error("Errors in [" + file.getName() + "]. Total failures #: " + errors);
                } else {
                    LOG.info("OK in [" + file.getName() + "]. ");
                }
            } else {
                LOG.error("Expected directory, but has been found  [" + file.getName() + "]");
            }
        }

        assertFalse("Some tests have been failed!", hasErrors);
    }
}

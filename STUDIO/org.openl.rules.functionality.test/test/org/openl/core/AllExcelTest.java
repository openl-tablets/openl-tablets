package org.openl.core;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AllExcelTest {

    private static final Logger LOG = LoggerFactory.getLogger(AllExcelTest.class);

    public static final String DIR = "test-resources/functionality/";
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
    public void testAllExcelFiles() throws NoSuchMethodException {
        LOG.info(">>> Positive tests...");
        boolean hasErrors = false;
        final File sourceDir = new File(DIR);
        
        if (!sourceDir.exists()) {
            LOG.warn("Tests directory doesn't exist!");
            return;
        }

        for (File file : sourceDir.listFiles()) {
            String sourceFile = file.getName();
            IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
            final CompiledOpenClass compiledOpenClass;
            if (file.isFile()) {
                try {
                    new FileInputStream(file).close();
                } catch (Exception ex) {
                    LOG.error("Failed to read file [" + sourceFile + "]", ex);
                    hasErrors = true;
                    continue;
                }

                RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>(DIR + sourceFile);
                engineFactory.setExecutionMode(false);
                compiledOpenClass = engineFactory.getCompiledOpenClass();
            } else {
                SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object> engineFactoryBuilder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object>();
                engineFactoryBuilder.setExecutionMode(false);
                engineFactoryBuilder.setProject(file.getPath());
                SimpleProjectEngineFactory<Object> engineFactory = engineFactoryBuilder.build();

                try {
                    compiledOpenClass = engineFactory.getCompiledOpenClass();
                } catch (ClassNotFoundException | ProjectResolvingException | RulesInstantiationException e) {
                    LOG.error("Compilation fails for [" + sourceFile + "].", e);
                    hasErrors = true;
                    continue;
                }

            }

            if (compiledOpenClass.hasErrors()) {
                LOG.error("Compilation errors in [" + sourceFile + "].");
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
                        LOG.error("Errors in [" + sourceFile + "]. Failed test: " + res
                            .getName() + "  Errors #:" + numberOfFailures);
                    }

                }
            }
            if (errors != 0) {
                hasErrors = true;
                LOG.error("Errors in [" + sourceFile + "]. Total failures #: " + errors);
            } else {
                LOG.info("OK in [" + sourceFile + "]. ");
            }
        }
        
        assertFalse("Some tests have been failed!", hasErrors);
    }
}

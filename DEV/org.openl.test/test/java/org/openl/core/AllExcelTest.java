package org.openl.core;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class AllExcelTest {
    private Locale defaultLocale;

    @Before
    public void setupLocale() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @After
    public void restoreLocale() {
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void testAllExcellFiles() throws NoSuchMethodException {
        boolean hasErrors = false;
        final File sourceDir = new File("./test/rules/functionality");
        final File[] files = sourceDir.listFiles();
        for (File sourceFile : files) {

            RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>(sourceFile);
            engineFactory.setExecutionMode(false);
            IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
            final CompiledOpenClass compiledOpenClass = engineFactory.getCompiledOpenClass();

            if (compiledOpenClass.hasErrors()) {
                System.out.println("!!! Compilation errors in [" + sourceFile.getName() + "].");
                System.out.println(compiledOpenClass.getMessages());
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
                        System.out
                            .println("!!! Errors in [" + sourceFile.getName() + "]. Failed test: " + res.getName());
                    }

                }
            }
            if (errors != 0) {
                hasErrors = true;
                System.out.println("!!! Errors in [" + sourceFile.getName() + "]. Total failures #: " + errors);
            } else {
                System.out.println("+++ OK in [" + sourceFile.getName() + "]. ");
            }
        }
        assertFalse("Failed test", hasErrors);
    }
}
